package gate.python;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gate.Annotation;
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.corpora.DocumentJsonUtils;
import gate.util.GateException;

import java.io.*;
import java.util.*;

/**
 * Starts an application process containing a GATE instance, allowing one to write scripts in Python that open and
 * modify GATE documents, which are saved directly using GATE.
 *
 * Created by dominic on 04/02/2016.
 */
public class PythonGATEInstance {

    public static void main(String[] args) {


        try {
            Gate.init();

            JsonFactory factory = new JsonFactory();
            factory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
            ObjectMapper objectMapper = new ObjectMapper(factory);
            JsonGenerator jsonG = factory.createGenerator(System.out);

            Scanner scanner = new Scanner(System.in);
            while(scanner.hasNext()){
                String inputLine = scanner.nextLine();
                HostExecutionCommand command = objectMapper.readValue(inputLine, new TypeReference<HostExecutionCommand>() {});

                if (command.getCommand().equals(HostExecutionCommandEnum.LOAD_DOCUMENT)) {
                    Document document = Factory.newDocument(command.getTargetURL());
                    PythonPR.exportDocument(document, new HashMap<String, Object>(), jsonG);
                    jsonG.writeRaw("\n");
                    Factory.deleteResource(document);
                } else if (command.getCommand().equals(HostExecutionCommandEnum.SAVE_DOCUMENT)) {
                    Document document = Factory.newDocument(command.getTargetURL());
                    PythonPR.transformDocument(document, command.getDocumentCommands());
                    File outputFile = new File(command.getOutputURL().getFile());
                    // Write output files using the same encoding as the original
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    OutputStreamWriter out;
                    out = new OutputStreamWriter(bos);

                    out.write(document.toXml());

                    out.close();

                    PythonPR.exportDocument(document, new HashMap<String, Object>(), jsonG);
                    jsonG.writeRaw("\n");
                    Factory.deleteResource(document);
                }
            }
        } catch (GateException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
