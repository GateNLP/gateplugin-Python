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

import gate.CorpusController;
import gate.Document;
import gate.DocumentExporter;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.corpora.DocumentStaxUtils;
import gate.creole.Plugin;
import gate.creole.ResourceInstantiationException;
import gate.gui.ResourceHelper;
import gate.lib.basicdocument.BdocDocument;
import gate.lib.basicdocument.BdocDocumentBuilder;
import gate.lib.basicdocument.docformats.SimpleJson;
import gate.persist.PersistenceException;
import gate.util.GateException;
import gate.util.GateRuntimeException;
import gate.util.persistence.PersistenceManager;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import javax.xml.stream.XMLStreamException;
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
  public static final org.apache.log4j.Logger LOGGER
          = org.apache.log4j.Logger.getLogger(PythonSlave.class);
  
  
  
  /**
   * Load a maven plugin.
   * 
   * @param group maven group
   * @param artifact maven artifact
   * @param version maven version
   * @throws gate.util.GateException if error
   * 
   */
  public static void loadMavenPlugin(
          String group, String artifact, String version) throws GateException {
    Gate.getCreoleRegister().registerPlugin(new Plugin.Maven(
            group, artifact, version));
  }
  
  /**
   * Load a pipeline from a file.
   * 
   * @param path gapp/xgapp file
   * @return the corpus controller
   */
  public static CorpusController loadPipeline(String path) {
    LOGGER.info("Loading pipeline (CorpusController) from "+path);
    try {
      return (CorpusController)PersistenceManager.loadObjectFromFile(new File(path));
    } catch (PersistenceException | IOException | ResourceInstantiationException ex) {
      throw new GateRuntimeException("Could not load pipeline from "+path, ex);
    } 
  }
  
  /**
   * Load document from the file.
   * 
   * This will load the document in the same way as if only the document 
   * URL had been specified in the GUI, if a document format is registered
   * for the extension, it is used. 
   * 
   * @param path file path of the document to load
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
    LOGGER.info("Loading document from "+path);
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
   * Save document to a file.NOTE: currently there is no way in GATE to register a document format
 for saving a document with a specific mime type.So this function currently
 only recognizes a few hard-coded mime types and rejects all others.
   * 
   * The mime types are: "" (empty string) for the default GATE xml serialization;
 all mime types supported by the Format_Bdoc plugin and all mime types 
 supported by the Format_FastInfoset plugin.
   * 
   * NOTE: for fastinfoset the plugin must first have been loaded with 
   * loadMavenPlugin("uk.ac.gate.plugins","format-fastinfoset","8.5") or 
   * whatever the wanted version is.
   * 
   * @param path file
   * @param mimetype  mime type
   * @throws java.io.IOException if something goes wrong saving
   * @throws javax.xml.stream.XMLStreamException if something goes wrong when saving
   */
  public static void saveDocument(Document doc, String path, String mimetype)
          throws IOException, XMLStreamException {
    if(mimetype==null || mimetype.isEmpty()) {
      DocumentStaxUtils.writeDocument(doc, new File(path));
    } else if("application/fastinfoset".equals(mimetype)) {
      DocumentExporter docExporter = (DocumentExporter)Gate.getCreoleRegister()
                     .get("gate.corpora.FastInfosetExporter")
                     .getInstantiations().iterator().next();
      docExporter.export(doc, new File(path), Factory.newFeatureMap());
    } else if("text/bdocsjson".equals(mimetype)) {
      DocumentExporter docExporter = (DocumentExporter)Gate.getCreoleRegister()
                     .get("gate.plugin.format.bdoc.FormatBdocSimpleJson")
                     .getInstantiations().iterator().next();
      docExporter.export(doc, new File(path), Factory.newFeatureMap());
    } else if("text/bdocsjson".equals(mimetype) || "text/bdocsjs".equals(mimetype)) {
      DocumentExporter docExporter = (DocumentExporter)Gate.getCreoleRegister()
                     .get("gate.plugin.format.bdoc.FormatBdocSimpleJson")
                     .getInstantiations().iterator().next();
      docExporter.export(doc, new File(path), Factory.newFeatureMap());
    } else if("text/bdocjson".equals(mimetype) || "text/bdocjs".equals(mimetype)) {
      DocumentExporter docExporter = (DocumentExporter)Gate.getCreoleRegister()
                     .get("gate.plugin.format.bdoc.FormatBdocJson")
                     .getInstantiations().iterator().next();
      docExporter.export(doc, new File(path), Factory.newFeatureMap());
    } else if("text/bdocsjson+gzip".equals(mimetype) || "text/bdocsjs+gzip".equals(mimetype)) {
      DocumentExporter docExporter = (DocumentExporter)Gate.getCreoleRegister()
                     .get("gate.plugin.format.bdoc.FormatBdocSimpleJsonGzip")
                     .getInstantiations().iterator().next();
      docExporter.export(doc, new File(path), Factory.newFeatureMap());
    } else if("text/bdocjson+gzip".equals(mimetype) || "text/bdocjs+gzip".equals(mimetype)) {
      DocumentExporter docExporter = (DocumentExporter)Gate.getCreoleRegister()
                     .get("gate.plugin.format.bdoc.FormatBdocJsonGzip")
                     .getInstantiations().iterator().next();
      docExporter.export(doc, new File(path), Factory.newFeatureMap());
    } else if("application/bdocmp".equals(mimetype)) {
      DocumentExporter docExporter = (DocumentExporter)Gate.getCreoleRegister()
                     .get("gate.plugin.format.bdoc.BdocMsgPack")
                     .getInstantiations().iterator().next();
      docExporter.export(doc, new File(path), Factory.newFeatureMap());
    }    
  }
  
  /**
   * Get the JSON serialization of the Bdoc representation of a document.
   * @param doc document
   * @return json
   */
  public static String getBdocJson(Document doc) {
    BdocDocument bdoc = new BdocDocumentBuilder().fromGate(doc).buildBdoc();
    return new SimpleJson().dumps(bdoc);
  }
  
  /**
   * Create a new GATE document from the Bdoc JSON serialization.
   * 
   * @param bdocjson the JSON 
   * @return a new GATE document built from the bdoc json
   * @throws gate.creole.ResourceInstantiationException should never occur
   */
  public static Document getDocument4BdocJson(String bdocjson) 
          throws ResourceInstantiationException {
    Document theDoc = Factory.newDocument("");
    ResourceHelper rh = (ResourceHelper)Gate.getCreoleRegister()
                     .get("gate.plugin.format.bdoc.API")
                     .getInstantiations().iterator().next();
    try {
      BdocDocument bdoc = (BdocDocument)rh.call("bdoc_from_string", null, bdocjson);
      rh.call("update_document", theDoc, bdoc);
    } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException ex) {
      throw new GateRuntimeException("Could not invoke bdoc_from_string", ex);
    } 
    return theDoc;
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
