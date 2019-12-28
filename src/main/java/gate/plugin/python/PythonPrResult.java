/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.plugin.python;

import gate.creole.AbstractLanguageResource;
import gate.creole.metadata.CreoleResource;
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

  private static final long serialVersionUID = -7248763456343502768L;

  protected Map<String, Object> resultData;

    
  @Override
  public void setResultData(Map<String, Object> resultData) {
    this.resultData = resultData;
    if(resultData != null) {
      getFeatures().putAll(resultData);
    }
  }

  @Override
  public Map<String, Object> getResultData() {
    return resultData;
  }
  
}
