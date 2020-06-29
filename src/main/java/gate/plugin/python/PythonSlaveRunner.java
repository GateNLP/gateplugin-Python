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

import gate.Gate;
import gate.Resource;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.gui.NameBearerHandle;
import gate.gui.ResourceHelper;
import gate.util.GateRuntimeException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import py4j.GatewayServer;

/**
 * Resource Helper for running the Python slave process.
 * 
 * This is meant to get used by a python process that starts the GATE 
 * process in order to use the gate slave. It differs from the PythonSlaveLr
 * in that it uses standard output to communicate success/failure of starting
 * the Gateway to Python and in that it will immediately terminate the 
 * Java process if something goes wrong or after a shutdown. 
 * 
 * @author Johann Petrak
 */
@CreoleResource(name = "PythonSlaveRunner",
        tool = true,
        comment = "Language Resource to represent a Python slave process.",        
        helpURL = "")
public class PythonSlaveRunner extends ResourceHelper  {
  private static final long serialVersionUID = -1392982236343502768L;
  
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
  
  /**
   * Set host.
   * 
   * @param value host address to bind to
   */
  @Optional
  @CreoleParameter(
          comment = "Host address",
          defaultValue = "127.0.0.1"
  )  
  public void setHost(String value) {
    host = value;
  }
  /**
   * Get the host address.
   * 
   * @return host address
   */
  public String getHost() {
    return host;
  }
  protected String host = "127.0.0.1";
  
  public PythonSlave slave = null;
  
  @Override
  @SuppressWarnings("unchecked") 
  public Object call(String action, Resource resource, Object... params) {
    switch(action) {
      case "set_port":
        port = (Integer)params[0];
        break;
      case "set_host":        
        host = (String)params[0];
        break;
      case "start":
        slave = new PythonSlave();
        slave.port = port;
        startServer(slave);
      case "stop":
        stopServer(slave);
      default:
        throw new GateRuntimeException("Not a known action: "+action);
    }
    return null;
  }

  /**
   * Start the server. 
   * 
   * This writes a special string to stdout indicating if the server has been
   * started or not which is used by the starting process to wait until the 
   * server is ready. 
   * 
   * @param pslave the python slave instance that owns the server
   */
  public void startServer(PythonSlave pslave) {
    Thread.currentThread().setContextClassLoader(Gate.getClassLoader());
    GatewayServer server = new GatewayServer(pslave, port);
    pslave.server = server;
    try {
      server.start();
      System.out.println("PYTHON SLAVE SERVER OK");
    } catch(Exception ex) {
      pslave.server = null;
      System.out.println("PYTHON SLAVE SERVER NOT OK");
      throw new GateRuntimeException("Could not start GatewayServer",ex);
    }
  }
  
  /**
   * Stop the server.
   * 
   * This will delay shutting down by one second in order to allow the 
   * python process to still get the response from the java process when
   * sending the stopServer request. 
   * 
   * @param pslave the PythonSlave instance that owns the server.
   */
  public void stopServer(PythonSlave pslave) {
    if(pslave.server != null) {
      Thread runner = new Thread() {
        @Override
        public void run() {
          try {Thread.sleep(1000); } catch(InterruptedException ex) {}
          pslave.server.shutdown();
        }
      };
      runner.start();
    }
  }

  @Override
  protected List<Action> buildActions(NameBearerHandle nbh) {
    return new ArrayList<>();
  }
  
  
  
  
}
