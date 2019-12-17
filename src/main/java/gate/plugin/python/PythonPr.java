/*
 * Copyright (c) 2019 The University of Sheffield.
 *
 * This file is part of gateplugin-python 
 * (see https://github.com/GateNLP/gateplugin-python).
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
import gate.lib.basicdocument.BdocDocument;
import gate.lib.basicdocument.BdocDocumentBuilder;
import gate.lib.basicdocument.BdocUtils;
import gate.lib.basicdocument.ChangeLog;
import gate.lib.basicdocument.GateDocumentUpdater;
import gate.lib.interaction.process.pipes.Process4StringStream;
import gate.util.Files;
import gate.util.GateRuntimeException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.io.FileUtils;



/**
 * Processing resource for running a python program on a document.
 * This allows to edit and run python code using the gatenlp python package
 * on documents.
 * 
 * @author Johann Petrak
 */
@CreoleResource(
        name = "PythonPr",
        helpURL = "http://gatenlp.github.io/gateplugin-python/",
        comment = "Use a Python program as a processing resource")
public class PythonPr
        extends AbstractLanguageAnalyser
        implements ControllerAwarePR, PythonCodeDriven
{

  private static final long serialVersionUID = -7294093586613502768L;

  /**
   * Set the location of the python program.
   * This parameter allows to set the location of the python program as 
   * either a normal URL (a file: URL for files on a local disk) or as an URL
   * that points into the plugin's jar. If the URL points to something that is
   * not a local file, the content gets copied into the working directory and
   * that copy of the file is used instead.
   * <p>
   * This parameter gets ignored if the pythonProgramPath parameter is set.
   * <p>
   * If both this and the pythonProgramPath parameter are empty, a new 
   * file is created in the working directory.
   * 
   * @param value the URL pointing to the python file. 
   */
  @Optional
  @CreoleParameter(
          comment = "The URL of the Python program to run",
          disjunction = "program",
          priority = 0,
          suffixes = ".py")
  public void setPythonProgram(ResourceReference value) {
    pythonProgram = value;
  }
  /**
   * Get the python program path parameter.
   * @return the value of the parameter
   */
  public ResourceReference getPythonProgram() {
    return pythonProgram;
  }
  protected ResourceReference pythonProgram;

  /**
   * Set the python program path.
   * 
   * This can be used as an alternative to the setPythonProgram method to 
   * set the path to the python program as an absolute or relative file path.
   * If the path is relative, it is interpreted as relative to whatever 
   * directory is used as a working directory.
   * 
   * @param value the python program file path
   */
  @Optional
  @CreoleParameter(
          comment = "An absolute or relative file path to the python program",
          disjunction = "program",
          priority = 1,
          suffixes = ".py")
  public void setPythonProgramPath(String value) {
    pythonProgramPath = value;
  }
  /**
   * Get the python program file path.
   * @return the value of the parameter
   */
  public String getPythonProgramPath() {
    return pythonProgramPath;
  }
  protected String pythonProgramPath;
  
  protected File currentPythonProgramFile = null;
  /**
   * Get the currently known python program file. 
   * @return python program file
   */
  public File getCurrentPythonProgramFile() { return currentPythonProgramFile; }


  /**
   * Set parameters to send to the python program.
   * The given parameters are passed on to all python functions as kwargs.
   * This expects a FeatureMap because we have a GUI for that but it should 
   * really be a map with String keys and values which can be serialized as
   * JSON.
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
   * @return program parameters
   */
  public FeatureMap getProgramParams() {
    return programParams;
  }
  protected FeatureMap programParams;

  /**
   * Set the python interpreter command name.
   * This expects the name of the python interpreter as it can be found
   * on the binary path. This must be a python version 3 interpreter!
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
   * @return python interpreter command
   */
  public String getPythonBinary() {
    return pythonBinary;
  }
  protected String pythonBinary;
  
  /**
   * The file URL for a python interpreter to use. 
   * This can be used as an alternative to the pythonBinary parameter and 
   * runs the specified file as a python interpreter. 
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
   * @return python interpreter URL
   */
  public URL getPythonBinaryUrl() {
    return pythonBinaryUrl;
  }
  protected URL pythonBinaryUrl;
  
  protected String pythonBinaryCommand;
  
  /**
   * The working directory to use.
   * If this is not set, the current directory of the process running GATE
   * is used. This is only relevant if the python program file is specified relative
   * to the working directory or if the python program file needs to get copied
   * to the working directory for editing and use. 
   * @param value URL of working directory
   */
  @Optional
  @CreoleParameter(comment = "Working directory.")
  public void setWorkingDirUrl(URL value) {
    workingDirUrl = value;
  }
  /**
   * Get the working directory URL.
   * @return working directory URL
   */
  public URL getWorkingDirUrl() {
    return workingDirUrl;
  }
  protected URL workingDirUrl;
  protected File workingDir;   // the file to use, based on the workingDirUrl
  
  
  /**
   * Enable debugging mode.
   * @param value flag to indiciate if debug mode is enabled
   */
  @Optional
  @RunTime
  @CreoleParameter(comment = "Enable debugging mode", defaultValue = "false")
  public void setDebugMode(Boolean value) {
    debugMode = value;
  }
  /**
   * Get debugging mode. 
   * @return debugging mode
   */
  public Boolean getDebugMode() {
    if(debugMode == null) {
      return false;
    }
    return debugMode;
  }
  protected Boolean debugMode;
          
  /**
   * If we should use our own copy of the Python gatenlp package.
   * If this is true (the default), the specific version of gatenlp that
   * is included in the plugin will be used, otherwise, the version installed
   * for the python environment is used.
   * @param value Flag inidicating if own gatenlp package should be used
   */
  @Optional
  @RunTime
  @CreoleParameter(comment = "Use Python gatenlp package included in the plugin, not the system one.",
          defaultValue = "true")
  public void setUseOwnGatenlpPackage(Boolean value) {
    useOwnGatenlpPackage = value;
  }
  /**
   * Get the ownGatenlpPackage parameter value.
   * @return value of the parameter
   */
  public Boolean getUseOwnGatenlpPackage() {
    if (useOwnGatenlpPackage == null) {
      return true;
    }
    return useOwnGatenlpPackage;
  }
  protected Boolean useOwnGatenlpPackage;
  
  /**
   * This field contains the currently active process for the python program.
   * Otherwise, the field should be null.
   * 
   */
  protected transient Process4StringStream process = null; 
            
  /**
   * Our logger instance.
   */
  public org.apache.log4j.Logger logger = 
          org.apache.log4j.Logger.getLogger(this.getClass());
  
  // the nrDuplicates counter will get shared between copies when this
  // PR is being duplicated. We will do a synchronized increment of the 
  // counter in our own duplication method.
  // NOTE: the first, initial PR will have NrDuplicates set to 0, the
  // actual duplicates will get numbers 1, 2, 3 ...
  // (so the first instance does NOT count as a duplicate)
  
  /**
   * Shared duplicates counter, setter.
   * @param value the counter
   */
  @Sharable
  public void setNrDuplicates(AtomicInteger value) {
    nrDuplicates = value;
  }
  /**
   * Shared duplicates counter, getter.
   * @return the counter
   */
  public AtomicInteger getNrDuplicates() {
    return nrDuplicates;
  }
  protected AtomicInteger nrDuplicates;
  
  /**
   * Number of duplicates running on a corpus.
   * This gets incremented for each duplicate that receives a controllerExecutionStarted
   * callback and decremented for each duplicate that receives a controllerExecutionFinished
   * or controllerExecutionAborted callback.
   * @param value the value
   */
  @Sharable
  public void setRunningDuplicates(AtomicInteger value) {
    runningDuplicates = value;
  }
  /**
   * Return the value of the currently running duplicates counter.
   * @return  number of running duplicates.
   */
  public AtomicInteger getRunningDuplicates() {
    return runningDuplicates;
  }
  protected AtomicInteger runningDuplicates;
  
  protected int duplicateId = 0;
  /**
   * Return the duplicate id of the PR instance.
   * When this PR gets duplicated, each instance will have its own duplicate
   * ID set, with the very first instance having id 0. 
   * @return the duplicate id of the PR.
   */
  public int getDuplicateId() {
    return duplicateId;
  }

  /**
   * Make sure the python program command is set.
   * Or we complain about missing parameters.
   */
  public void ensurePythonProgramCommand() {
    // Make sure we know which Python binary to run
    if((pythonBinary == null || pythonBinary.isEmpty()) && pythonBinaryUrl == null) {
      throw new GateRuntimeException("Cannot run, pythonBinary or pythonBinaryUrl must be specified");
    }
    if(pythonBinaryUrl != null) {
      pythonBinaryCommand = Files.fileFromURL(pythonBinaryUrl).getAbsolutePath();
    } else {
      pythonBinaryCommand = pythonBinary;
    }    
  }
  
  
  /**
   * Rough check if the program can be compiled.
   * 
   * This uses py_compile to check if the program can be compiled. 
   * Being able to compile does not guarantee there are no other errors
   * in it of course.
   * 
   * @return true if compilation went ok, false otherwise
   */
  public boolean tryCompileProgram() {
    ensurePythonProgramCommand();
    // we check for syntax errors by running <pythonbinary> -m py_compile <file>
    // see https://docs.python.org/3/library/py_compile.html
    // For now we do this by using apache commons exec with a watchdog
    CommandLine cmdLine = new CommandLine(pythonBinaryCommand);
    cmdLine.addArgument("-m");
    cmdLine.addArgument("py_compile");
    cmdLine.addArgument(currentPythonProgramFile.getAbsolutePath());
    // System.err.println("DEBUG: running: "+cmdLine.toString());
    DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
    ExecuteWatchdog watchdog = new ExecuteWatchdog(10*1000); // 10 secs
    Executor executor = new DefaultExecutor();
    
    executor.setWatchdog(watchdog);
    executor.setWorkingDirectory(workingDir);
    // Note: not sure if the following is how to do it and if does what 
    // I think it does: if the execute watchdog was not able to terminate
    // the process, mayne this makes sure it gets destroyed so that the Java
    // process does not hang on termination?
    executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());
    Map<String,String> env = new HashMap<>();
    env.putAll(System.getenv());
    if(getUseOwnGatenlpPackage()) {
      env.put("PYTHONPATH", usePythonPackagePath);
    }
    try {
      if(getDebugMode()) {        
        logger.info("Trying to compile program:");
        logger.info("Python path is "+usePythonPackagePath);
        logger.info("Running: "+cmdLine.toString());
      }
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
    ExecuteException exc = resultHandler.getException();    
    if(exc != null) {
      logger.error("Got exception running the compile command: "+exc);
    }
    if(getDebugMode()) {
      logger.info("Got return value from compiling: "+exitCode);
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
   * Last syntax check status. 
   * Initially, this is true.
   */
  public boolean isCompileOk;
  
  private PythonEditorVr registeredEditorVR = null;

  /**
   * Register the visual resource.
   * @param vr visual resource
   */
  public void registerEditorVR(PythonEditorVr vr) {
    registeredEditorVR = vr;
  }

  /**
   * Find the location of where the gatenlp package is in the zip or classes 
   * directory.
   * 
   * @return location of containing folder
   */
  public static String getPackageParentPathInZip() {
    URL artifactURL = PythonPr.class.getResource("/creole.xml");
    try {
      artifactURL = new URL(artifactURL, ".");
    } catch (MalformedURLException ex) {
      throw new GateRuntimeException("Could not get jar URL");
    }
    String urlString = artifactURL.toString();
    if(urlString.startsWith("jar:file:///")) {
      urlString = urlString.substring(11);
      urlString = urlString.substring(0, urlString.length()-2);
    } else if(urlString.startsWith("jar:file:/")) {
      urlString = urlString.substring(9);
      urlString = urlString.substring(0, urlString.length()-2);
    } else if(urlString.startsWith("file:///")) {
      urlString = urlString.substring(7);
      urlString = urlString.substring(0, urlString.length()-1);
    } else if(urlString.startsWith("file:/")) {
      urlString = urlString.substring(5);
      urlString = urlString.substring(0, urlString.length()-1);
    } else {
      throw new GateRuntimeException("Odd JAR URL: "+urlString);
    }
    urlString = urlString + "/resources/";
    //System.err.println("DEBUG: resources location: "+urlString);
    return urlString;
  }

  /**
   * The python package path to use when running Python.
   * So that the gatenlp package is properly found.
   */
  public String usePythonPackagePath;
  
  
  /**
   * Figure out which python file to use.
   * 
   * This is not trivial because which file to use depends on the following 
   * parameters: pythonProgram (ResourceReference), pythonProgramPath(String), 
   * and workingDirUrl(URL). Since all these parameters are runtime parameters,
   * they also can change at almost any time and they are all empty initially. 
   * <p>
   * A further complication is that we have a visual resource for editing 
   * the python code. This means that as soon as the user activates the 
   * visual resource, we must decide on which file to edit. This means that
   * if now program has explicitly been set yet, we should use some temporary
   * file somewhere. Also, if the program has been set to a location in the 
   * JAR (through the ResourceReference) than we need to create a copy of that
   * somewhere in order to edit it and use that copy instead. 
   * <p>
   * This method decides which file to use, and, if necessary, also creates
   * that file. The File object representing the file gets returned. 
   * The method also updates the global currentPythonProgramFile variable and
   * tries to update the editor, if necessary. 
   * 
   * @return the File representing the Python program file
   */
  public File figureOutPythonFile() {
    // This gets called to figure out the python file we want to use.
    //
    // Here are the main situations:
    // 1) no parameter is set but the user wants to edit/run:
    // in that case, we create a new file in the effective working directory,
    // initialised with the code template. 
    // 2) a pythonProgramPath is set: 
    File pythonProgramFile = null;   // the file we will end up using
    // if both parms are empty and we need a python program file, we create 
    // a temporary one in the working directory from the program template
    if (pythonProgram == null && (pythonProgramPath == null || pythonProgramPath.isEmpty())) {
      String tmpfilename = "tmpfile.py";
      pythonProgramFile = new File(workingDir, tmpfilename);
      if(!pythonProgramFile.exists()) {          
        logger.info("Creating new Python file from template: "+pythonProgramFile);
        copyResource("/resources/templates/default.py", pythonProgramFile);
      } else{
        logger.info("Using existing Python file "+pythonProgramFile);
      }
    } else if(pythonProgramPath != null && !pythonProgramPath.isEmpty()) {
      // If the pythonProgramPath is set, it takes precedence of ther the pythonProgram
      // if the path is absolute, use just that file, otherwise, make it
      // relative to the working directory, not the current directory
      File tmpfile = new File(pythonProgramPath);
      if(tmpfile.isAbsolute()) {
        pythonProgramFile = tmpfile;
      } else {
        pythonProgramFile = new File(workingDir, pythonProgramPath);
      }
      if(!pythonProgramFile.exists()) {          
        copyResource("/resources/templates/default.py", pythonProgramFile);
      }
    } else if(pythonProgram == null) {
      throw new GateRuntimeException("Should never be thrown");      
    } else  if(pythonProgram.toURI().getScheme().equals("file")) {
      try {
        pythonProgramFile = gate.util.Files.fileFromURL(pythonProgram.toURL());
        if(!pythonProgramFile.exists()) {          
          copyResource("/resources/templates/default.py", pythonProgramFile);
        }
      } catch (IOException ex) {
        throw new GateRuntimeException("Could not determine file for pythonProgram "+pythonProgram, ex);
      }
    } else {
      URI pythonProgramUri = pythonProgram.toURI();
      String tmpfilename = Paths.get(pythonProgramUri.getPath()).getFileName().toString();
      pythonProgramFile = new File(workingDir, tmpfilename);
      // if the file we figured out is already known we do not need to do 
      // anything, otherwise check if we need to copy and if yes, do it!
      if(!pythonProgramFile.equals(currentPythonProgramFile)) {      
        if (pythonProgramFile.exists()) {
          logger.warn("Not copying " + pythonProgram + " to " + pythonProgramFile + ", already exists!");
        } else {
          try (
                  BufferedReader br
                  = new BufferedReader(new InputStreamReader(pythonProgram.openStream(), "UTF-8"));
                  PrintStream osr
                  = new PrintStream(new FileOutputStream(pythonProgramFile), true, "UTF-8");) {
            String line;
            while (null != (line = br.readLine())) {
              osr.println(line);
            }
            logger.info("Copied from JAR to " + pythonProgramFile);
          } catch (IOException ex) {
            Logger.getLogger(PythonPr.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
    }
    try {
      // just check if we can read the script here ... what we read is not actually 
      // ever used
      FileUtils.readFileToString(pythonProgramFile, "UTF-8");
    } catch (IOException ex) {
      throw new GateRuntimeException("Could not read the python program from " + pythonProgramFile, ex);
    }

    if(!pythonProgramFile.equals(currentPythonProgramFile)) {
      currentPythonProgramFile = pythonProgramFile;
      if(registeredEditorVR != null) {
        registeredEditorVR.setFile(currentPythonProgramFile);
      }
    }
    
    return pythonProgramFile;
  } // end figureOutPythonFile
  
  /**
   * Initialize resource.
   * @return resource the PR instance
   * @throws ResourceInstantiationException could not initialise
   */
  @Override
  public Resource init() throws ResourceInstantiationException {
    // First of all, check the init parms:
    if(workingDirUrl == null) {
      workingDir = new File(".");
    } else {
      workingDir = Files.fileFromURL(workingDirUrl);
    }
    // check some problems with the working dir here because using something
    // odd can cause problems that are hard to debug when running commands later
      if(!workingDir.isDirectory()) {
        throw new ResourceInstantiationException("Working directory URL must specify a directory");
      }
      if(!workingDir.canRead()) {
        throw new ResourceInstantiationException("Working directory must be readable");
      }
      if(!workingDir.canWrite()) {
        throw new ResourceInstantiationException("Working directory must be writable");
      }
    usePythonPackagePath = PythonPr.getPackageParentPathInZip();
    //System.err.println("DEBUG: pythonpath is "+usePythonPath);
    // count which duplication id we have, the first instance gets null, the 
    // duplicates will find the instance from the first instance
    if(nrDuplicates==null) {
      nrDuplicates = new AtomicInteger(1);      
      runningDuplicates = new AtomicInteger(0);
      duplicateId = 0;
    } else {
      duplicateId = nrDuplicates.getAndAdd(1);
    }
    return this;
  } // end init()

  
  /**
   * This will run whenever a corpus gets run.
   * We start the python process for every new run on a corpus and stop
   * it when the corpus is finished. 
   */
  protected void whenStarting() {
    runningDuplicates.getAndIncrement();
    figureOutPythonFile();
    ensurePythonProgramCommand();
    // Make sure we have a Python program that at least looks like we could run it    
    // Get the effective path to the python binary: either use the pythonbinary name
    // or the corresponding path for the pythonbinaryurl, which must be a file url    
    isCompileOk = tryCompileProgram();
    if(!isCompileOk) {
      throw new GateRuntimeException("Cannot run the python program, my have a syntax error");
    }
    // ok, actually run the python program so we can communicate with it. 
    // for now we use Process4StringStream from gatelib-interaction for this.
    Map<String,String> env = new HashMap<>();
    if(getUseOwnGatenlpPackage()) {
      env.put("PYTHONPATH", usePythonPackagePath);
    }
    if(getDebugMode()) {
      process = Process4StringStream.create(workingDir, env, pythonBinaryCommand, "-d", currentPythonProgramFile.getAbsolutePath());
    } else {
      process = Process4StringStream.create(workingDir, env, pythonBinaryCommand, currentPythonProgramFile.getAbsolutePath());
    }
    String responseJson = (String)process.process(makeStartRequest());
    if(responseJson == null) {
      throw new GateRuntimeException("Invalid null response from Python process, did you run interact()?");
    }
    try {
      Map<String, Object> response = JSON.std.mapFrom(responseJson);
      if(!response.containsKey("status") || !"ok".equals(response.get("status"))) {
        throw new GateRuntimeException("Something went wrong, start response is "+responseJson);
      }
    } catch (IOException ex) {
      throw new GateRuntimeException("Could not convert start response", ex);
    }
  }

  protected void whenFinishing() {
    runningDuplicates.getAndDecrement();
    process.process(makeFinishRequest());
    // TODO: here or around here we need to do the python process result 
    // processing at some point!
    // If we have only one duplicate, call the result method with no
    // argument, this should tell the method to use its own result only.
    // If we have more than one duplicate, collect all the results from the
    // finishing method and call the result method 
    // TODO TODO TODO
    int exitValue = process.stop();
    if(exitValue != 0) {
      logger.info("Warning: python process ended with exit value "+exitValue);
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
    if(registeredEditorVR != null) {
      registeredEditorVR.setFile(getCurrentPythonProgramFile());
    }
    super.reInit();
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
   * @throws ExecutionException exception when processing fails
   */
  @Override
  public void execute() throws ExecutionException {
    if(isInterrupted()) {
      throw new ExecutionException("Processing was interrupted");
    }
    ensureProcess();
    // send over the current document in an execute command
    // get back the changelog in a result object (or some error)
    // if we get a changelog apply the changelog to the document
    // if we get an error, throw an error condition and abort the process
    String responseJson = (String)process.process(makeExecuteRequest(document));
    try {
      Response response = JSON.std.beanFrom(Response.class, responseJson);
      if(!"ok".equals(response.status)) {
        throw new GateRuntimeException("Error processing document: "+response.error+
                ", additional info: "+response.info);
      }
      ChangeLog chlog = response.data;
      if(chlog == null) {
        throw new GateRuntimeException("Got null changelog back from process");
      }
      new GateDocumentUpdater(document).fromChangeLog(chlog);
    } catch (IOException ex) {
      throw new GateRuntimeException("Could not convert execute response JSON: "+responseJson, ex);
    }
  }

  /**
   * Callback when running over a whole corpus starts.
   * @param controller the controller used
   */
  @Override
  public void controllerExecutionStarted(Controller controller) {
    whenStarting();
  }

  /**
   * Callback when running over a whole corpus finishes. 
   * @param controller the controller used
   */
  @Override
  public void controllerExecutionFinished(Controller controller) {
    whenFinishing();
  }

  /**
   * Callback when running over a wholr corpus terminates with an exception.
   * @param controller the controller used
   * @param throwable the throwable from the exception
   */
  @Override
  public void controllerExecutionAborted(Controller controller, Throwable throwable) {
    whenFinishing();
    throw new GateRuntimeException("Exception when running pipeline",throwable);
  }

  
  static protected class Response {
    public String status;
    public String error;
    public String info;
    public ChangeLog data;
  }
  
  protected String getResponseError(Map<String,Object> response) {
    String error = (String) response.get("error");
    if (error == null) {
      error = "(No error description from process)";
    }
    return error;
  }

  protected String getResponseInfo(Map<String,Object> response) {
    String info = (String) response.get("info");
    if (info == null) {
      info = "(No additional error infor from process)";
    }
    return info;
  }
  
  /**
   * Create and return the JSON String representing an execute request.
   * @param doc the document to send over
   * @return JSON string
   */
  protected String makeExecuteRequest(Document doc) {
    Map<String, Object> request = new HashMap<>();
    request.put("command", "execute");
    // create the BdocDocument from our document    
    BdocDocument bdoc = new BdocDocumentBuilder().fromGate(document).buildBdoc();
    bdoc.features.put("gate.plugin.python.docName", document.getName());
    request.put("data", bdoc);
    try {
      return JSON.std.asString(request);
    } catch (IOException ex) {
      throw new GateRuntimeException("Error when trying to convert execute request to JSON", ex);
    }
  }
  
  
  protected String makeStartRequest() {
    Map<String, Object> request = new HashMap<>();
    request.put("command", "start");
    Map<String,Object> params = BdocUtils.featureMap2Map(programParams, null);
    params.put("gate_plugin_python_duplicateId", duplicateId);
    params.put("gate_plugin_python_nrDuplicates", nrDuplicates);   
    params.put("gate_plugin_python_workingDir", workingDir.getAbsolutePath());
    params.put("gate_plugin_python_pythonFile", currentPythonProgramFile.getAbsolutePath());
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
      throw new GateRuntimeException("Error when trying to convert start request to JSON", ex);
    }    
  }
  
/**
   * Copy resource from plugin jar to target path.
   * NOTE: normally this copies from whatever JAR file the creole.xml for this
   * PR is in, but when running the gapploading test, the creole.xml is in
   * the target classes directory instead and we then need to copy in a 
   * different way.
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
    if(artifactURL.toString().startsWith("file:/")) {
      try {
        File containingDir = gate.util.Files.fileFromURL(artifactURL);
        File fromFile = new File(containingDir, source);
        logger.info("Copying python file from "+fromFile+" to "+targetPath);
        java.nio.file.Files.copy(
                fromFile.toPath(),
                targetPath.toPath());
      } catch (IOException ex) {
        throw new GateRuntimeException("Error trying to copy the resources", ex);
      }
    } else {
      try (
            FileSystem zipFs
            = FileSystems.newFileSystem(artifactURL.toURI(), new HashMap<>()); 
          ) {      
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
  
  
}
