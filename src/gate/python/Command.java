package gate.python;

import java.util.Map;

public class Command
{
  private CommandEnum command;
  private String annotationSet;
  private long startOffset;
  private long endOffset;
  private String annotationName;
  private Integer annotationID;
  private Map<String, Object> featureMap;
  private String featureName;
  private Object featureValue;

  public Command() { }

  public CommandEnum getCommand() {
    return command;
  }

  public void setCommand(CommandEnum command) {
    this.command = command;
  }

  public String getAnnotationSet() {
    return annotationSet;
  }

  public void setAnnotationSet(String annotationSet) {
    this.annotationSet = annotationSet;
  }

  public long getStartOffset() {
    return startOffset;
  }

  public void setStartOffset(long startOffset) {
    this.startOffset = startOffset;
  }

  public long getEndOffset() {
    return endOffset;
  }

  public void setEndOffset(long endOffset) {
    this.endOffset = endOffset;
  }

  public String getAnnotationName() {
    return annotationName;
  }

  public void setAnnotationName(String annotationName) {
    this.annotationName = annotationName;
  }

  public Map<String, Object> getFeatureMap() {
    return featureMap;
  }

  public void setFeatureMap(Map<String, Object> featureMap) {
    this.featureMap = featureMap;
  }

  public String getFeatureName() {
    return featureName;
  }

  public void setFeatureName(String featureName) {
    this.featureName = featureName;
  }

  public Object getFeatureValue() {
    return featureValue;
  }

  public void setFeatureValue(Object featureValue) {
    this.featureValue = featureValue;
  }

  public Integer getAnnotationID() {
    return annotationID;
  }

  public void setAnnotationID(Integer annotationID) {
    this.annotationID = annotationID;
  }
}