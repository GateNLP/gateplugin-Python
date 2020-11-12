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

import gate.Corpus;
import gate.CorpusController;
import gate.Document;
import gate.DocumentExporter;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.Resource;
import gate.corpora.DocumentStaxUtils;
import gate.creole.AbstractController;
import gate.creole.ExecutionException;
import gate.creole.Plugin;
import gate.creole.ResourceInstantiationException;
import gate.creole.ResourceReference;
import gate.gui.ResourceHelper;
import gate.gui.creole.manager.PluginUpdateManager;
import gate.persist.PersistenceException;
import gate.util.GateException;
import gate.util.GateRuntimeException;
import gate.util.persistence.PersistenceManager;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import gate.plugin.python.VersionLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py4j.GatewayServer;

/**
 * Run Java/GATE from python through this class.
 * 
 * @author Johann Petrak
 */
public class PythonSlave {

  /**
   * Logger
   */
  public transient final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

  protected GatewayServer server;

  /**
   * If we want to log all commands.
   */
  public boolean logActions = true;

  /**
   * Indicate if the slave should be kept running. This is the case if it was started by the PythonSlaveLr
   * or if the slave running does not clear this.
   */
  public boolean keepRunning = true;

  private Corpus tmpCorpus;
  
  /**
   * Create an instance.
   * @throws ResourceInstantiationException error
   */
  public PythonSlave() throws ResourceInstantiationException {   
    tmpCorpus = Factory.newCorpus("tmpCorpus");
  }
  
  /**
   * Load a maven plugin.
   * 
   * @param group maven group
   * @param artifact maven artifact
   * @param version maven version
   * @throws gate.util.GateException if error
   * 
   */
  public void loadMavenPlugin(
          String group, String artifact, String version) throws GateException {
    if (logActions) LOGGER.info("Slave run: load Maven plugin "+group+"/"+artifact+"/"+version);
    Gate.getCreoleRegister().registerPlugin(new Plugin.Maven(
            group, artifact, version));
  }
  
  /**
   * Load a pipeline from a file.
   * 
   * @param path gapp/xgapp file
   * @return the corpus controller
   */
  public CorpusController loadPipelineFromFile(String path) {
    if (logActions) LOGGER.info("Slave run: load controller from "+path);
    try {
      return (CorpusController)PersistenceManager.loadObjectFromFile(new File(path));
    } catch (PersistenceException | IOException | ResourceInstantiationException ex) {
      throw new GateRuntimeException("Could not load pipeline from "+path, ex);
    } 
  }
  
