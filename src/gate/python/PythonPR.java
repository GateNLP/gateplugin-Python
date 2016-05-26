package gate.python;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ControllerAwarePR;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.Files;


import gate.util.InvalidOffsetException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import com.fasterxml.jackson.core.JsonGenerationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;

@CreoleResource(name = "Python PR", comment = "Runs a Python script as a processing resource")
public class PythonPR extends AbstractLanguageAnalyser implements ControllerAwarePR {

	private static final Logger log = Logger.getLogger(PythonPR.class);
	
	private String pythonBinary;
	
	private String lastPythonBinary;

	private Boolean forceRestart;

	private URL script;
	
	private URL lastScript;

	private String inputAS;
	
	private String outputAS;
	
	private FeatureMap scriptParams;
	

	public String getPythonBinary() {
		return pythonBinary;
	}

	@RunTime
	@CreoleParameter(defaultValue = "python", comment = "Python command to use")
	public void setPythonBinary(String pythonBinary) {
		this.pythonBinary = pythonBinary;
	}

	public URL getScript() {
		return script;
	}

	@RunTime
	@CreoleParameter(defaultValue = "", comment = "Path to the Python script you want to run")
	public void setScript(URL script) {
		this.script = script;
	}

	@RunTime
	@Optional
	@CreoleParameter(comment = "Input annotation set to use")
	public void setInputAS(String inputAS) {
		this.inputAS = inputAS;
	}

	public String getInputAS() {
		return inputAS;
	}

	@RunTime
	@Optional
	@CreoleParameter(comment = "Output annotation set to use")
	public void setOutputAS(String outputAS) {
		this.outputAS = outputAS;
	}


	public Boolean getForceRestart() {
		return forceRestart;
	}

	@RunTime
	@CreoleParameter(defaultValue="false", comment = "Force a restart on the next document. Will toggle when executed.")
	public void setForceRestart(Boolean forceRestart) {
		this.forceRestart = forceRestart;
	}

	public FeatureMap getScriptParams() {
		return scriptParams;
	}

	@RunTime
	@Optional
	@CreoleParameter(comment = "Extra parameters to pass to the string")
	public void setScriptParams(FeatureMap scriptParams) {
		this.scriptParams = scriptParams;
	}

	public String getOutputAS() {
		return outputAS;
	}

	private Process pythonProcess;
	
	private PrintWriter pythonInput;

	private JsonGenerator pythonJsonG;

	private ObjectMapper pythonObjectMapper;

	private BufferedReader pythonOutput;
		
	public void execute() throws ExecutionException {
		ensureProcess();
		
		// Python is definitely running.
		HashMap<String, Object> extraFeatures = new HashMap<>();
		extraFeatures.put("inputAS", inputAS);
		extraFeatures.put("outputAS", outputAS);
		extraFeatures.put("scriptParams", scriptParams);

		try {
			exportDocument(getDocument(), extraFeatures, pythonJsonG);
		} catch (ExecutionException e) {
			log.error(e);
			cleanupProcess();
		}


		try {
			// Parse the reply, which should be a list of commands.
			List<Command> commands = (List<Command>) pythonObjectMapper.readValue(pythonOutput,
					new TypeReference<List<Command>>() {} );
			transformDocument(getDocument(), commands);
		} catch (IOException e) {
			log.error("Unable to read JSON from python process, closing pipeline", e);

			cleanupProcess();
			throw new ExecutionException("Unable to read JSON from python process", e);
		} catch (InvalidOffsetException e) {
			cleanupProcess();
			throw new ExecutionException("Unable to apply changes requested by python script", e);
		}
	}

