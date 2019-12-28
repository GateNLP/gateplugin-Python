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
import gate.creole.AbstractLanguageResource;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleResource;
import gate.persist.PersistenceException;
import java.util.Map;

/**
 * Represents the result of PythonPr processing.
 * This represents the result of processing a whole corpus with some
 * PythonPr. 
 * @author Johann Petrak
 */
@CreoleResource(name = "PythonPrResult",
        comment = "Language Resource to represent an over corpus result.",
        helpURL = "")
public class PythonPrResult 
        extends AbstractLanguageResource
        implements Result<Map<String,Object>> {


  protected Map<String, Object> resultData;

    
  @Override
  public void setResultData(Map<String, Object> resultData) {
    this.resultData = resultData;
    System.err.println("DEBUG Setting result: "+resultData);
    if(resultData != null) {
      getFeatures().putAll(resultData);
    }
  }

  @Override
  public Map<String, Object> getResultData() {
    return resultData;
  }
  
}
