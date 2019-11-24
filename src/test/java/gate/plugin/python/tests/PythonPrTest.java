package gate.plugin.python.tests;

import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.Plugin;
import gate.test.GATEPluginTestCase;
import java.io.File;

public class PythonPrTest extends GATEPluginTestCase {

  
  
  public void testPythonPr01() throws Exception {
    //Gate.getCreoleRegister().registerPlugin(
    //        new Plugin.Maven("uk.ac.gate.plugins","python","2.0-SNAPSHOT"));
    
    Document doc1 = Factory.newDocument("This is a small document");
    ProcessingResource pr;
    FeatureMap params = Factory.newFeatureMap();
    params.put("pythonProgram", new File("./examples/add_document_span.py").toURI().toURL());
    pr = (ProcessingResource)Factory.createResource("gate.plugin.python.PythonPr",params);
    System.err.println("Got pr: "+pr);
  }
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
