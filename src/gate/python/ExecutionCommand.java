package gate.python;

import gate.FeatureMap;

/**
 * Used to signal via JSON that we are at the start or the end of a corpus
 *
 * Created by dominic on 20/01/2016.
 */
public class ExecutionCommand {
    private ExecutionCommandEnum command;
    private String corpusName;
    private FeatureMap corpusFeatures;

    private FeatureMap parameterMap;

    public ExecutionCommandEnum getCommand() {
        return command;
    }

    public void setCommand(ExecutionCommandEnum command) {
        this.command = command;
    }


    public String getCorpusName() {
        return corpusName;
    }

    public void setCorpusName(String corpusName) {
        this.corpusName = corpusName;
    }

    public FeatureMap getCorpusFeatures() {
        return corpusFeatures;
    }

    public void setCorpusFeatures(FeatureMap corpusFeatures) {
        this.corpusFeatures = corpusFeatures;
    }

    public FeatureMap getParameterMap() {
        return parameterMap;
    }

    public void setParameterMap(FeatureMap parameterMap) {
        this.parameterMap = parameterMap;
    }

}
