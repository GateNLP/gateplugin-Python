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
import gate.creole.AbstractLanguageResource;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.util.GateRuntimeException;
import py4j.GatewayServer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

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

  /**
   * Set binding host name or address
   *
   * @param value host
   */
  @Optional
  @CreoleParameter(
          comment = "Binding host name or address",
          defaultValue = "127.0.0.1"
  )
  public void setHost(String value) {
    host = value;
  }
  /**
   * Get the host
   *
   * @return host
   */
  public String getHost() {
    return host;
  }
  protected String host = "127.0.0.1";

  /**
   * Set auth token
   *
   * @param value auth token
   */
  @Optional
  @CreoleParameter(
          comment = "Auth token to use",
          defaultValue = ""
  )
  public void setAuthToken(String value) {
    authToken = value;
  }
  /**
   * Get the auth token.
   *
   * @return auth token
   */
  public String getAuthToken() {
    return authToken;
  }
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



  /**
   * Set if we want to use an auth token.
   *
   * @param value flag
   */
  @CreoleParameter(comment = "Whether to use an auth token", defaultValue="true")
  public void setUseAuthToken(Boolean value) { useAuthToken = value; }

  /**
   * Should we use an auth token.
   *
   * @return flag
   */
  public Boolean getUseAuthToken() { return useAuthToken; }
  protected Boolean useAuthToken = true;


  protected transient PythonSlave pythonSlave;
  
  @Override
  public Resource init() throws ResourceInstantiationException {
    logger.info("Creating PythonSlave instance");
    try {
      pythonSlave = new PythonSlave();
      pythonSlave.logActions = logActions;
    } catch (ResourceInstantiationException ex) {
      throw new ResourceInstantiationException("Could not create PythonSlave", ex);
    }
    startServer(pythonSlave);
    logger.info("Python slave started at port "+port);
    return this;
  }
  
  @Override
  public void cleanup() {
    // logger.info("Trying to stop server");
    stopServer(pythonSlave);
    // logger.info("After stopping server");
    super.cleanup();
  }
  
  /**
   * Start the server.
   * 
   * @param pslave the python slave instance that owns the server
   */
  public void startServer(PythonSlave pslave) {
    InetAddress hostAddress;
    try {
      hostAddress = InetAddress.getByName(host);
    } catch (UnknownHostException ex) {
      throw new RuntimeException("Cannot resolve host address "+host, ex);
    }
    Thread.currentThread().setContextClassLoader(Gate.getClassLoader());
    GatewayServer server;
    if (useAuthToken) {
      if (authToken == null || authToken.trim().isEmpty()) {
        // generate a new random auth token and write it out
        authToken = UUID.randomUUID().toString();
        System.out.println("Using auth token: "+authToken);
      }
      server = new GatewayServer.GatewayServerBuilder()
              .entryPoint(pslave)
              .javaPort(port)
              .javaAddress(hostAddress)
              .authToken(this.authToken)
              .build();
    } else {
      server = new GatewayServer.GatewayServerBuilder()
              .entryPoint(pslave)
              .javaPort(port)
              .javaAddress(hostAddress)
              .build();
    }
    pslave.server = server;
    try {
      server.start();
    } catch(Exception ex) {
      pslave.server = null;
      throw new GateRuntimeException("Could not start GatewayServer",ex);
    }
  }
  
  /**
   * Stop the server.
   * 
   * @param pslave the PythonSlave instance that owns the server.
   */
  public void stopServer(PythonSlave pslave) {
    if(pslave.server != null)
      pslave.server.shutdown();
  }
  
  
  
  
}
