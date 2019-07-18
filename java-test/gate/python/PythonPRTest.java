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

import static org.junit.Assert.*;

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
        controller.setDocument(this.document);

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

        controller.controllerExecutionStarted(controller);
        controller.execute();
        controller.controllerExecutionFinished(controller);

        // Check that we got a new annotation in the document.
        assertTrue(document.getAnnotationSetNames().contains("python"));
        assertEquals(document.getAnnotations("python").size(), 1);
        assertEquals(document.getAnnotations("python").get(0).getStartNode().getOffset().longValue(), 0l);
        assertEquals(document.getAnnotations("python").get(0).getEndNode().getOffset().intValue(),
                TEST_TEXT.length());
        assertEquals(document.getAnnotations("python").get(0).getFeatures().get("text"),
                TEST_TEXT);

    }

    @Test
    public void testFunctionPR() throws Exception {
        pythonPR.setScript(new File("examples/add_document_span_function.py").toURI().toURL());
        pythonPR.setOutputAS("python");

        controller.controllerExecutionStarted(controller);
        controller.execute();
        controller.controllerExecutionFinished(controller);

        // Check that we got a new annotation in the document.
        assertTrue(document.getAnnotationSetNames().contains("python"));
        assertEquals(document.getAnnotations("python").size(), 1);
        assertEquals(document.getAnnotations("python").get(0).getStartNode().getOffset().longValue(), 0l);
        assertEquals(document.getAnnotations("python").get(0).getEndNode().getOffset().intValue(),
                TEST_TEXT.length());
        assertEquals(document.getAnnotations("python").get(0).getFeatures().get("text"),
                TEST_TEXT);

    }

    // In GATE 8.4.1 and 8.6 test fails because
    // some problem with default annotation set?
    @Ignore
    @Test
    public void testNullKeyInMap() throws Exception {
        pythonPR.setScript(new File("examples/tokenise_badly.py").toURI().toURL());

        loadDocumentXML(this.getClass().getResource("/bad_document.xml"));

        controller.controllerExecutionStarted(controller);
        controller.execute();
        controller.controllerExecutionFinished(controller);

        // Check that we got a new annotation in the document.
        assertEquals(document.getAnnotations().size(), 13);
    }


    // In GATE 8.4.1 and 8.6 test fails because
    // some problem with default annotation set?
    @Ignore
    @Test
    public void testAnnotationInDefaultSet() throws Exception {
        pythonPR.setScript(new File("examples/tokenise_badly.py").toURI().toURL());

        Document document = Factory.newDocument("anything goes");

        assertEquals(0, document.getAnnotations().size());

        controller.controllerExecutionStarted(controller);
        controller.execute();
        controller.controllerExecutionFinished(controller);

        // Check that we got a new annotation in the document.
        assertEquals(1, document.getAnnotations().size());
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
