/* 
 * Copyright (C) 2014-2016 The University of Sheffield.
 *
 * This file is part of gateplugin-python
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software. If not, see <http://www.gnu.org/licenses/>.
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
import gate.util.MethodNotImplementedException;
import java.io.File;
import java.io.IOException;
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
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.io.FileUtils;


// TODO:
// * use a working directory: this will be the current directory where the 
//   python process is run!
//   * if gatenlp is not installed, will copy gatenlp package dir there
//   * if the program URL is not a file, will copy/edit program there
// * maybe allow to specify python environment to use (how to set??)
//   * document: specify the python command from the environment as teh
// * duplication: pass duplication number as system parameter
//   * if pipe interaction, all duplicates do exactly the same
//   * if http interaction, for now all duplicates do the same as well???

@CreoleResource(
        name = "Python PR",
        helpURL = "https://github.com/gatenlp/gateplugin-python/wiki/PythonPr",
        comment = "Use a Python program as a processing resource")
public class PythonPr
        extends AbstractLanguageAnalyser
        implements ControllerAwarePR, PythonCodeDriven
{

  private static final long serialVersionUID = -7294093586613502768L;

  
  @Optional
  @RunTime
  @CreoleParameter(
          comment = "The URL of the Python program to run",
          disjunction = "program",
          priority = 0,
          suffixes = ".py")
  public void setPythonProgram(ResourceReference value) {
    pythonProgram = value;
  }
  public ResourceReference getPythonProgram() {
    return pythonProgram;
  }
  protected ResourceReference pythonProgram;

  @Optional
  @RunTime
  @CreoleParameter(
          comment = "An absolute or relative file path to the python program",
          disjunction = "program",
          priority = 1,
          suffixes = ".py")
  public void setPythonProgramPath(String value) {
    pythonProgramPath = value;
  }
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

  @Optional
  @RunTime
  @CreoleParameter(comment = "The input annotation set", defaultValue = "")
  public void setInputAS(String asname) {
    inputAS = asname;
  }

  public String getInputAS() {
    return inputAS;
  }
  protected String inputAS;

  @Optional
  @RunTime
  @CreoleParameter(comment = "The output annotation set", defaultValue = "")
  public void setOutputAS(String asname) {
    outputAS = asname;
  }

  public String getOutputAS() {
    return outputAS;
  }
  protected String outputAS;

  @Optional
  @RunTime
  @CreoleParameter(comment = "Extra parameters to pass on to the Python program", defaultValue = "")
  public void setProgramParams(FeatureMap parms) {
    programParams = parms;
  }
  public FeatureMap getProgramParams() {
    return programParams;
  }
  protected FeatureMap programParams;

  @Optional
  @RunTime
  @CreoleParameter(
          comment = "Python interpreter name (on system PATH)", 
          disjunction = "pythonbin",
          priority = 10,
          defaultValue = "python3")
  public void setPythonBinary(String value) {
    pythonBinary = value;
  }
  public String getPythonBinary() {
    return pythonBinary;
  }
  protected String pythonBinary;
  
  @Optional
  @RunTime
  @CreoleParameter(
          comment = "Python interpreter file URL. If provided overrides pythonBinary.",
          priority = 1,
          disjunction = "pythonbin"
          )
  public void setPythonBinaryUrl(URL value) {
    pythonBinaryUrl = value;
  }
  public URL getPythonBinaryUrl() {
    return pythonBinaryUrl;
  }
  protected URL pythonBinaryUrl;
  
  protected String pythonBinaryCommand;
  
  @Optional
  @RunTime
  @CreoleParameter(comment = "Working directory.")
  public void setWorkingDirUrl(URL value) {
    workingDirUrl = value;
  }
  public URL getWorkingDirUrl() {
    return workingDirUrl;
  }
  protected URL workingDirUrl;
  protected File workingDir;
  
  
  @Optional
  @RunTime
  @CreoleParameter(comment = "Enable debugging mode", defaultValue = "false")
  public void setDebugMode(Boolean value) {
    debugMode = value;
  }
  public Boolean getDebugMode() {
    if(debugMode == null) {
      return false;
    }
    return debugMode;
  }
  protected Boolean debugMode;
          
  @Optional
  @RunTime
  @CreoleParameter(comment = "Use Python gatenlp package included in the plugin, not the system one.",
          defaultValue = "true")
  public void setOwnGatenlpPackage(Boolean value) {
    ownGatenlpPackage = value;
  }
  public Boolean getOwnGatenlpPackage() {
    if (ownGatenlpPackage == null) {
      return true;
    }
    return ownGatenlpPackage;
  }
  protected Boolean ownGatenlpPackage;
  
  /**
   * This field contains the currently active process for the python program.
   * Otherwise, the field should be null.
   * 
   */
  protected transient Process4StringStream process = null; 
            
  public transient org.apache.log4j.Logger logger = 
          org.apache.log4j.Logger.getLogger(this.getClass());
  
  // the nrDuplicates counter will get shared between copies when this
  // PR is being duplicated. We will do a synchronized increment of the 
  // counter in our own duplication method.
  // NOTE: the first, initial PR will have NrDuplicates set to 0, the
  // actual duplicates will get numbers 1, 2, 3 ...
  // (so the first instance does NOT count as a duplicate)
  
  @Sharable
  public void setNrDuplicates(AtomicInteger value) {
    nrDuplicates = value;
  }
  public AtomicInteger getNrDuplicates() {
    return nrDuplicates;
  }
  protected AtomicInteger nrDuplicates;
  
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
    figureOutPythonFile();
    cmdLine.addArgument(currentPythonProgramFile.getAbsolutePath());
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
    if(getOwnGatenlpPackage()) {
      env.put("PYTHONPATH", usePythonPackagePath);
    }
    try {
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
  public boolean isCompileOk = true;
  
  private PythonEditorVr registeredEditorVR = null;

  public void registerEditorVR(PythonEditorVr vr) {
    registeredEditorVR = vr;
  }
  // TODO: make this atomic so it works better in a multithreaded setting
  private static int idNumber = 0;

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

    if(workingDirUrl == null) {
      workingDir = new File(".");
    } else {
      workingDir = Files.fileFromURL(workingDirUrl);
    }
    File pythonProgramFile = null;   // the file we will end up using
    // if both parms are empty and we need a python program file, we create 
    // a temporary one in the working directory from the program template
    if (pythonProgram == null && (pythonProgramPath == null || pythonProgramPath.isEmpty())) {
      System.err.println("Creating new Python file with tmp-name from template");
      String tmpfilename = "tmpfile.py";
      pythonProgramFile = new File(workingDir, tmpfilename);
      if(!pythonProgramFile.exists()) {          
        copyResource("/resources/templates/default.py", pythonProgramFile);
      }
      currentPythonProgramFile = pythonProgramFile;
      return pythonProgramFile;      
    }
    
    // If the pythonProgramPath is set, it takes precedence of ther the pythonProgram
    if(pythonProgramPath != null && !pythonProgramPath.isEmpty()) {
      // if the path is absolute, use just that file, otherwise, make it
      // relative to the working directory, not the current directory
      File tmpfile = new File(pythonProgramPath);
      if(tmpfile.isAbsolute()) {
        pythonProgramFile = tmpfile;
      } else {
        pythonProgramFile = new File(workingDir, pythonProgramPath);
      }
    }
    // We have two cases: either the resourcereference is pointing at a file,
    // then we just use that, or it is a creole reference or gettable URL,
    // then we copy the file to whatever our working directory is.
    // If the file already exists there already, it is not copied and used
    // instead. 
    // NOTE: for now, the editor will always edit the actual file which may
    // be a copy.
    
    // TODO/NOTE: The ResourceReference dialog does not allow to enter a relative file
    // URI, and defaults to creole: scheme. So it is really hard to easily 
    // specify a new file there. For now we have to live with this!
    
    // OK, this got more complicated by the need to split the parameter into two
    // If we have pythonProgramPath, use that as a file, if it is a relative
    // path use relative to the working dir. Create the file URI for that 
    // path and use it. Otherwise use the pythonProgramUri. Then
    // do for the URI what we described above.
    
    //System.err.println("DEBUG: python program URI: "+pythonProgram.toURI());
    //System.err.println("DEBUG: python program scheme: "+pythonProgram.toURI().getScheme());
    if(pythonProgram.toURI().getScheme().equals("file")) {
      try {
        pythonProgramFile = gate.util.Files.fileFromURL(pythonProgram.toURL());
        if(!pythonProgramFile.exists()) {          
          copyResource("/resources/templates/default.py", pythonProgramFile);
        }
      } catch (IOException ex) {
        throw new GateRuntimeException("Could not determine file for pythonProgram "+pythonProgram, ex);
      }
    } else {
      // check if the working directory already contains a file with the same name
      // Get the filename for the URL ... this better be a URL that has something like a file name!
      URI pythonProgramUri = pythonProgram.toURI();
      String tmpfilename = Paths.get(pythonProgramUri.getPath()).getFileName().toString();
      pythonProgramFile = new File(workingDir, tmpfilename);
      if(pythonProgramFile.exists()) {
        logger.warn("Not copying "+pythonProgram+" to "+pythonProgramFile+", already exists!");
      } else {
        // try to read the program from the URL and write it to the file
        throw new MethodNotImplementedException("Running from read-only URL not implemented yet");
      }
    }
    try {
      // just check if we can read the script here ... what we read is not actually 
      // ever used
      String tmp = FileUtils.readFileToString(pythonProgramFile, "UTF-8");
    } catch (IOException ex) {
      throw new GateRuntimeException("Could not read the python program from " + pythonProgramFile, ex);
    }
    // TODO: maybe also check if the program syntax is ok here!!
    currentPythonProgramFile = pythonProgramFile;
    return pythonProgramFile;
  } // end figureOutPythonFile
  
  @Override
  public Resource init() throws ResourceInstantiationException {
    usePythonPackagePath = PythonPr.getPackageParentPathInZip();
    //System.err.println("DEBUG: pythonpath is "+usePythonPath);
    // count which duplication id we have, the first instance gets null, the 
    // duplicates will find the instance from the first instance
    if(nrDuplicates==null) {
      nrDuplicates = new AtomicInteger(1);      
      duplicateId = 0;
    } else {
      duplicateId = nrDuplicates.getAndAdd(1);
    }
    //System.err.println("Duplicate id is "+duplicateId);
    return this;
  } // end init()

  /**
   * This will run whenever a corpus gets run.
   * We start the python process for every new run on a corpus and stop
   * it when the corpus is finished. 
   */
  protected void whenStarting() {
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
    if(getOwnGatenlpPackage()) {
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
    String responseJson = (String)process.process(makeFinishRequest());
    // TODO: here or around here we need to do the python process result 
    // processing at some point!
    int exitValue = process.stop();
    if(exitValue != 0) {
      System.err.println("Warning: python process ended with exit value "+exitValue);
    }
    
  }
  
  
  @Override
  public void reInit() throws ResourceInstantiationException {
    nrDuplicates = null;
    if(registeredEditorVR != null) {
      registeredEditorVR.setFile(getCurrentPythonProgramFile());
    }
    super.reInit();
  }

  @Override
  public void cleanup() {
    super.cleanup();
  }
  
  private void ensureProcess() throws ExecutionException {
    if (!(process != null && process.isAlive())) {
      throw new ExecutionException("Python process not alive during execution");
    }
  }
  

  @Override
  public void execute() throws ExecutionException {
    ensureProcess();
    // send over the current document in an execute command
    // get back the changelog in a result object (or some error)
    // if we get a changelog apply the changelog to the document
    // if we get an error, throw an error condition and abort the process
    String responseJson = (String)process.process(makeExecuteRequest(document));
    try {
      Response response = JSON.std.beanFrom(Response.class, responseJson);
      /*
      if(!response.containsKey("status")) {
        throw new GateRuntimeException("Execute response does not contain a status: "+responseJson);
      }
      if(!response.get("status").equals("ok")) {
        throw new GateRuntimeException("Error processing document: "+getResponseError(response)+
                ", additional info: "+getResponseInfo(response));
      }
      */
      ChangeLog chlog = response.data;
      if(chlog == null) {
        throw new GateRuntimeException("Got null changelog back from process");
      }
      new GateDocumentUpdater(document).fromChangeLog(chlog);
    } catch (IOException ex) {
      throw new GateRuntimeException("Could not convert execute response JSON: "+responseJson, ex);
    }
  }

  
  @Override
  public void controllerExecutionStarted(Controller controller) {
    whenStarting();
  }

  @Override
  public void controllerExecutionFinished(Controller controller) {
    whenFinishing();
  }

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
    params.put("gate.plugin.python.duplicateId", duplicateId);
    params.put("gate.plugin.python.nrDuplicates", nrDuplicates);            
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
   * @param source the path of the resource to copy
   * @param targetPath where to copy to, must not already exist
   */
  public static void copyResource(String source, File targetPath) {

    // TODO: check targetDir is a dir?
    //if (!hasResources())
    //  throw new UnsupportedOperationException(
    //      "this plugin doesn't have any resources you can copy as you would know had you called hasResources first :P");
    URL artifactURL = PythonPr.class.getResource("/creole.xml");
    try {
      artifactURL = new URL(artifactURL, ".");
    } catch (MalformedURLException ex) {
      throw new GateRuntimeException("Could not get jar URL");
    }
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
