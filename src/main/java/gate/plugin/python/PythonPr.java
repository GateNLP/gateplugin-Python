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
import gate.Document;
import gate.FeatureMap;
import gate.LanguageResource;
import gate.ProcessingResource;

import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ControllerAwarePR;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.creole.metadata.Sharable;
import gate.util.MethodNotImplementedException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
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
        // TODO: rethink duplication!!!
        //, CustomDuplication 
{

  private static final long serialVersionUID = -7294092586613502768L;

  // ********* Parameters
  @CreoleParameter(comment = "The URL of the Python program to run",suffixes = ".py")
  public void setPythonProgramUrl(URL surl) {
    pythonProgramUrl = surl;
  }

  public URL getPythonProgramUrl() {
    return pythonProgramUrl;
  }
  protected URL pythonProgramUrl;

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
  
  // NOTE: I think we have this so we can use the PR in other ways??
  @Override
  @Optional
  @RunTime
  @CreoleParameter()
  public void setDocument(Document d) {
    document = d;
  }
    
  public FeatureMap getProgramParams() {
    return programParams;
  }
  
            
  public org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(this.getClass());
  
  protected FeatureMap programParams;

  //NEEDED? Controller controller = null;
  File pythonProgramFile = null;
  // this is used by the VR
  public File getPythonProgramFile() { return pythonProgramFile; }

  
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
  
  
  // This tries to compile the python program by running python and trying to
  // impoert the script. A syntax error should be detectable!
  public boolean tryCompileProgram() {
    throw new MethodNotImplementedException("tryCompileProgram not yet implemented");
  }
  
  // We need this so that the VR can determine if the latest compile was
  // an error or ok. This is necessary if the VR gets activated after the
  // compilation.
  public boolean isCompileError;
  
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
    if (getPythonProgramUrl() == null) {
      throw new ResourceInstantiationException("The pythonProgramUrl must not be empty");
    }
    pythonProgramFile = gate.util.Files.fileFromURL(getPythonProgramUrl());
    try {
      // just check if we can read the script here ... what we read is not actually 
      // ever used
      String tmp = FileUtils.readFileToString(pythonProgramFile, "UTF-8");
    } catch (IOException ex) {
      throw new ResourceInstantiationException("Could not read the python program from " + getPythonProgramUrl(), ex);
    }
    isCompileError = tryCompileProgram();
    return this;
  }

  @Override
  public void reInit() throws ResourceInstantiationException {
    if(registeredEditorVR != null) {
      registeredEditorVR.setFile(getPythonProgramFile());
    }
    init();
  }

  @Override
  public void cleanup() {
    super.cleanup();
  }

  @Override
  public void execute() {
    // TODO: properly handle manual interruption here, or in cooperation
    // with the actual script!!
    // Check out how to use variable "interrupted" and method "isInterrupted()"
    // properly!
    // TODO: actually send an execute command over to the python program
  }

  
  @Override
  public void controllerExecutionStarted(Controller controller) {
    // TODO: send started command over to the program
  }

  @Override
  public void controllerExecutionFinished(Controller controller) {
    // TODO: send finished command over to the program
  }

  @Override
  public void controllerExecutionAborted(Controller controller, Throwable throwable) {
    // TODO: send aborted command over to the program
  }

  // TODO: rethink how to handle duplication properly!!!
  /*
  @Override
  public Resource duplicate(Factory.DuplicationContext dc) throws ResourceInstantiationException {
    JavaScriptingPR res = (JavaScriptingPR) Factory.defaultDuplicate(this, dc);
    int nr = nrDuplicates.addAndGet(1);
    if(res.javaProgramClass != null) {
      res.javaProgramClass.duplicationId = nr;
    }
    return res;
  }
*/
    
  public static interface LrOrPr extends LanguageResource, ProcessingResource { }
  
  
}
