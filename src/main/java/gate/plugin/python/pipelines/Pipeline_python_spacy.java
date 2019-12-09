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
