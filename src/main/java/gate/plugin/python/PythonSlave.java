
package gate.plugin.python;

import gate.CorpusController;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.lib.basicdocument.BdocDocument;
import gate.lib.basicdocument.BdocDocumentBuilder;
import gate.lib.basicdocument.docformats.SimpleJson;
import gate.persist.PersistenceException;
import gate.util.GateRuntimeException;
import gate.util.persistence.PersistenceManager;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import py4j.GatewayServer;

/**
 * Run Java/GATE from python through this class.
 * 
 * @author Johann Petrak
 */
public class PythonSlave {
  protected int port;
  protected GatewayServer server;
  
  /**
   * Our logger instance.
   */
  public static org.apache.log4j.Logger logger
          = org.apache.log4j.Logger.getLogger(PythonSlave.class);
  
  
  public PythonSlave() {
    port = 25333;
  }
  public PythonSlave(int port) {
    this.port = port;
  }
  
  public static CorpusController loadPipeline(String path) {
    logger.info("Loading pipeline (CorpusController) from "+path);
    try {
      return (CorpusController)PersistenceManager.loadObjectFromFile(new File(path));
    } catch (PersistenceException | IOException | ResourceInstantiationException ex) {
      throw new GateRuntimeException("Could not load pipeline from "+path, ex);
    } 
  }
  
  public static Document loadDocument(String path) {
    return loadDocument(path, null);
  }
  
  public static Document loadDocument(String path, String mimeType) {
    logger.info("Loading document from "+path);
    FeatureMap params = Factory.newFeatureMap();
    try {
      params.put("sourceUrl", new File(path).toURI().toURL());      
      if(mimeType != null) {
        params.put("mimeType", mimeType);
      }
      params.put("encoding", "utf-8");
      Document doc = (Document)Factory.createResource("gate.corpora.DocumentImpl", params);
      return doc;
    } catch (ResourceInstantiationException | MalformedURLException ex) {
      throw new GateRuntimeException("Could not load document from "+path, ex);
    }
  }
  
  public static void saveDocument(String path, String mimetype) {
    logger.info("[NOT IMPLEMENTED YET!!!] Saving document from "+path);    
  }
  
  public static String getBdocDocumentJson(Document doc) {
    BdocDocument bdoc = new BdocDocumentBuilder().fromGate(doc).buildBdoc();
    return new SimpleJson().dumps(bdoc);
  }
  
  
  public void startServer() {
    Thread.currentThread().setContextClassLoader(Gate.getClassLoader());
    server = new GatewayServer(this, port);
    server.start();
  }
  public void stopServer() {
    server.shutdown();
  }
  
  
}
