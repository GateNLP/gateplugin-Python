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

import gate.plugin.python.gui.PythonEditorVr;
import java.net.URL;

import gate.Resource;
import gate.Controller;
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
import gate.lib.interaction.process.Process4StringStream;
import gate.util.Files;
import gate.util.GateRuntimeException;
import gate.util.MethodNotImplementedException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
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
// * add parameters for python binary: name and URL
// * maybe allow to specify python environment to use (how to set??)
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

  private static final long serialVersionUID = -7294092586613502768L;

  // ********* Parameters
  @CreoleParameter(comment = "The URL of the Python program to run",suffixes = ".py")
  public void setPythonProgram(ResourceReference value) {
    pythonProgram = value;
  }

  public ResourceReference getPythonProgram() {
    return pythonProgram;
  }
  protected ResourceReference pythonProgram;
  
  File pythonProgramFile = null;
  public File getPythonProgramFile() { return pythonProgramFile; }

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
  @CreoleParameter(comment = "Python interpreter name (on system PATH)", defaultValue = "python")
  public void setPythonBinary(String value) {
    pythonBinary = value;
  }
  public String getPythonBinary() {
    return pythonBinary;
  }
  protected String pythonBinary;
  
  @Optional
  @RunTime
  @CreoleParameter(comment = "Python interpreter file URL. If provided overrides pythonBinary.")
  public void setPythonBinaryUrl(URL value) {
    pythonBinaryUrl = value;
  }
  public URL getPythonBinaryUrl() {
    return pythonBinaryUrl;
  }
  protected URL pythonBinaryUrl;
  
  protected String pythonBinaryCommand;
  
  @Optional
  @CreoleParameter(comment = "Working directory.")
  public void setWorkingDirUrl(URL value) {
    workingDirUrl = value;
  }
  public URL getWorkingDirUrl() {
    return workingDirUrl;
  }
  protected URL workingDirUrl;
  protected File workingDir;
  
  
  /**
   * This field contains the currently active process for the python program.
   * Otherwise, the field should be null.
   * 
   */
  protected Process4StringStream process = null; 
            
  public org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(this.getClass());
  
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
    System.err.println("PythonBinary command set to "+pythonBinaryCommand);
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
    cmdLine.addArgument(pythonProgramFile.getAbsolutePath());
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
    try {
      executor.execute(cmdLine, resultHandler);
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

  private static synchronized String getNextId() {
    idNumber++;
    return ("" + idNumber);
  }


  @Override
  public Resource init() throws ResourceInstantiationException {
    // count which duplication id we have, the first instance gets null, the 
    // duplicates will find the instance from the first instance
    if(nrDuplicates==null) {
      nrDuplicates = new AtomicInteger(1);      
      duplicateId = 0;
    } else {
      duplicateId = nrDuplicates.getAndAdd(1);
    }
    System.err.println("Duplicate id is "+duplicateId);
    
    if(workingDirUrl == null) {
      workingDir = new File(".");
    } else {
      workingDir = Files.fileFromURL(workingDirUrl);
    }
    if (pythonProgram == null) {
      throw new ResourceInstantiationException("The pythonProgram parameter must not be empty");
    }
    // We have two cases: either the resourcereference is pointing at a file,
    // then we just use that, or it is a creole reference or gettable URL,
    // then we copy the file to whatever our working directory is.
    // If the file already exists there already, it is not copied and used
    // instead. 
    // NOTE: for now, the editor will always edit the actual file which may
    // be a copy.
    System.err.print("DEBUG: python program scheme: "+pythonProgram.toURI().getScheme());
    if(pythonProgram.toURI().getScheme().equals("file")) {
      try {
        pythonProgramFile = gate.util.Files.fileFromURL(pythonProgram.toURL());
      } catch (IOException ex) {
        throw new ResourceInstantiationException("Could not determine file for pythonProgram "+pythonProgram, ex);
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
      throw new ResourceInstantiationException("Could not read the python program from " + pythonProgramFile, ex);
    }
    // NOTE: at this point we cannot already check the python program because
    // we do not know the python binary yet (which is a runtime parameter).
    return this;
  } // end init()

  /**
   * This will run whenever a corpus gets run.
   * We start the python process for every new run on a corpus and stop
   * it when the corpus is finished. 
   */
  protected void whenStarting() {
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
    process = Process4StringStream.create(workingDir, env, pythonBinaryCommand, pythonProgramFile.getAbsolutePath());
    // send over the starting command with additional data (script params, duplication id)
    // !!!!TODO:
    process.writeObject("");
    String responseString = (String)process.readObject();
    // check if the response is ok
  }

  protected void whenFinishing() {
    // Send over the finish command, get back any over-corpus results
    // !!! TODO    
    process.writeObject("");
    String responseString = (String)process.readObject();
    
    // shutdown the process
    int exitValue = process.stop();
    throw new GateRuntimeException("Python process ended with exit value "+exitValue);
  }
  
  
  @Override
  public void reInit() throws ResourceInstantiationException {
    nrDuplicates = null;
    if(registeredEditorVR != null) {
      registeredEditorVR.setFile(getPythonProgramFile());
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

    
  
}
