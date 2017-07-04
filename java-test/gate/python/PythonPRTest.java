package gate.python;

import gate.*;
import gate.creole.SerialAnalyserController;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
        corpus = Factory.newCorpus("test_corpus");
        document = Factory.newDocument(TEST_TEXT);

        corpus.add(document);

        controller.setCorpus(corpus);
        controller.setDocument(document);
    }

    @Test
    public void addDocumentSpan() throws Exception {
        pythonPR.setScript(new File("examples/add_document_span.py").toURI().toURL());
        controller.execute();

        // Check that we got a new annotation in the document.
        assertTrue(document.getAnnotationSetNames().contains("python"));
        assertEquals(document.getAnnotations("python").size(), 1);
        assertEquals(document.getAnnotations("python").get(0).getStartNode().getOffset().longValue(), 0l);
        assertEquals(document.getAnnotations("python").get(0).getEndNode().getOffset().intValue(),
                TEST_TEXT.length());
        assertEquals(document.getAnnotations("python").get(0).getFeatures().get("text"),
                TEST_TEXT);

    }

    @After
    public void tearDown() throws Exception {
        Factory.deleteResource(pythonPR);
        Factory.deleteResource(controller);
        Factory.deleteResource(corpus);
        Factory.deleteResource(document);

    }

}