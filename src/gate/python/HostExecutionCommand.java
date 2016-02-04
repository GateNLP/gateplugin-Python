package gate.python;

import com.fasterxml.jackson.core.type.TypeReference;

import java.net.URL;
import java.util.List;

/**
 * Used by Python to tell GATE instance what to do.
 * Created by dominic on 04/02/2016.
 */
public class HostExecutionCommand {
    private HostExecutionCommandEnum command;
    private URL targetURL;
    private URL outputURL;
    private List<Command> documentCommands;

    public HostExecutionCommandEnum getCommand() {
        return command;
    }

    public void setCommand(HostExecutionCommandEnum command) {
        this.command = command;
    }

    public URL getTargetURL() {
        return targetURL;
    }

    public void setTargetURL(URL targetURL) {
        this.targetURL = targetURL;
    }

    public List<Command> getDocumentCommands() {
        return documentCommands;
    }

    public void setDocumentCommands(List<Command> documentCommands) {
        this.documentCommands = documentCommands;
    }

    public URL getOutputURL() {
        return outputURL;
    }

    public void setOutputURL(URL outputURL) {
        this.outputURL = outputURL;
    }
}

// {"command": "LOAD_DOCUMENT", "targetURL": "file:///Users/dominic/mention-nec/corpora/EFGH-split-locations-features/development//venue_mentions_enriched.json-85.xml"}