	/**
	 * Applies the supplied list of document change commands to the given document, and returns it.
	 *
	 * @param document
	 * @param commands
	 * @return
	 * @throws InvalidOffsetException
	 */
	public static Document transformDocument(Document document, List<Command> commands) throws InvalidOffsetException {
		AnnotationSet targetAnnotationSet;
		Annotation targetAnnotation;
		for (Command command : commands) {
            switch (command.getCommand()) {
                case ADD_ANNOT:
                    document.getAnnotations(command.getAnnotationSet()).
							add(command.getStartOffset(), command.getEndOffset(),
									command.getAnnotationName(), Utils.toFeatureMap(command.getFeatureMap()));
                    break;
                case REMOVE_ANNOT:
                    targetAnnotationSet = document.getAnnotations(command.getAnnotationSet());
                    targetAnnotation = targetAnnotationSet.get(command.getAnnotationID());
                    targetAnnotationSet.remove(targetAnnotation);
                    break;
                case UPDATE_FEATURE:
                    targetAnnotationSet = document.getAnnotations(command.getAnnotationSet());
                    targetAnnotation = targetAnnotationSet.get(command.getAnnotationID());
                    targetAnnotation.getFeatures().put(command.getFeatureName(), command.getFeatureValue());
                    break;
                case CLEAR_FEATURES:
                    targetAnnotationSet = document.getAnnotations(command.getAnnotationSet());
                    targetAnnotation = targetAnnotationSet.get(command.getAnnotationID());
                    targetAnnotation.getFeatures().clear();
                    break;
                case REMOVE_FEATURE:
                    targetAnnotationSet = document.getAnnotations(command.getAnnotationSet());
                    targetAnnotation = targetAnnotationSet.get(command.getAnnotationID());
                    targetAnnotation.getFeatures().remove(command.getFeatureName());
                    break;

            }
        }
		return document;
	}

	public static void exportDocument(Document document, HashMap<String, Object> extraFeatures, JsonGenerator jsonG) throws ExecutionException {
		extraFeatures.put("documentFeatures", document.getFeatures());

		Map<String, Collection<Annotation>> allAnnotations = new TreeMap<String, Collection<Annotation>>();

		Set<String> annotationSetNames = new TreeSet<String>();
		annotationSetNames.add(""); // Include the default annotation set.
		annotationSetNames.addAll(document.getAnnotationSetNames());

		// Collect all annotations together in a format we can output directly to JSON
		for (String annotationSetName : annotationSetNames) {
			AnnotationSet annotationSet = document.getAnnotations(annotationSetName);

			for (Annotation annotation : annotationSet) {
				String type = annotation.getType();
				String typeKey = (annotationSetName == null ? "" : annotationSetName) + ":" + type;

				if (!allAnnotations.containsKey(typeKey)) {
					allAnnotations.put(typeKey, new ArrayList<Annotation>());
				}

				allAnnotations.get(typeKey).add(annotation);

			}
		}

		// Output the document in JSON format on the pipe
		try {

			PythonJsonUtils.writeDocument(document, 0l, document.getContent().size(), allAnnotations,
					extraFeatures, null, "annotationID", jsonG);

			jsonG.writeRaw("\n");
			jsonG.flush();
		} catch (InvalidOffsetException e) {
			log.error("Impossible document offset exception", e);
			throw new ExecutionException("Unable to write entire document span", e);
		} catch (JsonGenerationException e) {
			log.error("Unable to generate JSON for document", e);
		} catch (IOException e) {
			log.error("Unable to flush JSON to python process, closing pipeline", e);
			throw new ExecutionException("Unable to flush JSON to python process", e);
		}
	}

