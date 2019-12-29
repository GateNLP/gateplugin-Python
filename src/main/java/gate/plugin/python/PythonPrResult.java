package gate.plugin.python;

import gate.FeatureMap;
import gate.Resource;
import gate.creole.AbstractLanguageResource;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.util.GateRuntimeException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Represents the result of PythonPr processing.
 * 
 * This represents the result of processing a whole corpus with some
 * PythonPr. All data is stored in the feature map of the LR.
 * 
 * @author Johann Petrak
 */
@CreoleResource(name = "PythonPrResult",
        comment = "Language Resource to represent a PythonPr over corpus result.",
        helpURL = "")
public class PythonPrResult 
        extends AbstractLanguageResource {

  private static final long serialVersionUID = -7248763456343502768L;

  /**
   * Our logger instance.
   */
  public org.apache.log4j.Logger logger
          = org.apache.log4j.Logger.getLogger(this.getClass());
  
  
  /**
   * Set log features flag.
   * If true, features will get logged whenever updated.
   * @param value flag
   */
  @Optional
  @CreoleParameter(
          comment = "If true, log the current features whenever the result is updated."
  )  
  public void setLogFeatures(Boolean value) {
    logFeatures = value;
  }
  /**
   * Get the log features flag.
   * @return flag
   */
  public Boolean getLogFeatures() {
    return logFeatures;
  }
  protected Boolean logFeatures = false;
  
  
  @Optional
  @CreoleParameter(
          comment = "File to use for saving/loading the result. Nothing saved/loaded if missing."
  )  
  public void setFileUrl(URL value) {
    fileUrl = value;
  }
  public URL getFileUrl() {
    return fileUrl;
  }
  protected URL fileUrl;
  
  /**
   * Initialize resource.
   * @return 
   */
  @Override
  public Resource init() {
    loadFromFile();
    return this;
  }
  
  
  protected void saveToFile() {
    if(fileUrl != null) {
      File file = gate.util.Files.fileFromURL(fileUrl);
      try (
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file)) )
      {
        oos.writeObject(getFeatures());
      } catch (IOException ex) {
        throw new GateRuntimeException("Could not save PythonPrResult to file "+file.getAbsolutePath(), ex);
      }
    }
  }

  protected void loadFromFile() {
    if(fileUrl != null) {
      File file = gate.util.Files.fileFromURL(fileUrl);
      try (ObjectInputStream ois = new ObjectInputStream(fileUrl.openStream()) )
      {
        FeatureMap fm = (FeatureMap)ois.readObject();
        getFeatures().putAll(fm);
      } catch (IOException | ClassNotFoundException ex) {
        throw new GateRuntimeException("Could not load PythonPrResult from file "+file.getAbsolutePath(), ex);
      }
    }
  }
  
  /**
   * Log the current features to the logger.
   * @param level log level to use
   */
  public void outputFeaturesToLog(org.apache.log4j.Level level) {
    TreeSet<String> keys = new TreeSet<>();
    FeatureMap fm = getFeatures();
    for(Object k : fm.keySet()) {
      keys.add(k.toString());
    }
    for(String k : keys) {
      logger.log(level, k+": "+fm.get(k));
    }
  }
  
  
  /**
   * Set or update the result data.
   * 
   * Updates the resource with the passed result data. Existing features
   * are cleared first.
   * 
   * @param resultData the result data to set
   */
  public void setResultData(Map<String, Object> resultData) {
    if(resultData != null) {
      getFeatures().clear();
      getFeatures().putAll(resultData);
      saveToFile();
      if(getLogFeatures() != null && getLogFeatures()) {
        outputFeaturesToLog(org.apache.log4j.Level.INFO);
      }
    }
  }

  /**
   * Get the current result data stored in the features.
   * 
   * @return  current result data
   */
  public Map<String, Object> getResultData() {
    Map<String,Object> ret = new HashMap<>();
    FeatureMap fm = getFeatures();
    for(Object k : fm.keySet()) {
      ret.put(k.toString(), fm.get(k));
    }
    return ret;
  }
  
}
