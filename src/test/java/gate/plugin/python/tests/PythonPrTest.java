package gate.plugin.python.tests;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.ProcessingResource;
import gate.creole.SerialAnalyserController;
import gate.test.GATEPluginTestCase;
import java.io.File;

public class PythonPrTest extends GATEPluginTestCase {

  
  
  public void testPythonPr01() throws Exception {
    Document doc1 = Factory.newDocument("This is a small document");
    ProcessingResource pr;
    FeatureMap params = Factory.newFeatureMap();
    params.put("pythonBinary", "python3");
    params.put("pythonProgram", new File("./src/test/python/test1.py").toURI().toURL());
    pr = (ProcessingResource)Factory.createResource("gate.plugin.python.PythonPr",params);
    System.err.println("Got pr: "+pr);
    Corpus corpus = Factory.newCorpus("test");
    corpus.add(doc1);
    SerialAnalyserController controller = (SerialAnalyserController) Factory.createResource(
            "gate.creole.SerialAnalyserController");
    controller.add(pr);
    controller.setCorpus(corpus);
    controller.execute();
    AnnotationSet anns = doc1.getAnnotations("Set1");
    System.err.println("Got anns: "+anns);
    assertEquals(1, anns.size());
    Annotation ann = anns.iterator().next();
    System.err.println("Got ann: "+ann);
    assertEquals("Type1", ann.getType());
    assertEquals(1L, (long)ann.getStartNode().getOffset());
    assertEquals(4L, (long)ann.getEndNode().getOffset());
    FeatureMap fm = ann.getFeatures();
    Object f1 = fm.get("f1");
    Object f2 = fm.get("f2");
    assertNotNull(f1);
    assertNotNull(f2);
    assertTrue(f1 instanceof Integer);
    assertTrue(f2 instanceof String);
    assertEquals(12, (int)f1);
    assertEquals("val2", (String)f2);
    fm = doc1.getFeatures();
    f1 = fm.get("feat1");
    f2 = fm.get("feat2");
    assertNotNull(f1);
    assertNotNull(f2);
    assertTrue(f1 instanceof Integer);
    assertTrue(f2 instanceof String);
    assertEquals(13, (int)f1);
    assertEquals("asdf", (String)f2);
  }

  public void testPythonPr02() throws Exception {
    Document doc1 = Factory.newDocument("This is a small document");
    ProcessingResource pr;
    FeatureMap params = Factory.newFeatureMap();
    params.put("pythonBinary", "python3");
    params.put("pythonProgram", new File("./src/test/python/test2.py").toURI().toURL());
    pr = (ProcessingResource)Factory.createResource("gate.plugin.python.PythonPr",params);
    System.err.println("Got pr: "+pr);
    Corpus corpus = Factory.newCorpus("test");
    corpus.add(doc1);
    SerialAnalyserController controller = (SerialAnalyserController) Factory.createResource(
            "gate.creole.SerialAnalyserController");
    controller.add(pr);
    controller.setCorpus(corpus);
    controller.execute();
    AnnotationSet anns = doc1.getAnnotations("Set1");
    System.err.println("Got anns: "+anns);
    assertEquals(1, anns.size());
    Annotation ann = anns.iterator().next();
    System.err.println("Got ann: "+ann);
    assertEquals("Type1", ann.getType());
    assertEquals(1L, (long)ann.getStartNode().getOffset());
    assertEquals(4L, (long)ann.getEndNode().getOffset());
    FeatureMap fm = ann.getFeatures();
    Object f1 = fm.get("f1");
    Object f2 = fm.get("f2");
    assertNotNull(f1);
    assertNotNull(f2);
    assertTrue(f1 instanceof Integer);
    assertTrue(f2 instanceof String);
    assertEquals(12, (int)f1);
    assertEquals("val2", (String)f2);
    fm = doc1.getFeatures();
    f1 = fm.get("feat1");
    f2 = fm.get("feat2");
    assertNotNull(f1);
    assertNotNull(f2);
    assertTrue(f1 instanceof Integer);
    assertTrue(f2 instanceof String);
    assertEquals(13, (int)f1);
    assertEquals("asdf", (String)f2);
  }

}