	private void ensureProcess() throws ExecutionException {
		boolean needNewProcess = forceRestart || (!processRunning() ||
					!ObjectUtils.equals(pythonBinary, lastPythonBinary) ||
					!ObjectUtils.equals(script, lastScript));

		// Close the existing long-running process before opening a new one, if needed
		if(pythonProcess != null && needNewProcess) {
			IOUtils.closeQuietly(pythonInput);
			try {
				pythonProcess.waitFor();
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ExecutionException("Interrupted while waiting for python to exit");
			}
		}
		// Start the fresh process as needed.
		if(needNewProcess) {
			// Runs python -u some_script.py  (unbuffered)
			ProcessBuilder builder = new ProcessBuilder(pythonBinary, "-u", Files.fileFromURL(script).getAbsolutePath());
			try {
				// Todo: there must be a better way to achieve this
				URL pythonPath = new URL(Gate.getCreoleRegister().get(this.getClass().getName()).getXmlFileUrl(), ".");
				String oldPythonPath = System.getenv("PYTHONPATH");
				oldPythonPath = oldPythonPath != null ? oldPythonPath : ".";
				builder.environment().put("PYTHONPATH", oldPythonPath + File.pathSeparator +
						Files.fileFromURL(pythonPath).getAbsolutePath());
				builder.directory(Files.fileFromURL(script).getAbsoluteFile().getParentFile());
			} catch (MalformedURLException e) {
				throw new ExecutionException("Couldn't form python path for running PR", e);
			}

			lastPythonBinary = pythonBinary;
			lastScript = script;

			try {
				pythonProcess = builder.start();

				setForceRestart(false);
				report(pythonProcess.getErrorStream());
				pythonInput = new PrintWriter(new OutputStreamWriter(pythonProcess.getOutputStream(), "UTF-8"));
				pythonOutput = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream(), "UTF-8"));

				JsonFactory factory = new JsonFactory(); // Todo: maybe move this into Init if it causes problems

				pythonJsonG = factory.createGenerator(pythonInput);
				factory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
				pythonObjectMapper = new ObjectMapper(factory);
			} catch(IOException e) {
				throw new ExecutionException("Could not start python", e);
			}

		}
	}

	protected boolean processRunning() {
		if(pythonProcess == null) return false;
		
		try {
			pythonProcess.exitValue();
			return false;
		}
		catch (IllegalThreadStateException e) {
			return true;
		}
	}
	
	protected void report(final InputStream stream) {
		Thread t = new Thread() {
			public void run() {
				try {
					IOUtils.copy(stream, System.err);
				} catch(IOException e) {
					log.warn("Error printing stderr output from python", e);
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}
	
	protected void cleanupProcess() {
		if(pythonProcess != null) {
			IOUtils.closeQuietly(pythonInput);
			try {
				IOUtils.copy(pythonOutput, (true ? System.err : NullOutputStream.NULL_OUTPUT_STREAM));
			} catch(IOException e1) {
				log.warn("Exception draining python output stream");
			}
			try {
				pythonProcess.destroy();
				pythonProcess.waitFor();
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
				log.warn("Interrupted while waiting for python to exit");
			}      
		}
	}
	
	public void cleanup() {
		super.cleanup();
		cleanupProcess();
	}

	@Override
	public void controllerExecutionStarted(Controller controller) throws ExecutionException {
		ensureProcess();
		ExecutionCommand command = new ExecutionCommand();
		command.setCommand(ExecutionCommandEnum.BEGIN_EXECUTION);

		if (corpus != null) {
			command.setCorpusName(corpus.getName());
			command.setCorpusFeatures(corpus.getFeatures());
		}

		try {

			pythonObjectMapper.writeValue(pythonJsonG, command);
			pythonJsonG.writeRaw("\n");
			pythonJsonG.flush();

		} catch (IOException e) {
			throw new ExecutionException("Unable to send begin execution command to python process");
		}
	}

	@Override
	public void controllerExecutionFinished(Controller controller) throws ExecutionException {
		ensureProcess();
		ExecutionCommand command = new ExecutionCommand();
		command.setCommand(ExecutionCommandEnum.END_EXECUTION);

		try {
			pythonObjectMapper.writeValue(pythonJsonG, command);
			pythonJsonG.writeRaw("\n");
			pythonJsonG.flush();

			// Expect python to exit at this point.
			try {
				pythonProcess.waitFor();
				cleanupProcess();
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ExecutionException("Interrupted while waiting for python to exit");
			}			
		} catch (IOException e) {
			throw new ExecutionException("Unable to send end execution command to python process");
		}
	}

	@Override
	public void controllerExecutionAborted(Controller controller, Throwable throwable) throws ExecutionException {
		ensureProcess();
		ExecutionCommand command = new ExecutionCommand();
		command.setCommand(ExecutionCommandEnum.ABORT_EXECUTION);

		try {
			pythonObjectMapper.writeValue(pythonJsonG, command);
			pythonJsonG.writeRaw("\n");
			pythonJsonG.flush();
		} catch (IOException e) {
			throw new ExecutionException("Unable to send abort execution command to python process");
		}
	}
}
