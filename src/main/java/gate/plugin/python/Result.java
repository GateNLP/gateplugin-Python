/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.plugin.python;

import gate.LanguageResource;

/**
 * Interface for processing results.
 * This is probably mostly something that represents a result of processing
 * a whole corpus of documents. The idea is that a result is a (language)resource and 
 * thus can get passed on via parameters and can get visualized using a VR.
 * We can also register actions in the menu of the resource. 
 * @author johann
 */
public interface Result<T extends Object> extends LanguageResource {
  /**
   * Set the result data.
   * @param resultData  some that the Result resource will represent
   */
  public void setResultData(T resultData);
  /**
   * Get the result data.
   * @return the result data currently stored in the resource.
   */
  public T getResultData();
}
