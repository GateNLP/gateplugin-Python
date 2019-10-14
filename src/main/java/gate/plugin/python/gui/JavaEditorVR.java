/* 
 * Copyright (C) 2014-2016 The University of Sheffield.
 *
 * This file is part of gateplugin-Java
 * (see https://github.com/johann-petrak/gateplugin-Java)
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
package gate.plugin.python.gui;

import gate.plugin.python.PythonCodeDriven;
import gate.plugin.python.PythonScripting;
import gate.Resource;
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
public class PythonEditorVR extends AbstractVisualResource 
{
  
  protected PythonEditorPanel panel;
  protected PythonCodeDriven theTarget;
  protected PythonScripting pr = null;
  
  @Override
  public void setTarget(Object target) {
    if(target instanceof PythonCodeDriven) {
      //System.out.println("Found a PythonCodeDriven, activating panel");
      theTarget = (PythonCodeDriven)target;
      panel = new PythonEditorPanel();
      this.add(panel);
      this.setLayout(new GridLayout(1,1));
      // register ourselves as the EditorVR
      pr = (PythonScripting)target;
      pr.registerEditorVR(this);
      panel.setPR(pr);
      panel.setFile(pr.getJavaProgramFile());
      if(pr.isCompileError) {
        panel.setCompilationError();
      } else {
        panel.setCompilationOk();
      }
    } else {
      //System.out.println("Not a JavaCodeDriven: "+((Resource)target).getName());
    }
  }
  
  public void setFile(File file) {
    panel.setFile(file);
  }
  
  public void setCompilationError() {
    panel.setCompilationError();
  }
  public void setCompilationOk() {
    panel.setCompilationOk();
  }
  
}