  /**
   * Find and return a loaded Maven plugin instance. 
   * 
   * @param group plugin group
   * @param artifact plugin artifact
   * @return the plugin instance or null of nothing found
   */
  public Plugin.Maven findMavenPlugin(String group, String artifact) {
    if (logActions) LOGGER.info("Slave run: find Maven plugin "+group+"/"+artifact);
    Set<Plugin> allPlugins = new LinkedHashSet<>(Gate.getCreoleRegister().getPlugins());
    allPlugins.addAll(PluginUpdateManager.getDefaultPlugins());
    for (Plugin plugin : allPlugins) {
      if (plugin instanceof Plugin.Maven) {
        Plugin.Maven mp = (Plugin.Maven)plugin;
        if (mp.getGroup().equals(group) && mp.getArtifact().equals(artifact)) {
          return mp;
        }
      }
    }
    return null;
  }
  
  
  /**
   * Load a pipeline from the maven plugin resources.
   * 
   * Example: "uk.ac.gate.plugins", "annie", "/resources/ANNIE_with_defaults.gapp"
   * 
   * @param group the plugin group
   * @param artifact the plugin artifact
   * @param path the path in the plugin resources
   * @return controller
   * @throws java.net.URISyntaxException  exception
   */
  public CorpusController loadPipelineFromPlugin(String group, String artifact, String path) throws URISyntaxException {
    if (logActions) LOGGER.info("Slave run: load pipeline "+path+" from plugin "+group+"/"+artifact);
    Plugin.Maven mp = findMavenPlugin(group, artifact);
    if(mp == null) {
      throw new GateRuntimeException("Could not find plugin, please load it first!");
    }
    ResourceReference rr;
    try {
      rr = new ResourceReference(mp, path);
    } catch (URISyntaxException ex) {
      throw new GateRuntimeException("Could not create ResourceReference for the pipeline");
    }
    if(rr == null) {
      throw new GateRuntimeException("Could not create ResourceReference for the pipeline");
    }
    try {
      return (CorpusController)PersistenceManager.loadObjectFromUri(rr.toURI());
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
  public Document loadDocumentFromFile(String path) {
    if (logActions) LOGGER.info("Slave run: load document from "+path);
    return loadDocumentFromFile(path, null);
  }
  
  /**
   * Create a new document from the text.
   * 
   * @param content the document content
   * @return the document
   */
  public Document createDocument(String content) {
    if (logActions) LOGGER.info("Slave run: createDocument from content");
    try {
      return Factory.newDocument(content);
    } catch (ResourceInstantiationException ex) {
      throw new GateRuntimeException("Could not create document", ex);
    }
  }
  
  /**
   * Create a new corpus.
   * 
   * @return  corpus
   */
  public Corpus newCorpus() {
    if (logActions) LOGGER.info("Slave run: create new corpus");
    try {
      return Factory.newCorpus("Corpus_"+Gate.genSym());
    } catch (ResourceInstantiationException ex) {
      throw new GateRuntimeException("Could not create document", ex);
    }    
  }
  
  /**
   * Delete a GATE resource and release memory.
   * 
   * @param res the resource to remove
   */
  public void deleteResource(Resource res) {
    if (logActions) LOGGER.info("Slave run: remove resource"+res.getName());
    Factory.deleteResource(res);
  }
  
  /**
   * Run a pipeline for a single document.
   * 
   * @param pipeline the pipeline to run
   * @param doc  the document to process
   */
  public void run4Document(CorpusController pipeline, Document doc) {
    if (logActions) LOGGER.info("Slave run: run controller "+pipeline.getName()+" for "+doc.getName());
    tmpCorpus.clear();
    tmpCorpus.add(doc);
    if(pipeline instanceof AbstractController) {
      ((AbstractController)pipeline).setControllerCallbacksEnabled(false);
    }
    pipeline.setCorpus(tmpCorpus);
    try {
      pipeline.execute();
    } catch (ExecutionException ex) {
      throw new GateRuntimeException("Exception when running the pipeline", ex);
    }
  }
  
  /**
   * Invoke the controller execution started code.
   * 
   * This should be run before documents are run individually using the run4doc
   * method.
   * 
   * @param pipeline pipeline
   */
  public void runExecutionStarted(CorpusController pipeline) {
    if (logActions) LOGGER.info("Slave run: run executionStarted for "+pipeline.getName());
    if(pipeline instanceof AbstractController) {
      try {
        ((AbstractController)pipeline).invokeControllerExecutionStarted();
      } catch (ExecutionException ex) {
        throw new GateRuntimeException("Problem running ExecutionStarted", ex);
      }
    }    
  }

  /**
   * Invoke the controller execution finished code.
   * 
   * This should be run after all documents are run individually using the run4doc
   * method.
   * 
   * @param pipeline pipeline
   */
  public void runExecutionFinished(CorpusController pipeline) {
    if (logActions) LOGGER.info("Slave run: run executionFinished for "+pipeline.getName());
    if(pipeline instanceof AbstractController) {
      try {
        ((AbstractController)pipeline).invokeControllerExecutionFinished();
      } catch (ExecutionException ex) {
        throw new GateRuntimeException("Problem running ExecutionFinished", ex);
      }
    }    
  }
  
  /**
   * Run the pipeline on the given corpus.
   * 
   * @param pipeline the pipeline
   * @param corpus  the corpus
   */
  public void run4Corpus(CorpusController pipeline, Corpus corpus) {
    if (logActions) LOGGER.info("Slave run: run pipline "+pipeline.getName()+" for corpus "+corpus.getName());
    if(pipeline instanceof AbstractController) {
      ((AbstractController)pipeline).setControllerCallbacksEnabled(true);
    }
    pipeline.setCorpus(corpus);
    try {
      pipeline.execute();
    } catch (ExecutionException ex) {
      throw new GateRuntimeException("Exception when running the pipeline", ex);
    }  
  }
  
  /**
   * Load document from the file, using mime type
   * @param path file
   * @param mimeType mime type
   * @return document
   */
  public Document loadDocumentFromFile(String path, String mimeType) {
    if (logActions) LOGGER.info("Slave run: load document from "+path+" mimetype:"+mimeType);
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
   * 
   * NOTE: currently there is no way in GATE to register a document format
   * for saving a document with a specific mime type.So this function currently
   * only recognizes a few hard-coded mime types and rejects all others.
   * 
   * The mime types are: "" (empty string) for the default GATE xml serialization;
   * all mime types supported by the Format_Bdoc plugin and all mime types 
   * supported by the Format_FastInfoset plugin.
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
  public void saveDocumentToFile(Document doc, String path, String mimetype)
          throws IOException, XMLStreamException {
    if (logActions) LOGGER.info("Slave run: save document to "+path+" mimetype:"+mimetype);
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
  public String getBdocJson(Document doc) {
    if (logActions) LOGGER.info("Slave run: get bdocjson for "+doc.getName());
    ResourceHelper rh = (ResourceHelper)Gate.getCreoleRegister()
                     .get("gate.plugin.format.bdoc.API")
                     .getInstantiations().iterator().next();
    try {
      String json = (String)rh.call("json_from_doc", doc);
      return json;
    } catch (NoSuchMethodException | IllegalArgumentException | 
            IllegalAccessException | InvocationTargetException ex) {
      throw new GateRuntimeException("Could not convert GATE document to json", ex);
    }
  }
  
  /**
   * Create a new GATE document from the Bdoc JSON serialization.
   * 
   * @param bdocjson the JSON 
   * @return a new GATE document built from the bdoc json
   * @throws gate.creole.ResourceInstantiationException should never occur
   */
  public Document getDocument4BdocJson(String bdocjson) 
          throws ResourceInstantiationException {
    if (logActions) LOGGER.info("Slave run: create document from bdocjson");
    Document theDoc = Factory.newDocument("");
    ResourceHelper rh = (ResourceHelper)Gate.getCreoleRegister()
                     .get("gate.plugin.format.bdoc.API")
                     .getInstantiations().iterator().next();
    try {
      rh.call("update_document_from_bdocjson", theDoc, bdocjson);
    } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException ex) {
      throw new GateRuntimeException("Could not invoke bdoc_from_string", ex);
    } 
    return theDoc;
  }

  /**
   * Copy string to standard output.
   * 
   * Note: no new line character is appended.
   * 
   * @param txt the string to copy
   */
  public void print2out(String txt) {
    System.out.print(txt);
  }

  /**
   * Copy string to standard error.
   * 
   * Note: no new line character is appended.
   * 
   * @param txt the string to copy
   */
  public void print2err(String txt) {
    System.err.print(txt);
  }
  
  /**
   * Activate the GUI.
   * 
   * Caution: experimental!
   * 
   */
  public void showGui() {
    if (logActions) LOGGER.info("Slave run: sho GUI");
    if (gate.gui.MainFrame.getInstance().isVisible()) {
      return;
    }
    Runnable r = new Runnable() {
      @Override
      public void run() {
        gate.Main.applyUserPreferences();
        gate.gui.MainFrame.getInstance().setVisible(true);
      }      
    };
    javax.swing.SwingUtilities.invokeLater(r);
  }
  
  /**
   * Return a list of all resources with the given name.
   * @param name resource name
   * @return list of matching resources
   * @throws GateException 
   */
  public List<Resource> getResources4Name(String name) throws GateException {
    if (logActions) LOGGER.info("Slave run: get resource for name "+name);
    return gate.Gate.getCreoleRegister().getAllInstances("gate.Resource");
  }
  
  /**
   * Return a list of all resources with the given name and class.
   * @param name resource name
   * @param clazz resource class name
   * @return list of matching resources
   * @throws GateException 
   */
  public List<Resource> getResources4Name(String name, String clazz) throws GateException {
    if (logActions) LOGGER.info("Slave run: get resource for name "+name+" and class "+clazz);
    return gate.Gate.getCreoleRegister().getAllInstances(clazz);
  }
  
  /**
   * Return the document with the given name.
   * 
   * If there are more than one, return an arbitrary one of those.
   * 
   * @param name the document name
   * @return document matching the name
   * @throws GateException 
   */
  public Document getDocument4Name(String name) throws GateException {
    if (logActions) LOGGER.info("Slave run: get document for name "+name);
    List<Resource> matching = getResources4Name(name, "gate.Document");
    if(matching.size() > 0) {
      return (Document)matching.get(0);
    } else {
      return null;
    }
  }

  /**
   * Return the corpus with the given name.
   * 
   * If there are more than one, return an arbitrary one of those.
   * 
   * @param name the corpus name
   * @return corpus matching the name
   * @throws GateException 
   */
  public Corpus getCorpus4Name(String name) throws GateException {
    if (logActions) LOGGER.info("Slave run: get corpus with name "+name);
    List<Resource> matching = getResources4Name(name, "gate.Corpus");
    if(matching.size() > 0) {
      return (Corpus)matching.get(0);
    } else {
      return null;
    }
  }

  /**
   * Return the pipeline with the given name.
   * 
   * If there are more than one, return an arbitrary one of those.
   * 
   * @param name the pipeline name
   * @return pipeline matching the name
   * @throws GateException 
   */
  public CorpusController getPipeline4Name(String name) throws GateException {
    if (logActions) LOGGER.info("Slave run: get pipeline with name "+name);
    List<Resource> matching = getResources4Name(name, "gate.CorpusController");
    if(matching.size() > 0) {
      return (CorpusController)matching.get(0);
    } else {
      return null;
    }
  }

  /**
   * Return the processing resource with the given name.
   * 
   * If there are more than one, return an arbitrary one of those.
   * 
   * @param name the pr name
   * @return pr matching the name
   * @throws GateException 
   */
  public ProcessingResource getPr4Name(String name) throws GateException {
    if (logActions) LOGGER.info("Slave run: get processing resource with name "+name);
    List<Resource> matching = getResources4Name(name, "gate.ProcessingResource");
    if(matching.size() > 0) {
      return (ProcessingResource)matching.get(0);
    } else {
      return null;
    }
  }
  
  /**
   * Return list of all known document names.
   * 
   * @return list of names
   * @throws GateException 
   */
  public List<String> getDocumentNames() throws GateException {
    if (logActions) LOGGER.info("Slave run: get known document names");
    List<Resource> tmp = gate.Gate.getCreoleRegister().getAllInstances("gate.Document");
    List<String> names = new ArrayList<>();
    for(Resource r : tmp) {
      names.add(r.getName());
    }
    return names;
  }

  /**
   * Return list of all known corpus names.
   * 
   * @return list of names
   * @throws GateException 
   */
  public List<String> getCorpusNames() throws GateException {
    if (logActions) LOGGER.info("Slave run: get known corpus names");
    List<Resource> tmp = gate.Gate.getCreoleRegister().getAllInstances("gate.Corpus");
    List<String> names = new ArrayList<>();
    for(Resource r : tmp) {
      names.add(r.getName());
    }
    return names;
  }
  
  /**
   * Return list of all known pipeline names.
   * 
   * @return list of names
   * @throws GateException 
   */
  public List<String> getPipelineNames() throws GateException {
    if (logActions) LOGGER.info("Slave run: get known pipeline names");
    List<Resource> tmp = gate.Gate.getCreoleRegister().getAllInstances("gate.CorpusController");
    List<String> names = new ArrayList<>();
    for(Resource r : tmp) {
      names.add(r.getName());
    }
    return names;
  }

  /**
   * Return list of all known processing resource names.
   * 
   * @return list of names
   * @throws GateException 
   */
  public List<String> getPrNames() throws GateException {
    if (logActions) LOGGER.info("Slave run: get known resource names");
    List<Resource> tmp = gate.Gate.getCreoleRegister().getAllInstances("gate.ProcessingResource");
    List<String> names = new ArrayList<>();
    for(Resource r : tmp) {
      names.add(r.getName());
    }
    return names;
  }

  /**
   * Enable or disable logging the actions.
   *
   * @param flag true or false
   */
  public void logActions(boolean flag) {
    if (logActions) LOGGER.info("Slave run: set logActions to "+flag);
    this.logActions = flag;
  }

  /**
   * Kill the slave.
   * This may cause data to get lost as the slave may not send something that is expected to the python master.
   *
   */
  public void kill() {
    server.shutdown();
  }

  /**
   * Check if the slave is closable.
   * It is closable if not started by the Lr.
   *
   * @return flag
   */
  public boolean isClosable() {
    return !keepRunning;
  }

  /**
   * Return version of the Python plugin.
   * 
   * @return  version
   */
  public String pluginVersion() {
    return VersionLogger.getPluginVersion();
  }
  
  /**
   * Return build (short commit id) of Python plugin. 
   * 
   * @return commit id
   */
  public String pluginBuild() {
    return VersionLogger.getPluginBuild();
  }
  
  /**
   * Return version of GATE.
   * 
   * @return  version
   */
  public String gate_version() {
    return gate.Main.version;
  }
  
  /**
   * Return build (short commit id) of GATE.
   * 
   * @return commit id
   */
  public String gate_build() {
    return gate.Main.build;
  }

}
