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
package gate.plugin.python.tests;

import gate.Factory;
import gate.FeatureMap;
import gate.creole.ResourceInstantiationException;
import gate.gui.ResourceHelper;
import gate.test.GATEPluginTestCase;
import java.io.IOException;

/**
 * Class for testing.
 * @author Johann Petrak
 */
public class PythonSlaveRunnerTest extends GATEPluginTestCase {

  /**
   * A test.
   * @throws IOException   exception
   * @throws gate.creole.ResourceInstantiationException exception
   */
  public void testPythonSlaveRunner() throws IOException, ResourceInstantiationException {
    // Try to instantiate the Slave runner
    FeatureMap parms = Factory.newFeatureMap();
    parms.put("port", 25333);
    parms.put("host", "127.0.0.1");
    ResourceHelper slave = 
            (ResourceHelper)Factory.createResource("gate.plugin.python.PythonSlaveRunner", parms);
    System.err.println("!!!!!!!!!SLAVE:"+slave);
  }
  
}
