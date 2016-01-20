package gate.python;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Gate;
import gate.Utils;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.Files;


import gate.corpora.DocumentJsonUtils;
import gate.util.InvalidOffsetException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonGenerationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

@CreoleResource(name = "Python PR", comment = "Runs a Python script as a processing resource")
public class PythonPR extends AbstractLanguageAnalyser {

	private static final Logger log = Logger.getLogger(PythonPR.class);
	
	private URL pythonBinary;
	
	private URL lastPythonBinary;

	private URL script;
	
	private URL lastScript;

	private String inputAS;
	
	private String outputAS;
	
	private Map<String, Object> scriptParams;
	

	public URL getPythonBinary() {
		return pythonBinary;
	}

	@RunTime
	@CreoleParameter(defaultValue = "python", comment = "Location of the python binary")
	public void setPythonBinary(URL pythonBinary) {
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
	@CreoleParameter(comment = "Input annotation set to use")
	public void setInputAS(String inputAS) {
		this.inputAS = inputAS;
	}

	public String getInputAS() {
		return inputAS;
	}

	@RunTime
	@CreoleParameter(comment = "Output annotation set to use")
	public void setOutputAS(String outputAS) {
		this.outputAS = outputAS;
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
		boolean needNewProcess = (!processRunning() || 
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
			String binPath = Files.fileFromURL(pythonBinary).getAbsolutePath();

			// Windows special case, ensure the binary name ends with .exe
			if(SystemUtils.IS_OS_WINDOWS && !binPath.endsWith(".exe")) {
				binPath += ".exe";
			}

			// Runs python -u some_script.py  (unbuffered)

			ProcessBuilder builder = new ProcessBuilder(binPath, "-u", Files.fileFromURL(script).getAbsolutePath());
			try {
				// Todo: there must be a better way to achieve this
				URL pythonPath = new URL(Gate.getCreoleRegister().get(this.getClass().getName()).getXmlFileUrl(), ".");
				String oldPythonPath = System.getenv("PYTHONPATH");
				oldPythonPath = oldPythonPath != null ? oldPythonPath : ".";
				builder.environment().put("PYTHONPATH", oldPythonPath + ":" + pythonPath.getPath());
			} catch (MalformedURLException e) {
				throw new ExecutionException("Couldn't form python path for running PR", e);
			}
			lastPythonBinary = pythonBinary;
			lastScript = script;

			try {
				pythonProcess = builder.start();
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
		
		// Python is definitely running.
		Map<String, Collection<Annotation>> allAnnotations = new TreeMap<String, Collection<Annotation>>();

		Set<String> annotationSetNames = new TreeSet<String>();
		annotationSetNames.add(""); // Include the default annotation set.
		annotationSetNames.addAll(getDocument().getAnnotationSetNames());

		// Collect all annotations together in a format we can output directly to JSON
		for (String annotationSetName : annotationSetNames) {
			AnnotationSet annotationSet = getDocument().getAnnotations(annotationSetName);

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
			DocumentJsonUtils.writeDocument(getDocument(), 0l, getDocument().getContent().size(), allAnnotations, null, null,
				"annotationID", pythonJsonG);
			pythonJsonG.writeRaw("\n");
			pythonJsonG.flush();
		} catch (InvalidOffsetException e) {
			log.error("Impossible document offset exception", e);
			throw new ExecutionException("Unable to write entire document span", e);
		} catch (JsonGenerationException e) {
			log.error("Unable to generate JSON for document", e);
		} catch (IOException e) {
			log.error("Unable to flush JSON to python process, closing pipeline", e);

			cleanupProcess();
			throw new ExecutionException("Unable to flush JSON to python process", e);
		}


		// Parse the reply, which should be a list of commands.

		AnnotationSet targetAnnotationSet;
		Annotation targetAnnotation;
		try {
			List<Command> commands = (List<Command>) pythonObjectMapper.readValue(pythonOutput, new TypeReference<List<Command>>() {} );
			for (Command command : commands) {
				switch (command.getCommand()) {
					case ADD_ANNOT:
						getDocument().getAnnotations(command.getAnnotationSet()).
										add(command.getStartOffset(), command.getEndOffset(),
												command.getAnnotationName(), Utils.toFeatureMap(command.getFeatureMap()));
						break;
					case REMOVE_ANNOT:
						targetAnnotationSet = getDocument().getAnnotations(command.getAnnotationSet());
						targetAnnotation = targetAnnotationSet.get(command.getAnnotationID());
						targetAnnotationSet.remove(targetAnnotation);
						break;
					case UPDATE_FEATURE:
						targetAnnotationSet = getDocument().getAnnotations(command.getAnnotationSet());
						targetAnnotation = targetAnnotationSet.get(command.getAnnotationID());
						targetAnnotation.getFeatures().put(command.getFeatureName(), command.getFeatureValue());
						break;
					case CLEAR_FEATURES:
						targetAnnotationSet = getDocument().getAnnotations(command.getAnnotationSet());
						targetAnnotation = targetAnnotationSet.get(command.getAnnotationID());
						targetAnnotation.getFeatures().clear();
						break;
					case REMOVE_FEATURE:
						targetAnnotationSet = getDocument().getAnnotations(command.getAnnotationSet());
						targetAnnotation = targetAnnotationSet.get(command.getAnnotationID());
						targetAnnotation.getFeatures().remove(command.getFeatureName());
						break;

				}
			}
		} catch (IOException e) {
			log.error("Unable to read JSON from python process, closing pipeline", e);

			cleanupProcess();
			throw new ExecutionException("Unable to read JSON from python process", e);
		} catch (InvalidOffsetException e) {
			cleanupProcess();
			throw new ExecutionException("Unable to apply changes requested by python script", e);
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
					log.warn("Error discarding stderr output from python", e);
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
}
