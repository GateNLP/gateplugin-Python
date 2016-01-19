package gate.python;

import gate.Annotation;
import gate.AnnotationSet;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.TreeMap;
import java.util.Map;
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

			// Runs python some_script.py 
			ProcessBuilder builder = new ProcessBuilder(binPath, Files.fileFromURL(script).getAbsolutePath());
			lastPythonBinary = pythonBinary;
			lastScript = script;

			try {
				pythonProcess = builder.start();
				discard(pythonProcess.getErrorStream());
				pythonInput = new PrintWriter(new OutputStreamWriter(pythonProcess.getOutputStream(), "UTF-8"));
				pythonOutput = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream(), "UTF-8"));

				JsonFactory factory = new JsonFactory(); // Todo: maybe move this into Init if it causes problems

				pythonJsonG = factory.createGenerator(pythonInput);
			} catch(IOException e) {
				throw new ExecutionException("Could not start python", e);
			}
		}
		
		// Python is definitely running.
		Map<String, Collection<Annotation>> allAnnotations = new TreeMap<String, Collection<Annotation>>();

		// Collect all annotations together in a format we can output directly to JSON
		for (String annotationSetName : getDocument().getAnnotationSetNames()) {
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

		// try {
		// 		// morfette replies with one line per token followed by a blank
		// 		Iterator<String> stringIt = tokenStrings.iterator();
		// 		for(Annotation tok : tokensInSentence) {
		// 			String tokenString = stringIt.next(); // guaranteed to work as strings and tokens lists are parallel
		// 			String line = pythonOutput.readLine();
		// 			String[] fields = spacePattern.split(line, 3);
		// 			// first field is the string, second the name, third the morphosyntactic tag
		// 			if(!tokenString.equals(fields[0])) {
		// 				// something wrong!
		// 				throw new ExecutionException("Mismatched token returned by morfette - returned \"" + fields[0] + "\" but expected \"" + tokenString + "\"");
		// 			}
		// 			if(fields.length > 1) {
		// 				tok.getFeatures().put("root", fields[1]);
		// 			} else {
		// 				log.warn("Morfette failed to return a root for token " + tok + " in document " + getDocument().getName());
		// 			}
		// 			if(fields.length > 2) {
		// 				tok.getFeatures().put("morfetteTag", fields[2]);
		// 			}
		// 		}
		// 		// read the blank line following sentence
		// 		pythonOutput.readLine();
		// 	} catch(Exception e) {
		// 		// something went wrong, kill process to start clean next time
		// 		cleanupProcess();
		// 		if(e instanceof ExecutionException) {
		// 			throw (ExecutionException)e;
		// 		} else {
		// 			throw new ExecutionException("Exception running python", e);
		// 		}
		// }

		// AnnotationSet inputAS = getDocument().getAnnotations(annotationSetName);
		// List<Annotation> sentences = Utils.inDocumentOrder(inputAS.get(sentenceAnnotationType));
		// AnnotationSet allTokens = inputAS.get(tokenAnnotationType);
		// for(Annotation sentence : sentences) {
		// 	List<Annotation> tokensInSentence = Utils.inDocumentOrder(Utils.getContainedAnnotations(allTokens, sentence));
		// 	List<String> tokenStrings = new ArrayList<>();
		// 	for(Annotation tok : tokensInSentence) {
		// 		String tokenString = String.valueOf(tok.getFeatures().get(tokenStringFeature));
		// 		// morfette absolutely forbids spaces in token strings, so...
		// 		Matcher m = upToSpacePattern.matcher(tokenString);
		// 		m.find(); // cannot fail
		// 		tokenString = m.group(1);
		// 		tokenStrings.add(tokenString);
		// 		pythonInput.println(tokenString);
		// 	}
			
			
		// }
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
	
	protected void discard(final InputStream stream) {
		Thread t = new Thread() {
			public void run() {
				try {
					IOUtils.copy(stream, (log.isDebugEnabled() ? System.err : NullOutputStream.NULL_OUTPUT_STREAM));
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
				IOUtils.copy(pythonOutput, (log.isDebugEnabled() ? System.err : NullOutputStream.NULL_OUTPUT_STREAM));
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
