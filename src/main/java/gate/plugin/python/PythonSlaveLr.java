
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
