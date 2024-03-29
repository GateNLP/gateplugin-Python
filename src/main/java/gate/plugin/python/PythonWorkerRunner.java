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
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.gui.NameBearerHandle;
import gate.gui.ResourceHelper;
import gate.util.GateRuntimeException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import py4j.GatewayServer;

/**
 * Resource Helper for running the Python worker process.
 * 
 * This is meant to get used by a python process that starts the GATE 
 * process in order to use the gate worker. It differs from the PythonWorkerLr
 * in that it uses standard output to communicate success/failure of starting
 * the Gateway to Python and in that it will immediately terminate the 
 * Java process if something goes wrong or after a shutdown. 
 * 
 * @author Johann Petrak
 */
@CreoleResource(name = "PythonWorkerRunner",
        tool = true,
        comment = "Language Resource to represent a Python worker process.",        
        helpURL = "")
public class PythonWorkerRunner extends ResourceHelper  {
  private static final long serialVersionUID = -1392982236343502768L;
  
  /**
   * Our logger instance.
   */
  public transient org.apache.log4j.Logger LOGGER
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
  
  private transient PythonWorker worker = null;

  /**
   * Set auth token to use.
   *
   * @param value: the auth token to use or an empty String to use none.
   */
  @Optional
  @CreoleParameter(comment="Auth Token", defaultValue="")
  public void setAuthToken(String value) { authToken = value;}

  /**
   * Get auth Token.
   *
   * @return the auth token
   */
  public String getAuthToken() { return authToken; }
  protected String authToken = "";

  /**
   * Set whether or not to log actions.
   *
   * @param val flag
   */
  @CreoleParameter(comment="If actions should get logged", defaultValue="false")
  public void setLogActions(Boolean val) { logActions = val; }

  /**
   * Get if actions get logged.
   * @return flag
   */
  public Boolean getLogActions() { return logActions; }
  protected Boolean logActions = false;


  @Optional
  @CreoleParameter(comment="Keep running", defaultValue="false")
  public void setKeep(Boolean val) { keep = val; }
  public Boolean getKeep() { return keep; }
  protected Boolean keep = false;

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
        try {
          worker = new PythonWorker();
        } catch(ResourceInstantiationException ex) {
          throw new GateRuntimeException("Could not create PythonWorker", ex);
        }
        startServer(worker);
        break;

      default:
        throw new GateRuntimeException("Not a known action: "+action);
    }
    return null;
  }

  /**
   * Initialize the resource
   *
   * @return the resource
   */
  public Resource init() {

    return this;
  }

  /**
   * Start the server. 
   * 
   * This writes a special string to stdout indicating if the server has been
   * started or not which is used by the starting process to wait until the 
   * server is ready. 
   * 
   * @param pworker the python worker instance that owns the server
   */
  public void startServer(PythonWorker pworker){
    InetAddress hostAddress;
    try {
      hostAddress = InetAddress.getByName(host);
    } catch (UnknownHostException ex) {
      throw new RuntimeException("Cannot resolve host address "+host, ex);
    }
    Thread.currentThread().setContextClassLoader(Gate.getClassLoader());
    // Try to find the auth token from the environment variable
    if (this.authToken == null || this.authToken.trim().isEmpty()) {
      this.authToken = System.getenv("GATENLP_WORKER_TOKEN_"+port);
    }
    System.err.println("PythonWorkerRunner.java: starting server with "+port+"/"+host+"/"+authToken+"/"+logActions);
    pworker.logActions = logActions;
    pworker.keepRunning = keep;
    GatewayServer  server;
    if (this.authToken == null || this.authToken.trim().isEmpty()) {
      server = new GatewayServer.GatewayServerBuilder()
              .entryPoint(pworker)
              .javaPort(port)
              .javaAddress(hostAddress)
              .build();
    } else {
      //System.err.println("Using auth token: "+auth_token);
      server = new GatewayServer.GatewayServerBuilder()
              .entryPoint(pworker)
              .authToken(this.authToken)
              .javaPort(port)
              .javaAddress(hostAddress)
              .build();
    }
    pworker.server = server;
    try {
      server.start();
      System.err.println("PythonWorkerRunner.java: server start OK");
      System.err.flush();
    } catch(Exception ex) {
      pworker.server = null;
      System.err.println("PythonWorkerRunner.java: server start NOT OK");
      System.err.flush();
      throw new GateRuntimeException("Could not start GatewayServer",ex);
    }
  }
  
  @Override
  protected List<Action> buildActions(NameBearerHandle nbh) {
    return new ArrayList<>();
  }
  
  
  
  
}
