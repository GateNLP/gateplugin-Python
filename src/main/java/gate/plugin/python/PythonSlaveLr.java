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

import gate.Resource;
import gate.creole.AbstractLanguageResource;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;

/**
 * Language Resource for running the Python slave process.
 * This is a language resource so we can interact with the initialized resource
 * easier.
 * @author Johann Petrak
 */
@CreoleResource(name = "PythonSlaveLr",
        comment = "Language Resource to represent a Python slave process.",
        helpURL = "")
public class PythonSlaveLr extends AbstractLanguageResource  {
  private static final long serialVersionUID = -1392763456343502768L;
  
  /**
   * Our logger instance.
   */
  public transient org.apache.log4j.Logger logger
          = org.apache.log4j.Logger.getLogger(this.getClass());
  
  
  /**
   * Set port.
   * 
   * @param value port number
   */
  @Optional
  @CreoleParameter(
          comment = "Port number to use.",
          defaultValue = "25333"
  )  
  public void setPort(Integer value) {
    port = value;
  }
  /**
   * Get the port number.
   * @return port
   */
  public Integer getPort() {
    return port;
  }
  protected Integer port = 25333;
  
  
  protected transient PythonSlave pythonSlave;
  
  @Override
  public Resource init() {
    pythonSlave = new PythonSlave();
    pythonSlave.port = port;
    pythonSlave.startServer();
    logger.info("Python slave started at port "+port);
    return this;
  }
  
  @Override
  public void cleanup() {
    pythonSlave.stopServer();
    super.cleanup();
  }
  
}