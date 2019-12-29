
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


  /**
   * Port number to use.
   */
  public int port;

  protected GatewayServer server;
  
  /**
   * Our logger instance.
   */
  public static org.apache.log4j.Logger logger
          = org.apache.log4j.Logger.getLogger(PythonSlave.class);
  
  
  /**
   * Load a pipeline from a file.
   * @param path gapp/xgapp file
   * @return the corpus controller
   */
  public static CorpusController loadPipeline(String path) {
    logger.info("Loading pipeline (CorpusController) from "+path);
    try {
      return (CorpusController)PersistenceManager.loadObjectFromFile(new File(path));
    } catch (PersistenceException | IOException | ResourceInstantiationException ex) {
      throw new GateRuntimeException("Could not load pipeline from "+path, ex);
    } 
  }
  
  /**
   * Load document from the file.
   * @param path file
   * @return document
   */
  public static Document loadDocument(String path) {
    return loadDocument(path, null);
  }
  
  /**
   * Load document from the file, using mime type
   * @param path file
   * @param mimeType mime type
   * @return document
   */
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
  
  /**
   * Save document to a file.
   * @param path file
   * @param mimetype  mime type
   */
  public static void saveDocument(String path, String mimetype) {
    logger.info("[NOT IMPLEMENTED YET!!!] Saving document from "+path);    
  }
  
  /**
   * Get the JSON serialization of the Bdoc representation of a document.
   * @param doc document
   * @return json
   */
  public static String getBdocDocumentJson(Document doc) {
    BdocDocument bdoc = new BdocDocumentBuilder().fromGate(doc).buildBdoc();
    return new SimpleJson().dumps(bdoc);
  }
  

  /**
   * Start the server.
   * Uses whatever has been set before for port number.
   * 
   */
  public void startServer() {
    Thread.currentThread().setContextClassLoader(Gate.getClassLoader());
    server = new GatewayServer(this, port);
    server.start();
  }
  
  /**
   * Stop the server.
   */
  public void stopServer() {
    server.shutdown();
  }
  
  
}
