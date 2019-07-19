package gate.python;

import gate.*;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Created by "Dominic Rout" on 04/07/2017.
 */
public class PythonPRTest {
    private static final String TEST_TEXT = "this is a test document";
    private SerialAnalyserController controller;
    private PythonPR pythonPR;
    private Corpus corpus;
    private Document document;

    @BeforeClass
    public static void setupClass() throws Exception {
        Gate.init();
        Gate.getCreoleRegister().registerDirectories(new File(".").toURI().toURL());  // Add this directory as a plugin.
    }
    @Before
    public void setUp() throws Exception {
        controller = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");

        pythonPR = (PythonPR) Factory.createResource("gate.python.PythonPR");
        String python = System.getProperty("gate.python.binary");
        if(python != null) {
            pythonPR.setParameterValue("pythonBinary", python);
        }

        controller.add(pythonPR);
        loadDocument(TEST_TEXT);
    }

    public void setDocument(Document document) throws ResourceInstantiationException {
        if (corpus != null) {
            Factory.deleteResource(corpus);
        }

        if (this.document != null) {
            Factory.deleteResource(this.document);
        }

        corpus = Factory.newCorpus("test_corpus");
        this.document = document;

        corpus.add(this.document);

        controller.setCorpus(corpus);

    }

    public void loadDocument(String documentText) throws ResourceInstantiationException {
        setDocument(Factory.newDocument(documentText));
    }

    public void loadDocumentXML(URL url) throws ResourceInstantiationException {
        setDocument(Factory.newDocument(url));
    }



    @Test
    public void addDocumentSpan() throws Exception {
        pythonPR.setScript(new File("examples/add_document_span.py").toURI().toURL());

        controller.execute();

        // Check that we got a new annotation in the document.
        assertTrue(document.getAnnotationSetNames().contains("python"));
        assertEquals(1, document.getAnnotations("python").size());
        assertEquals(0L, document.getAnnotations("python").get(0).getStartNode().getOffset().longValue());
        assertEquals(TEST_TEXT.length(),
            document.getAnnotations("python").get(0).getEndNode().getOffset().intValue());
        assertEquals(TEST_TEXT,
            document.getAnnotations("python").get(0).getFeatures().get("text"));

    }

    @Test
    public void testFunctionPR() throws Exception {
        pythonPR.setScript(new File("examples/add_document_span_function.py").toURI().toURL());
        pythonPR.setOutputAS("python");

        controller.execute();

        // Check that we got a new annotation in the document.
        assertTrue(document.getAnnotationSetNames().contains("python"));
        assertEquals(1, document.getAnnotations("python").size());
        assertEquals(0L, document.getAnnotations("python").get(0).getStartNode().getOffset().longValue());
        assertEquals(TEST_TEXT.length(),
            document.getAnnotations("python").get(0).getEndNode().getOffset().intValue());
        assertEquals(TEST_TEXT,
            document.getAnnotations("python").get(0).getFeatures().get("text"));

    }

    // In GATE 8.4.1 and 8.6 test fails because
    // we don't do anything sensible with null map keys
    @Ignore
    @Test
    public void testNullKeyInMap() throws Exception {
        pythonPR.setScript(new File("examples/tokenise_badly.py").toURI().toURL());

        loadDocumentXML(new File("test-resources/bad_document.xml").toURI().toURL());

        controller.execute();

        // Check that we got a new annotation in the document.
        assertEquals(13, document.getAnnotations().size());
    }


    @Test
    public void testAnnotationInDefaultSet() throws Exception {
        pythonPR.setScript(new File("examples/tokenise_badly.py").toURI().toURL());

        // send an annotation over to python
        document.getAnnotations().add(0l, 4l, "test", Factory.newFeatureMap());

        assertEquals(1, document.getAnnotations().size());

        controller.execute();

        // Check that we got usual token annotations added in addition to the one we sent
        assertEquals(6, document.getAnnotations().size());
    }

    @After
    public void tearDown() throws Exception {
        Factory.deleteResource(pythonPR);
        Factory.deleteResource(controller);
        Factory.deleteResource(corpus);
        Factory.deleteResource(document);
        corpus = null;
        document = null;
    }

}
