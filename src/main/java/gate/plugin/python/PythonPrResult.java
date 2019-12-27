/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.plugin.python;

import gate.DataStore;
import gate.Factory;
import gate.FeatureMap;
import gate.LanguageResource;
import gate.Resource;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import java.util.Map;

/**
 * Represents the result of PythonPr processing.
 * This represents the result of processing a whole corpus with some
 * PythonPr. 
 * @author Johann Petrak
 */
public class PythonPrResult implements Result<Map<String,Object>> {

  @Override
  public DataStore getDataStore() {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public void setDataStore(DataStore ds) throws PersistenceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public Object getLRPersistenceId() {
    throw new UnsupportedOperationException("Not supported."); 
  }

  @Override
  public void setLRPersistenceId(Object o) {
    throw new UnsupportedOperationException("Not supported."); 
  }

  @Override
  public void sync() throws PersistenceException, SecurityException {
    // do nothing
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public LanguageResource getParent() throws PersistenceException, SecurityException {
    throw new UnsupportedOperationException("Not supported."); 
  }

  @Override
  public void setParent(LanguageResource lr) throws PersistenceException, SecurityException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Resource init() throws ResourceInstantiationException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void cleanup() {
    
  }

  @Override
  public Object getParameterValue(String string) throws ResourceInstantiationException {
    return null;
  }

  @Override
  public void setParameterValue(String string, Object o) throws ResourceInstantiationException {
    
  }

  @Override
  public void setParameterValues(FeatureMap fm) throws ResourceInstantiationException {
    
  }

  @Override
  public FeatureMap getFeatures() {
    return features;
  }

  @Override
  public void setFeatures(FeatureMap fm) {
    features = fm;
  }

  @Override
  public void setName(String string) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public String getName() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  protected Map<String, Object> resultData;
  protected FeatureMap features = Factory.newFeatureMap();

    
  @Override
  public void setResultData(Map<String, Object> resultData) {
    this.resultData = resultData;
  }

  @Override
  public Map<String, Object> getResultData() {
    return resultData;
  }
  
}
