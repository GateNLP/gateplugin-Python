/*
 * Copyright (c) 2019 The University of Sheffield.
 *
 * This file is part of gateplugin-Python 
 * (see https://github.com/GateNLP/gateplugin-Python).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package gate.plugin.python;

import com.fasterxml.jackson.jr.ob.JSON;
import gate.plugin.python.gui.PythonEditorVr;
import java.net.URL;

import gate.Resource;
import gate.Controller;
import gate.Document;
import gate.FeatureMap;
import gate.Gate;

import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ControllerAwarePR;
import gate.creole.ResourceInstantiationException;
import gate.creole.ResourceReference;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.creole.metadata.Sharable;
import gate.creole.ExecutionException;
import gate.gui.ResourceHelper;
import gate.lib.interaction.process.pipes.Process4StringStream;
import gate.util.GateRuntimeException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processing resource for running a python program on a document. This allows
 * to edit and run python code using the gatenlp python package on documents.
 *
 * @author Johann Petrak
 */
@CreoleResource(
        name = "PythonPr",
        helpURL = "http://gatenlp.github.io/gateplugin-Python/",
        comment = "Use a Python program as a processing resource")
public class PythonPr
        extends AbstractLanguageAnalyser
        implements ControllerAwarePR, PythonCodeDriven {
  
  private static final long serialVersionUID = -7294555647613502768L;

  /**
   * Set the location of the python program. This parameter allows to set the
   * location of the python program as either a file URL for files on a local
   * disk or as an URL that points into the plugin's jar. If the URL points to a
   * file in a JAR or a local file that is not writable, the file cannot be
   * edited in the editor. If the local file does not exist, it gets created
   * with a template content.
   *
   * @param value the URL pointing to the python file.
   */
  @Optional
  @CreoleParameter(
          comment = "The (file or jar) URL of the Python program to run",
          suffixes = ".py")
  public void setPythonProgram(ResourceReference value) {
    pythonProgram = value;
  }

  /**
   * Get the python program path parameter.
   *
   * @return the value of the parameter
   */
  public ResourceReference getPythonProgram() {
    return pythonProgram;
  }
  protected ResourceReference pythonProgram;

  // fields calculated from pythonProgram
  protected boolean pythonProgramIsJar;
  protected boolean pythonProgramIsReadonly;
  protected File pythonProgramFile;  // if not jar, the file on the disk
  protected String pythonProgramPathInJar;  // if jar, the parent dir of the file
  protected String pythonProgramModuleInJar; // if jar the module name 
  protected URL pythonProgramUrl;  // the python source as URL

  /**
   * Set parameters to send to the python program. The given parameters are
   * passed on to all python functions as kwargs. This expects a FeatureMap
   * because we have a GUI for that but it should really be a map with String
   * keys and values which can be serialized as JSON.
   *
   * @param parms a FeatureMap of parameters
   */
  @Optional
  @RunTime
  @CreoleParameter(comment = "Extra parameters to pass on to the Python program", defaultValue = "")
  public void setProgramParams(FeatureMap parms) {
    programParams = parms;
  }

  /**
   * Get the program parameters.
   *
   * @return program parameters
   */
  public FeatureMap getProgramParams() {
    return programParams;
  }
  protected FeatureMap programParams;

  /**
   * Set the python interpreter command name. This expects the name of the
   * python interpreter as it can be found on the binary path. This must be a
   * python version 3 interpreter!
   *
   * @param value the python interpreter command
   */
  @Optional
  @RunTime
  @CreoleParameter(
          comment = "Python interpreter name (on system PATH)",
          disjunction = "pythonbin",
          priority = 1,
          defaultValue = "python")
  public void setPythonBinary(String value) {
    pythonBinary = value;
  }

  /**
   * Get the python interpreter command.
   *
   * @return python interpreter command
   */
  public String getPythonBinary() {
    return pythonBinary;
  }
  protected String pythonBinary;

  /**
   * The file URL for a python interpreter to use. This can be used as an
   * alternative to the pythonBinary parameter and runs the specified file as a
   * python interpreter.
   *
   * @param value python interpreter program file
   */
  @Optional
  @RunTime
  @CreoleParameter(
          comment = "Python interpreter file URL. If provided overrides pythonBinary.",
          priority = 10,
          disjunction = "pythonbin"
  )
  public void setPythonBinaryUrl(URL value) {
    pythonBinaryUrl = value;
  }

  /**
   * Get the python interpreter file URL.
   *
   * @return python interpreter URL
   */
  public URL getPythonBinaryUrl() {
    return pythonBinaryUrl;
  }
  protected URL pythonBinaryUrl;
  
  protected String pythonBinaryCommand;

  /**
   * Possible python side logging level values.
   */
  public static enum LoggingLevel {
    /**
     * DEBUG
     */
    DEBUG,
    /**
     * INFO
     */
    INFO,
    /**
     * WARNING
     */
    WARNING,
    /**
     * ERROR
     */
    ERROR,
    /**
     * CRITICAL
     */
    CRITICAL
  }

  /**
   * Select log level on the python side.
   *
   * @param value one of the LoggingLevel enum values
   */
  @Optional
  @RunTime
  @CreoleParameter(comment = "Logging level to use on the python side", defaultValue = "INFO")
  public void setLoggingLevel(LoggingLevel value) {
    loggingLevel = value;
  }

  /**
   * Get python logging level.
   *
   * @return logging level.
   */
  public LoggingLevel getLoggingLevel() {
    return loggingLevel;
  }
  protected LoggingLevel loggingLevel;

  /**
   * If we should use our own (the plugin's) copy of the Python gatenlp package.
   * If this is true (the default), the specific version of gatenlp that is
   * included in the plugin will be used, otherwise, the version installed for
   * the python environment is used.
   *
   * @param value Flag inidicating if own gatenlp package should be used
   */
  @Optional
  @RunTime
  @CreoleParameter(comment = "Use Python gatenlp package included in the plugin, not the system one.",
          defaultValue = "true")
  public void setUsePluginGatenlpPackage(Boolean value) {
    usePluginGatenlpPackage = value;
  }

  /**
   * Get the ownGatenlpPackage parameter value.
   *
   * @return value of the parameter
   */
  public Boolean getUsePluginGatenlpPackage() {
    if (usePluginGatenlpPackage == null) {
      return true;
    }
    return usePluginGatenlpPackage;
  }
  protected Boolean usePluginGatenlpPackage;

  /**
   * Result language resource to store any corpus processing results.
   *
   * @param value a PythonPrResult language resource
   */
  @Optional
  @RunTime
  @CreoleParameter(comment = "Result object.")
  public void setOutputResultResource(PythonPrResult value) {
    outputResultResource = value;
  }

  /**
   * Result language resource, accessor. 
   * 
   * @return the result resource
   */
  public PythonPrResult getOutputResultResource() {
    return outputResultResource;
  }
  protected PythonPrResult outputResultResource;

  /**
   * Set the annotation set names to use.
   *
   * If the special name "*" is included in the set, all sets are used.
   * Otherwise only sets with the given names are included. To include the default annotation set
   * add a null value or a value that only consists of spaces.
   *
   * @param val the set of set names to use.
   */
  @Optional
  @RunTime
  @CreoleParameter(
          comment = "Annotation set names to send and use in Python, *=all, null/space=default set",
          defaultValue = "*")
  public void setSetsToUse(Set<String> val) {
    // Make sure the default set is specified as an empty string, even if originally
    // specified as as several spaces
    // Also remove leading and trailing spaces from the names
    Set<String> tmp = new HashSet<>();
    System.err.println("DEBUG: got set: "+val);
    for (String n : val) {
      if(n==null || n.trim().isEmpty()) {
        tmp.add(null);
      } else {
        tmp.add(n.trim());
      }
    }
    setsToUse = tmp;
  }

  /**
   * Get which annotation set names to use.
   *
   * @return set of set names.
   */
  public Set<String> getSetsToUse() { return setsToUse; }
  protected Set<String> setsToUse = new HashSet<>();


  /**
   * This field contains the currently active process for the python program.
   * Otherwise, the field should be null.
   *
   */
  protected transient Process4StringStream process = null;

  /**
   * Our logger instance.
   */
  public transient Logger logger = LoggerFactory.getLogger(this.getClass());

  // the nrDuplicates counter will get shared between copies when this
  // PR is being duplicated. We will do a synchronized increment of the 
  // counter in our own duplication method.
  // NOTE: the first, initial PR will have NrDuplicates set to 0, the
  // actual duplicates will get numbers 1, 2, 3 ...
  // (so the first instance does NOT count as a duplicate)
  /**
   * Shared duplicates counter, setter.
   *
   * @param value the counter
   */
  @Sharable
  public void setNrDuplicates(AtomicInteger value) {
    nrDuplicates = value;
  }

  /**
   * Shared duplicates counter, getter.
   *
   * @return the counter
   */
  public AtomicInteger getNrDuplicates() {
    return nrDuplicates;
  }
  protected AtomicInteger nrDuplicates;

  /**
   * Result list setter.
   *
   * @param value should be a synchronized list
   */
  @Sharable
  public void setResultList(List<Object> value) {
    resultList = value;
  }

  /**
   * Result list getter.
   *
   * @return the list
   */
  public List<Object> getResultList() {
    return resultList;
  }
  protected List<Object> resultList;

  /**
   * Number of duplicates running on a corpus. This gets incremented for each
   * duplicate that receives a controllerExecutionStarted callback and
   * decremented for each duplicate that receives a controllerExecutionFinished
   * or controllerExecutionAborted callback.
   *
   * @param value the value
   */
  @Sharable
  public void setRunningDuplicates(AtomicInteger value) {
    runningDuplicates = value;
  }

  /**
   * Return the value of the currently running duplicates counter.
   *
   * @return number of running duplicates.
   */
  public AtomicInteger getRunningDuplicates() {
    return runningDuplicates;
  }
  protected AtomicInteger runningDuplicates;
  
  protected int duplicateId = 0;

  /**
   * Return the duplicate id of the PR instance. When this PR gets duplicated,
   * each instance will have its own duplicate ID set, with the very first
   * instance having id 0.
   *
   * @return the duplicate id of the PR.
   */
  public int getDuplicateId() {
    return duplicateId;
  }

  /**
   * Make sure the python program command is set. Or we complain about missing
   * parameters.
   */
  public void ensurePythonProgramCommand() {
    // Make sure we know which Python binary to run
    if ((pythonBinary == null || pythonBinary.isEmpty()) && pythonBinaryUrl == null) {
      throw new GateRuntimeException("Cannot run, pythonBinary or pythonBinaryUrl must be specified");
    }
    if (pythonBinaryUrl != null) {
      pythonBinaryCommand = gate.util.Files.fileFromURL(pythonBinaryUrl).getAbsolutePath();
    } else {
      pythonBinaryCommand = pythonBinary;
    }    
  }

  /**
   * Figure out what kind of python file we use.
   *
   * We allow two kinds of locations: on the local file system, i.e. a file:
   * URL, and from within a JAR. This program determines which of those we have
   * and sets the fields pythonProgramXXXX.
   *
   * @param pythonProgramUrl python progrsam url
   */
  public void figureOutPythonFile(URL pythonProgramUrl) {
    if (pythonProgramUrl.getProtocol().equals("file")) {
      pythonProgramFile = gate.util.Files.fileFromURL(pythonProgramUrl);
      if (!pythonProgramFile.exists()) {        
        copyResource("/resources/templates/default.py", pythonProgramFile);
      }
      pythonProgramIsJar = false;
      pythonProgramPathInJar = null;
      pythonProgramModuleInJar = null;
      Path pythonProgramPath = FileSystems.getDefault().getPath(pythonProgramFile.getAbsolutePath());
      if (!java.nio.file.Files.isReadable(pythonProgramPath)) {
        throw new GateRuntimeException("File is not readable: " + pythonProgramFile);
      }
      pythonProgramIsReadonly = !java.nio.file.Files.isWritable(pythonProgramPath);
    } else {
      pythonProgramIsJar = true;
      String[] info = jarUrl2PythonPathAndModule(pythonProgramUrl);
      pythonProgramPathInJar = info[0];
      pythonProgramModuleInJar = info[1];
    }
  } // end figureOutPythonFile

  /**
   * Return flag indicating if the python file can be edited.
   *
   * @return true if it can be edited
   */
  public boolean pythonFileCanBeEdited() {
    return !pythonProgramIsJar && !pythonProgramIsReadonly;
  }

  /**
   * Return URL of the python program we determined.
   * 
   * @return python program URL
   */
  public URL getPythonProgramUrl() {
    return pythonProgramUrl;
  }
  
  /**
   * Return the python program as String.
   * 
   * @return python program as String
   */
  public String getPythonProgramString() {
    try {
      if (pythonProgramIsJar) {
        StringBuilder sb;
        try (
                InputStream is = pythonProgramUrl.openStream();
                BufferedReader rdr = new BufferedReader(new InputStreamReader(is));
            ) {
          sb = new StringBuilder();
          String line;
          while((line = rdr.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
          } 
          return sb.toString();
        }
      } else {
        return new String(Files.readAllBytes(Paths.get(pythonProgramUrl.toURI())));

      }
    } catch (URISyntaxException | IOException ex) {
      ex.printStackTrace(System.err);
      return "[Could not read content of file, see stack trace in message pane!]";
    }
  }

  /**
   * Return the python file or null if in JAR.
   *
   * @return file or null
   */
  public File getPythonProgramFile() {
    return pythonProgramFile;
  }

  /**
   * Rough check if the program can be compiled.
   *
   * This invokes the program with the --mode check parameters to check for
   * syntax errors, import erros and anything that will be detected without
   * actually running the class and interchaning data.
   * <p>
   * The python program is run as a normal program if it is a normal file, if it
   * is in the jar, it gets invoked by loading it as a library.
   *
   * @return true if compilation went ok, false otherwise
   */
  public boolean tryCompileProgram() {
    ensurePythonProgramCommand();
    // TODO: need to figure out if quoting should be disabled based on OS
    // For Linux, disabling seems to be necessary
    boolean doQuoting = false;
    CommandLine cmdLine = new CommandLine(pythonBinaryCommand);
    String pythonPath = "";
    if (getUsePluginGatenlpPackage()) {
      pythonPath = usePythonPackagePath;
    }
    if (!pythonProgramIsJar) {
      // System.err.println("!!!!!!!!!!!!! ######### DEBUG: adding file name: >"+pythonProgramFile.getAbsolutePath()+"<");      
      cmdLine.addArgument(pythonProgramFile.getAbsolutePath(), doQuoting);
    } else {
      // to load as library, we need the RELATIVE path in the jar, relative
      // to what we have set as PYTHONPATH (which is the /resources dir).
      // AND we need to remove the .py extension.
      if (pythonPath.isEmpty()) {
        pythonPath = pythonProgramPathInJar;
      } else {
        pythonPath = pythonPath
                + (PythonPr.isOsWindows() ? ";" : ":")
                + pythonProgramPathInJar;
      }
      cmdLine.addArgument("-m");
      cmdLine.addArgument(pythonProgramModuleInJar, doQuoting);
    }
    cmdLine.addArgument("--mode");
    cmdLine.addArgument("check");
    DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
    ExecuteWatchdog watchdog = new ExecuteWatchdog(10 * 1000); // 10 secs
    Executor executor = new DefaultExecutor();
    
    executor.setWatchdog(watchdog);
    // Note: not sure if the following is how to do it and if does what 
    // I think it does: if the execute watchdog was not able to terminate
    // the process, mayne this makes sure it gets destroyed so that the Java
    // process does not hang on termination?
    executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());
    Map<String, String> env = new HashMap<>();
    env.putAll(System.getenv());
    env.put("PYTHONPATH", pythonPath);
    try {
      if (loggingLevel == LoggingLevel.DEBUG) {        
        logger.info("Trying to compile program:");
        logger.info("Python path is " + pythonPath);
        logger.info("Running: " + cmdLine);        
      }
      // System.err.println("!!!!!!!!!!!!! ######### DEBUG: running: "+cmdLine);      
      executor.execute(cmdLine, env, resultHandler);
    } catch (IOException ex) {
      throw new GateRuntimeException("Could not check the python file", ex);
    }
    try {
      resultHandler.waitFor();
    } catch (InterruptedException ex) {
      throw new GateRuntimeException("Something went wrong when checking the python file", ex);
    }
    int exitCode = resultHandler.getExitValue();
    // System.err.println("!!!!DEBUG: got exit code "+exitCode);
    ExecuteException exc = resultHandler.getException();    
    if (exc != null) {
      logger.error("Got exception running the compile command: " + exc);
    }
    if (loggingLevel == LoggingLevel.DEBUG) {
      logger.info("Got return value from compiling: " + exitCode);
    }
    isCompileOk = (exitCode == 0);
    if (registeredEditorVR != null) {
      if (isCompileOk) {
        registeredEditorVR.setCompilationOk();
      } else {
        registeredEditorVR.setCompilationError();
      }
    }
    return isCompileOk;
  }

  /**
   * Last syntax check status. Initially, this is true.
   */
  public boolean isCompileOk;
  
  private PythonEditorVr registeredEditorVR = null;

  /**
   * Register the visual resource.
   *
   * This gets called by the visual resource as part of the code that is run
   * when setTarget is invoked. If setTarget finds out we should not have VR in
   * the first place, it does not call back and our registeredEditorVR remains
   * null.
   *
   * @param vr visual resource
   */
  public void registerEditorVR(PythonEditorVr vr) {
    registeredEditorVR = vr;
  }

  /**
   * Return the path and module from a JAR URL.
   *
   * @param jarUrl expected to point to a python file inside a jar
   * @return array with two elements, the pythonpath and the module name
   */
  public String[] jarUrl2PythonPathAndModule(URL jarUrl) {
    // eg jar:file:/home/johann/.m2/repository/uk/ac/gate/plugins/python/2.0-SNAPSHOT/python-2.0-SNAPSHOT.jar!/resources/pipelines/python-spacy.py
    // should return /resources/pipelines/python-spacy.py
    if (!"jar".equals(jarUrl.getProtocol())) {
      throw new RuntimeException("Expected JAR url but got: " + jarUrl);
    }
    String urlString = jarUrl.toString();
    urlString = removeValidProtocols(urlString);
    // first of all, split the whole url into the urlfile and urlpath parts
    // before and after the !/ 
    int sepIdx = urlString.indexOf("!/");
    String urlFile;
    String urlPath;
    if (sepIdx == -1) {
      urlFile = urlString;
      urlPath = "/";
    } else {
      urlFile = urlString.substring(0, sepIdx);
      urlPath = urlString.substring(sepIdx + 1);
    }
    // now process the urlPath
    // everything after the last slash is the file, everything before the last
    // slash is the path-part
    sepIdx = urlPath.lastIndexOf("/");
    String file = urlPath.substring(sepIdx + 1);
    String path = urlPath.substring(0, sepIdx);
    // if the file ends with .py, we remove that
    if (file.endsWith(".py")) {
      file = file.substring(0, file.length() - 3);
    }
    // now prepend the file again
    path = urlFile + path;
    String[] ret = new String[2];
    ret[0] = path;
    ret[1] = file;
    return ret;
  }

  /**
   * Given a jar/file URL, remove the valid protocol parts.
   *
   * @param urlString the URL string
   * @return url string with protocols removed
   */
  public String removeValidProtocols(String urlString) {
    // file URL if of the form file://host/path where //host may be missing
    // or host may be empty, so we can have file:/path or file:///path
    // jar: can be prepended to identify this as a JAR file
    // In theory we could also have file://localhost/path etc. but we 
    // do not handle these here yet
    if (urlString.startsWith("jar:file:///")) {
      urlString = urlString.substring(11);
    } else if (urlString.startsWith("jar:file:/")) {
      urlString = urlString.substring(9);
    } else if (urlString.startsWith("file:///")) {
      urlString = urlString.substring(7);
    } else if (urlString.startsWith("file:/")) {
      urlString = urlString.substring(5);
    } else {
      throw new GateRuntimeException("Odd JAR URL: " + urlString);
    }
    // On a windows system, we must remove the leading slash because 
    // there, we want to start with the drive
    if (PythonPr.isOsWindows()) {
      urlString = urlString.substring(1);
    }
    return urlString;
  }

  /**
   * Find the location of where the gatenlp package is in the jar or, for the
   * tests, in the classes directory.
   *
   * This returns the absolute path we need to specify in the PYTHONPATH
   * environment variable in order to put the included gatenlp package on the
   * python path. This first figures out which location our own class got loaded
   * from which is either the plugin JAR or the classes directory during
   * building and testing. Then converts any of these locations into the full
   * path needed for python to find the directory, ither as a zip import or from
   * the ordinary file system.
   *
   * @return location of containing folder
   */
  public String getPythonpathInZip() {
    URL artifactURL = PythonPr.class.getResource("/creole.xml");
    try {
      artifactURL = new URL(artifactURL, ".");
    } catch (MalformedURLException ex) {
      throw new GateRuntimeException("Could not get jar URL");
    }
    String urlString = artifactURL.toString();
    urlString = removeValidProtocols(urlString);
    if (urlString.endsWith("!/")) {
      urlString = urlString.substring(0, urlString.length() - 2);
    } else if (urlString.endsWith("/")) {
      urlString = urlString.substring(0, urlString.length() - 1);
    }
    urlString = urlString + "/resources/pythonpath";
    return urlString;
  }

  /**
   * The python package path to use when running Python. So that the gatenlp
   * package is properly found.
   */
  public String usePythonPackagePath;
  
  static boolean versionInfoShown = false;

  protected ResourceHelper rhBdocApi;
  
  
  //public boolean isJarUrl(URL url) {
  //  String scheme = url.toURI().getScheme();
  //  if()
  //}
  /**
   * Initialise resource.
   *
   * @return resource the PR instance
   * @throws ResourceInstantiationException could not initialise
   */
  @Override
  public Resource init() throws ResourceInstantiationException {    
    if (!versionInfoShown) {      
      try {
        Properties properties = new Properties();        
        InputStream is = getClass().getClassLoader().getResourceAsStream("gateplugin-Python.git.properties");
        if (is != null) {
          properties.load(is);
          String buildVersion = properties.getProperty("gitInfo.build.version");
          String isDirty = properties.getProperty("gitInfo.dirty");
          if (buildVersion != null && buildVersion.endsWith("-SNAPSHOT")) {
            logger.info("Plugin Python version=" + buildVersion
                    + " commit="
                    + properties.getProperty("gitInfo.commit.id.abbrev")
                    + " dirty=" + isDirty
            );
          }
        } else {
          logger.error("Could not obtain plugin Python version info");
        }
      } catch (IOException ex) {
        logger.error("Could not obtain plugin Python version info: " + ex.getMessage(), ex);
      }
      /*
      try {
        Properties properties = new Properties();        
        
        InputStream is = BdocDocument.class.getClassLoader().getResourceAsStream("gatelib-basicdocument.git.properties");
        if (is != null) {
          properties.load(is);
          String buildVersion = properties.getProperty("gitInfo.build.version");
          String isDirty = properties.getProperty("gitInfo.dirty");
          if (buildVersion != null && buildVersion.endsWith("-SNAPSHOT")) {
            logger.info("Lib basicdocument version=" + buildVersion
                    + " commit=" + properties.getProperty("gitInfo.commit.id.abbrev")
                    + " dirty=" + isDirty
            );
          }
        }
      } catch (IOException ex) {
        logger.error("Could not obtain lib basicdocument version info: " + ex.getMessage(), ex);
      }
      */
      try {
        Properties properties = new Properties();        
        InputStream is = Process4StringStream.class.getClassLoader().getResourceAsStream("gatelib-interaction.git.properties");
        if (is != null) {
          properties.load(is);
          String buildVersion = properties.getProperty("gitInfo.build.version");
          String isDirty = properties.getProperty("gitInfo.dirty");
          if (buildVersion != null && buildVersion.endsWith("-SNAPSHOT")) {
            logger.info("Lib interaction version=" + buildVersion
                    + " commit=" + properties.getProperty("gitInfo.commit.id.abbrev")
                    + " dirty=" + isDirty
            );
          }
        }
      } catch (IOException ex) {
        logger.error("Could not obtain lib interaction version info: " + ex.getMessage(), ex);
      }
      versionInfoShown = true;
    }
    // check the pythonProgram parameter: must be JAR or file URL
    if (pythonProgram == null) {
      throw new ResourceInstantiationException("Parameter pythonProgram must be set!");
    }
    String scheme = pythonProgram.toURI().getScheme();
    if (!"file".equals(scheme) && !"jar".equals(scheme) && !"creole".equals(scheme)) {
      throw new ResourceInstantiationException(
              "Parameter pythonProgram is not a file, jar, or creole URL but: "
              + getPythonProgram());      
    }
    usePythonPackagePath = getPythonpathInZip();
    pythonProgramUrl = null;
    try {
      pythonProgramUrl = pythonProgram.toURL();
    } catch (IOException ex) {
      throw new ResourceInstantiationException("Could not convert to URL: " + pythonProgram, ex);
    }
    figureOutPythonFile(pythonProgramUrl);
    // count which duplication id we have, the first instance gets null, the 
    // duplicates will find the instance from the first instance
    if (nrDuplicates == null) {
      nrDuplicates = new AtomicInteger(1);      
      runningDuplicates = new AtomicInteger(0);
      duplicateId = 0;
      List<Object> wrappedList = new ArrayList<>();
      setResultList(Collections.synchronizedList(wrappedList));
    } else {
      duplicateId = nrDuplicates.getAndAdd(1);
    }
    rhBdocApi = (ResourceHelper)Gate.getCreoleRegister()
                     .get("gate.plugin.format.bdoc.API")
                     .getInstantiations().iterator().next();    
    return this;
  } // end init()

  /**
   * This will run whenever a corpus gets run. We start the python process for
   * every new run on a corpus and stop it when the corpus is finished.
   */
  protected void whenStarting() {
    runningDuplicates.getAndIncrement();
    ensurePythonProgramCommand();
    // Make sure we have a Python program that at least looks like we could run it    
    // Get the effective path to the python binary: either use the pythonbinary name
    // or the corresponding path for the pythonbinaryurl, which must be a file url    
    isCompileOk = tryCompileProgram();
    if (!isCompileOk) {
      throw new GateRuntimeException("Cannot run the python program, my have a syntax error");
    }
    // ok, actually run the python program so we can communicate with it. 
    // for now we use Process4StringStream from gatelib-interaction for this.
    Map<String, String> env = new HashMap<>();
    String pythonPath = "";
    if (getUsePluginGatenlpPackage()) {
      pythonPath = usePythonPackagePath;
    }
    if (pythonProgramIsJar) {
      if (pythonPath.isEmpty()) {
        pythonPath = pythonProgramPathInJar;
      } else {
        pythonPath = pythonPath
                + (PythonPr.isOsWindows() ? ";" : ":")
                + pythonProgramPathInJar;
      }
      env.put("PYTHONPATH", pythonPath);      
      process = Process4StringStream.create(
              new File("."),
              env,
              pythonBinaryCommand,
              "-m",
              pythonProgramModuleInJar,
              "--mode",
              "pipe",
              "--log_lvl",
              loggingLevel.toString()
      );
    } else {
      env.put("PYTHONPATH", pythonPath);
      process = Process4StringStream.create(
              new File("."),
              env,
              pythonBinaryCommand,
              pythonProgramFile.getAbsolutePath(),
              "--mode",
              "pipe",
              "--log_lvl",
              loggingLevel.toString()
      );
    }
    String responseJson = (String) process.process(makeStartRequest());
    if (responseJson == null) {
      throw new GateRuntimeException("Invalid null response from Python process, did you run interact()?");
    }
    try {
      Map<String, Object> response = JSON.std.mapFrom(responseJson);
      if (!response.containsKey("status") || !"ok".equals(response.get("status"))) {
        throw new GateRuntimeException("Something went wrong, start response is " + responseJson);
      }
    } catch (IOException ex) {
      throw new GateRuntimeException("Could not convert start response", ex);
    }
  }
  
  
  
  protected void whenFinishing() {
    runningDuplicates.getAndDecrement();
    logger.debug("Finishing duplicate " + duplicateId + " running: " + runningDuplicates.get());
    String responseJson = (String) process.process(makeFinishRequest());
    Map<String, Object> result = null;
    try {
      FinishResponse response = JSON.std.beanFrom(FinishResponse.class, responseJson);
      if (!"ok".equals(response.status)) {
        throw new GateRuntimeException("Error Finishing Processing: " + response.error
                + "\nAdditional info from Python:\n" + response.info);
      }
      Map<String, Object> data = response.data;
      // if the number of duplicates is 1, then data already is the final result
      if (nrDuplicates.get() == 1) {
        result = data;
      } else {
        // add the data to the resultList, but only if we got something 
        if (data != null) {
          getResultList().add(data);
        }
      }
    } catch (IOException ex) {
      throw new GateRuntimeException("Could not convert execute response JSON: " + responseJson, ex);
    }
    // if the number of running duplicates is 0, call the reduce method
    // but only if there is something in the list
    if (runningDuplicates.get() == 0) {
      if (!getResultList().isEmpty()) {
        logger.debug("Calling reduce for number of results: " + getResultList().size());
        // Call the reduce method
        responseJson = (String) process.process(makeReduceRequest());
        try {
          FinishResponse response = JSON.std.beanFrom(FinishResponse.class, responseJson);
          if (!"ok".equals(response.status)) {
            throw new GateRuntimeException("Error calling Reduce: " + response.error
                    + "\nAdditional info from Python:\n" + response.info);
          }
          result = response.data;
        } catch (IOException ex) {
          throw new GateRuntimeException("Could not convert execute response JSON: " + responseJson, ex);
        }        
        
      } else {
        logger.debug("Not calling reduce, result list is empty");
      }
      // if we have a result resource, set the result in the resource      
      // otherwise set the features of the PR from it.
      // Only do any of this if the result is a map
      if (result instanceof Map) {
        if (getOutputResultResource() != null) {
          getOutputResultResource().setResultData(result);
        } else {
          if (result != null) {
            this.getFeatures().putAll(result);
          }
        }
      } else {
        logger.info("Result returned from the Python process is not a map, ignored");
      }
    }
    int exitValue = process.stop();
    if (exitValue != 0) {
      logger.info("Warning: python process ended with exit value " + exitValue);
    }    
  }

  /**
   * Re-initialize resource.
   *
   * @throws ResourceInstantiationException exception if initialisation fails
   */
  @Override
  public void reInit() throws ResourceInstantiationException {
    nrDuplicates = null;  // should we do this?
    super.reInit();
    init();
  }

  /**
   * Cleanup resource.
   */
  @Override
  public void cleanup() {
    super.cleanup();
  }
  
  private void ensureProcess() throws ExecutionException {
    if (!(process != null && process.isAlive())) {
      throw new ExecutionException("Python process not alive during execution");
    }
  }

  /**
   * Process document.
   *
   * @throws ExecutionException exception when processing fails
   */
  @Override
  public void execute() throws ExecutionException {
    if (isInterrupted()) {
      throw new ExecutionException("Processing was interrupted");
    }
    ensureProcess();
    // send over the current document in an execute command
    // get back the changelog in a result object (or some error)
    // if we get a changelog apply the changelog to the document
    // if we get an error, throw an error condition and abort the process
    String responseJson = (String) process.process(makeExecuteRequest(document));
    try {
      ExecuteResponse response = JSON.std.beanFrom(ExecuteResponse.class, responseJson);
      if (!"ok".equals(response.status)) {
        logger.debug("Python exception, stacktrace we got: "+response.stacktrace);
        throw new GateRuntimeException("Error processing document: " + response.error
                + "\nAdditional info from Python:\n" + response.info);
      }      
      //ChangeLog chlog = response.data;
      if (response.data == null) {
        throw new GateRuntimeException("Got null changelog back from process");
      }
      rhBdocApi.call("update_document_from_logmap", document, response.data);
    } catch (IOException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | 
            InvocationTargetException ex) {
      throw new GateRuntimeException("Could not convert execute response JSON: " + responseJson, ex);
    }
  }

  /**
   * Callback when running over a whole corpus starts.
   *
   * @param controller the controller used
   */
  @Override
  public void controllerExecutionStarted(Controller controller) {
    whenStarting();
  }

  /**
   * Callback when running over a whole corpus finishes.
   *
   * @param controller the controller used
   */
  @Override
  public void controllerExecutionFinished(Controller controller) {
    whenFinishing();
  }

  /**
   * Callback when running over a wholr corpus terminates with an exception.
   *
   * @param controller the controller used
   * @param throwable the throwable from the exception
   */
  @Override
  public void controllerExecutionAborted(Controller controller, Throwable throwable) {
    whenFinishing();
    throw new GateRuntimeException("Exception when running pipeline", throwable);
  }
  
  static protected class ExecuteResponse {

    public String status;
    public String error;
    public String info;
    public List<List<String>> stacktrace;
    // public ChangeLog data;
    public Map<String, Object> data;
  }
  
  static protected class FinishResponse {

    public String status;
    public String error;
    public String info;
    public List<List<String>> stacktrace;
    public Map<String, Object> data;
  }
  
  protected String getResponseError(Map<String, Object> response) {
    String error = (String) response.get("error");
    if (error == null) {
      error = "(No error description from process)";
    }
    return error;
  }
  
  /*
  protected String getResponseInfo(Map<String, Object> response) {
    String info = (String) response.get("info");
    if (info == null) {
      info = "(No additional error infor from process)";
    }
    return info;
  }
  */

  /**
   * Create and return the JSON String representing an execute request.
   *
   * @param doc the document to send over
   * @return JSON string
   */
  @SuppressWarnings("unchecked")
  protected String makeExecuteRequest(Document doc) {
    Map<String, Object> request = new HashMap<>();
    request.put("command", "execute");
    // create the BdocDocument from our document   
    Map<String,Object> mdoc;
    try {
      if (setsToUse == null || setsToUse.contains("*")) {
        mdoc = (Map<String, Object>) rhBdocApi.call("bdocmap_from_doc", document);
      } else {
        mdoc = (Map<String, Object>) rhBdocApi.call("bdocmap_from_doc", document, setsToUse, true);
      }
    } catch (NoSuchMethodException | IllegalArgumentException |
            IllegalAccessException | InvocationTargetException ex) {
      throw new GateRuntimeException("Error when trying to convert document to map", ex);
    }
    //BdocDocument bdoc = new BdocDocumentBuilder().fromGate(document).buildBdoc();
    request.put("data", mdoc);
    try {
      return JSON.std.asString(request);
    } catch (IOException ex) {
      throw new GateRuntimeException("Error when trying to convert execute request to JSON", ex);
    }
  }
  
  @SuppressWarnings("unchecked")
  protected String makeStartRequest() {
    Map<String, Object> request = new HashMap<>();
    request.put("command", "start");
    Map<String, Object> params;
    try {
      params = (Map<String, Object>)rhBdocApi.call("fmap_to_map", null, programParams);
    } catch (NoSuchMethodException | IllegalArgumentException | 
            IllegalAccessException | InvocationTargetException ex) {
      throw new GateRuntimeException("Could not create start request map", ex);
    }
    //Map<String, Object> params = BdocUtils.featureMap2Map(programParams, null);
    params.put("gate_plugin_python_duplicateId", duplicateId);
    params.put("gate_plugin_python_nrDuplicates", nrDuplicates.get());    
    if (pythonProgramIsJar) {
      params.put("gate_plugin_python_pythonPath", pythonProgramPathInJar);
      params.put("gate_plugin_python_pythonModule", pythonProgramModuleInJar);
    } else {
      params.put("gate_plugin_python_pythonFile", pythonProgramFile.getAbsolutePath());
    }
    request.put("data", params);
    try {
      return JSON.std.asString(request);
    } catch (IOException ex) {
      throw new GateRuntimeException("Error when trying to convert start request to JSON", ex);
    }    
  }
  
  protected String makeFinishRequest() {
    Map<String, Object> request = new HashMap<>();
    request.put("command", "finish");
    try {
      return JSON.std.asString(request);
    } catch (IOException ex) {
      throw new GateRuntimeException("Error when trying to convert finish request to JSON", ex);
    }    
  }
  
  protected String makeReduceRequest() {
    Map<String, Object> request = new HashMap<>();
    request.put("command", "reduce");
    request.put("data", getResultList());
    try {
      return JSON.std.asString(request);
    } catch (IOException ex) {
      throw new GateRuntimeException("Error when trying to convert reduce request to JSON", ex);
    }    
  }

  /**
   * Copy resource from plugin jar to target path. NOTE: normally this copies
   * from whatever JAR file the creole.xml for this PR is in, but when running
   * the gapploading test, the creole.xml is in the target classes directory
   * instead and we then need to copy in a different way.
   *
   * @param source the path of the resource to copy
   * @param targetPath where to copy to, must not already exist
   */
  public void copyResource(String source, File targetPath) {
    
    URL artifactURL = PythonPr.class.getResource("/creole.xml");
    try {
      artifactURL = new URL(artifactURL, ".");
    } catch (MalformedURLException ex) {
      throw new GateRuntimeException("Could not get jar URL");
    }
    if (artifactURL.toString().startsWith("file:/")) {
      try {
        File containingDir = gate.util.Files.fileFromURL(artifactURL);
        File fromFile = new File(containingDir, source);
        logger.info("Copying python file from " + fromFile + " to " + targetPath);
        java.nio.file.Files.copy(
                fromFile.toPath(),
                targetPath.toPath());
      } catch (IOException ex) {
        throw new GateRuntimeException("Error trying to copy the resources", ex);
      }
    } else {
      try (
              FileSystem zipFs
              = FileSystems.newFileSystem(artifactURL.toURI(), new HashMap<>());) {        
        Path target = Paths.get(targetPath.toURI());
        Path pathInZip = zipFs.getPath(source);
        if (java.nio.file.Files.isDirectory(pathInZip)) {
          throw new GateRuntimeException("ODD: is a directory " + pathInZip);
        }
        java.nio.file.Files.copy(pathInZip, target);
      } catch (IOException | URISyntaxException ex) {
        throw new GateRuntimeException("Error trying to copy the resources", ex);
      }
    }
  }

  /**
   * Check if we are running on windows.
   *
   * @return true if on Windows, false otherwise
   */
  public static boolean isOsWindows() {
    return System.getProperty("os.name").toLowerCase(Locale.UK).contains("win");
  }
  
}
