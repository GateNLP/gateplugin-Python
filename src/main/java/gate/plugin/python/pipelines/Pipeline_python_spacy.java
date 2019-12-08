/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.plugin.python.pipelines;

import gate.creole.PackagedController;
import gate.creole.metadata.AutoInstance;
import gate.creole.metadata.AutoInstanceParam;
import gate.creole.metadata.CreoleResource;

/**
 * Class for providing the ready made application.
 * @author Johann Petrak, johann.petrak@gmail.com
 */
@CreoleResource(
        name = "python-spacy", 
        comment = "Example pipeline to run Python spacy on documents",
        icon = "gateplugin-python", 
        autoinstances = @AutoInstance(parameters = {
          @AutoInstanceParam(name="pipelineURL", value="resources/pipelines/python-spacy.xgapp"), 
          @AutoInstanceParam(name="menu", value="Python")})) 
public class Pipeline_python_spacy  extends PackagedController {
  private static final long serialVersionUID = -1892123999555424276L;
}
