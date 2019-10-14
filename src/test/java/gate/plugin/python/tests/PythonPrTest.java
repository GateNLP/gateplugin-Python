package gate.plugin.python.tests;

import gate.*;
import gate.creole.Plugin;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.plugin.python.PythonPr;
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
// TODO: need to re-work this, in here just testing the functions within 
// the plugin, turn the test where we just load the plugin into an integration
// test!
public class PythonPrTest {
  /*
    private static final String TEST_TEXT = "this is a test document";
    private SerialAnalyserController controller;
    private PythonPr pythonPr;
    private Corpus corpus;
    private Document document;

    @BeforeClass
    public static void setupClass() throws Exception {
        Gate.init();
        Gate.getCreoleRegister().registerPlugin(new Plugin.Maven("uk.ac.gate.plugins","python","2.0-SNAPSHOT"));
    }
    @Before
    public void setUp() throws Exception {
        controller = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");

        pythonPr = (PythonPr) Factory.createResource("gate.plugin.python.PythonPr");
        String python = System.getProperty("gate.python.binary");
        if(python != null) {
            pythonPr.setParameterValue("pythonBinary", python);
        }

        controller.add(pythonPr);
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
        pythonPr.setPythonProgramUrl(new File("examples/add_document_span.py").toURI().toURL());

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
        pythonPr.setPythonProgramUrl(new File("examples/add_document_span_function.py").toURI().toURL());
        pythonPr.setOutputAS("python");

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
        pythonPr.setPythonProgramUrl(new File("examples/tokenise_badly.py").toURI().toURL());

        loadDocumentXML(new File("test-resources/bad_document.xml").toURI().toURL());

        controller.execute();

        // Check that we got a new annotation in the document.
        assertEquals(13, document.getAnnotations().size());
    }


    @Test
    public void testAnnotationInDefaultSet() throws Exception {
        pythonPr.setPythonProgramUrl(new File("examples/tokenise_badly.py").toURI().toURL());

        // send an annotation over to python
        document.getAnnotations().add(0l, 4l, "test", Factory.newFeatureMap());

        assertEquals(1, document.getAnnotations().size());

        controller.execute();

        // Check that we got usual token annotations added in addition to the one we sent
        assertEquals(6, document.getAnnotations().size());
    }

    @After
    public void tearDown() throws Exception {
        Factory.deleteResource(pythonPr);
        Factory.deleteResource(controller);
        Factory.deleteResource(corpus);
        Factory.deleteResource(document);
        corpus = null;
        document = null;
    }
    */
}
