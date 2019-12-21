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
package gate.plugin.python.gui;

import gate.plugin.python.PythonCodeDriven;
import gate.plugin.python.PythonPr;
import gate.creole.AbstractVisualResource;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.GuiType;
import java.awt.GridLayout;
import java.io.File;


/**
 *
 * @author johann
 */
@CreoleResource(
  name = "Python Code Editor", 
  comment = "Editor for Python code", 
  guiType = GuiType.LARGE, 
  mainViewer = true, 
  resourceDisplayed = "gate.plugin.python.PythonCodeDriven")
public class PythonEditorVr extends AbstractVisualResource 
{

  private static final long serialVersionUID = 2225798440338388811L;
  
  protected PythonEditorPanel panel;
  protected PythonCodeDriven theTarget;
  protected PythonPr pr = null;
  
  /**
   * Set the target PR. 
   * @param target associated PR.
   */
  @Override
  public void setTarget(Object target) {
    if(target instanceof PythonCodeDriven) {
      theTarget = (PythonCodeDriven)target; 
      pr = (PythonPr)target;      
      if(!pr.pythonFileCanBeEdited()) {
        return;
      }
      panel = new PythonEditorPanel();
      this.add(panel);
      this.setLayout(new GridLayout(1,1));
      // register ourselves as the EditorVR
      
      pr.registerEditorVR(this);
      panel.setPR(pr);
      pr.figureOutPythonFile();
      pr.tryCompileProgram();
      panel.setFile(pr.getPythonProgramFile());
      if(!pr.isCompileOk) {
        panel.setCompilationError();
      } else {
        panel.setCompilationOk();
      }
    } else {
      //System.out.println("Not a JavaCodeDriven: "+((Resource)target).getName());
    }
  }
  
  /**
   * Set the file to edit.
   * @param file file to edit.
   */
  public void setFile(File file) {
    panel.setFile(file);
  }
  
  /**
   * Notify of compilation error.
   */
  public void setCompilationError() {
    panel.setCompilationError();
  }
  /**
   * Notify of successful compilation.
   */
  public void setCompilationOk() {
    panel.setCompilationOk();
  }
  
}
