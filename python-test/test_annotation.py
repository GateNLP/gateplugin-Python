"""
  Runs the same tests over the annotation sets as provided in GATE java.

  This will actually use GATE to load the documents for now because we don't support
  parsing the XML files directly.
"""

from gate import *
import unittest, sys, os

"""Tests for the Annotation classes"""

Gate = gate.Gate() # Use this for loading documents
class TestAnnotationSet(unittest.TestCase):
  def setUp(self): 
    self.document = Gate.load("data/doc0.html")

    self.basicAS = self.document.annotationSets[None]

    self.basicAS.add(10, 20, "T1", {})
    self.basicAS.add(10, 20, "T2", {})
    self.basicAS.add(10, 20, "T3", {})
    self.basicAS.add(10, 20, "T1", {})

    fm = {"pos": "NN",
          "author": "hamish",
          "version": 1}

    self.basicAS.add(10, 20, "T1", fm)
    self.basicAS.add(15, 40, "T1", fm)
    self.basicAS.add(15, 40, "T3", fm)
    self.basicAS.add(15, 40, "T1", fm)

    fm = {
      "pos": "JJ",
      "author": "the devil himself",
      "version": 44,
      "created": "monday"
    }

    self.basicAS.add(15, 40, "T3", fm)
    self.basicAS.add(15, 40, "T1", fm)
    self.basicAS.add(15, 40, "T1", fm)


  def testOffsetIndex(self):
    """Test indexing by offset"""
    localAS = AnnotationSet(self.document)

    newId = localAS.add(10, 20, "T", {}).id
    self.assertEquals(newId, 1)

    a = localAS.byID(newId)

    self.assertEquals(a.start, 10)
    self.assertEquals(a.end, 20)

    newId = localAS.add(10, 30, "T", {}).id
    self.assertEquals(newId, 2)
    a = localAS.byID(newId)

    self.assertEquals(a.start, 10)
    self.assertEquals(a.end, 30)

    self.assertEquals(len(localAS.at(10)), 2)

  # Immutability tests have been dropped because I don't think I need immutability

  def testExceptions(self):
    """Test offset exception throwing"""
    localAS = AnnotationSet(self.document)

    with self.assertRaises(InvalidOffsetException):
      localAS.add(-1, 1, "T", {})

    with self.assertRaises(InvalidOffsetException):
      localAS.add(1, -1, "T", {})

    with self.assertRaises(InvalidOffsetException):
      localAS.add(1, 0, "T", {})

    with self.assertRaises(InvalidOffsetException):
      localAS.add(None, 1, "T", {})

    with self.assertRaises(InvalidOffsetException):
      localAS.add(1, None, "T", {})

    with self.assertRaises(InvalidOffsetException):
      localAS.add(999999, 100000000, "T", {})


  def testTypeIndex(self):
    """Test type index"""
    doc = Gate.load("data/doc0.html")
    localAS = AnnotationSet(doc)


    localAS.add(10, 20, "T1", {});    # 0
    localAS.add(10, 20, "T2", {});    # 1
    localAS.add(10, 20, "T3", {});    # 2
    localAS.add(10, 20, "T1", {});    # 3
    localAS.add(10, 20, "T1", {});    # 4
    localAS.add(10, 20, "T1", {});    # 5
    # to trigger type indexing
    localAS["T"]

    localAS.add(10, 20, "T3", {});    # 6
    localAS.add(10, 20, "T1", {});    # 7
    localAS.add(10, 20, "T3", {});    # 8
    localAS.add(10, 20, "T1", {});    # 9
    localAS.add(10, 20, "T1", {});    # 10

    self.assertEquals(0, localAS["T"].size())
    self.assertEquals(7, localAS["T1"].size())
    self.assertEquals(1, localAS["T2"].size())
    self.assertEquals(3, localAS["T3"].size())

    # let's check that we've only got two nodes, what the ids are and so on
    for idCounter, a in enumerate(localAS):
      # check annot ids
      self.assertEquals(idCounter+1, a.id)

      # start offset
      self.assertEquals(10, a.start)
      # end offset
      self.assertEquals(20, a.end)

  def testGetCovering(self):
    self.assertEquals(0, self.basicAS[None].covering(0,5).size())
    self.assertEquals(0, self.basicAS["T1"].covering(0,5).size())

    #None and blank strings should be treated the same.  Just test
    #with both a couple of times.  Mostly can just test with None.
    self.assertEquals(0, self.basicAS[None].covering(9,12).size())
    # self.assertEquals(0, self.basicAS["  "].covering(9,12).size()) # REMOVED: I don't think this is a good idea
    self.assertEquals(0, self.basicAS["T1"].covering(9,12).size())

    self.assertEquals(5, self.basicAS[None].covering(10,20).size())
    # self.assertEquals(5, self.basicAS["  "].covering(10,20).size())
    self.assertEquals(3, self.basicAS["T1"].covering(10,20).size())

    self.assertEquals(11, self.basicAS[None].covering(16,20).size())
    self.assertEquals(7, self.basicAS["T1"].covering(16,20).size())

    self.assertEquals(6, self.basicAS[None].covering(16,21).size())
    self.assertEquals(4, self.basicAS["T1"].covering(16,21).size())


  def testRemove(self):
    result = self.basicAS["T1"]
    self.assertEquals(7, result.size())

    result = self.basicAS.firstAfter(9)
    self.assertEquals(5, result.size())

    annotation = self.basicAS.byID(1)
    self.basicAS.remove(annotation) # REmove annotation with index 1

    self.assertEquals(10, self.basicAS.size())
    self.assertEquals(10, len(self.basicAS._annots))

    result = self.basicAS.type("T1")
    self.assertEquals(6, result.size())

    result = self.basicAS.firstAfter(9)
    self.assertEquals(4, result.size())

    with self.assertRaises(KeyError):
      self.basicAS.byID(1)

    self.basicAS.remove(self.basicAS.byID(8))
    self.assertEquals(9, self.basicAS.size())
    for annotation in self.basicAS.type(None): # Type is called to create a clone in case iteration is unsafe.
      self.basicAS.remove(annotation)
    self.assertEquals(0, self.basicAS.size())
    self.assertEquals(0, self.basicAS.type("T1").size())

    with self.assertRaises(KeyError):
      self.basicAS.byID(2)

  def testRemoveInexistant(self):
    self.basicAS.add(0, 10, "Foo", {})
    ann = self.basicAS.type("Foo").first()

    self.basicAS.remove(ann)
    with self.assertRaises(KeyError): # GATE allows this, but it doesn't seem like a good idea...
      self.basicAS.remove(ann)

  # Iterator removal tests were removed because removing from an iterator is not very pythonic.

  def testSetMethods(self):
    T1 = self.basicAS.type("T1") # Remove all T1 annotations from the annotation set.
    newAS = self.basicAS - T1 

    self.assertEquals(4, newAS.size())

    # Put T1 back in
    newAS |= T1
    self.assertEquals(11, newAS.size())

    # Get *only* T1 using intersection (no idea why)
    newAS &= T1
    self.assertEquals(7, newAS.size())

    # Get T2 using another convoluted method
    newAS = self.basicAS ^ (T1 | self.basicAS.type("T3") | self.basicAS.type("T3"))
    self.assertEquals(1, newAS.size())

  # def testGap(self):
  #   """Test get with offset and no annotation starting at given offset"""
  #   localAS = self.basicAS
  #   localAS.clear()
  #   FeatureMap fm = Factory.newFeatureMap()
  #   fm.put("A", "B")
  #   localAS.add(0, 10, "foo", fm)
  #   localAS.add(11, 20, "foo", fm)
  #   localAS.add(10, 11, "space", fm)

  #   #do the input selection (ignore spaces)
  #   input = {"foo", "foofoo")
  #   AnnotationSet annotations = null

  #   if(input.isEmpty()) annotations = localAS
  #   else{
  #     Iterator<String> typesIter = input.iterator()
  #     AnnotationSet ofOneType = null

  #     while(typesIter.hasNext()){
  #       ofOneType = localAS.get(typesIter.next())

  #       if(ofOneType != null){
  #         #System.out.println("Adding " + ofOneType.getAllTypes())
  #         if(annotations == null) annotations = ofOneType
  #         else annotations.addAll(ofOneType)
  #       }
  #     }
  #   }
  #   # if(annotations == null) annotations = AnnotationSetImpl(doc)
  #   if (DEBUG)
  #     Out.println(
  #       "Actual input:" + annotations.getAllTypes() + "\n" + annotations
  #     )

  #   AnnotationSet res =
  #     annotations.get("foo", Factory.newFeatureMap(), 10)

  #   if (DEBUG)
  #     Out.println(res)
  #   assertTrue(!res.isEmpty())
  # }

#   """Test Overlaps"""
#   public void testOverlapsAndCoextensive() throws InvalidOffsetException {
#     Node node1 = NodeImpl(new Integer(1),10)
#     Node node2 = NodeImpl(new Integer(2),20)
#     Node node4 = NodeImpl(new Integer(4),15)
#     Node node5 = NodeImpl(new Integer(5),20)
#     Node node6 = NodeImpl(new Integer(6),30)

#     FeatureMap fm1 = SimpleFeatureMapImpl()
#     fm1.put("color","red")
#     fm1.put("Age",25)
#     fm1.put(23, "Cristian")

#     FeatureMap fm2 = SimpleFeatureMapImpl()
#     fm2.put("color","red")
#     fm2.put("Age",25)
#     fm2.put(23, "Cristian")

#     FeatureMap fm4 = SimpleFeatureMapImpl()
#     fm4.put("color","red")
#     fm4.put("Age",26)
#     fm4.put(23, "Cristian")

#     FeatureMap fm3 = SimpleFeatureMapImpl()
#     fm3.put("color","red")
#     fm3.put("Age",25)
#     fm3.put(23, "Cristian")
#     fm3.put("best",new Boolean(true))

#     # Start=10, End = 20
#     Annotation annot1 = createAnnotation(new Integer(1),
#                                            node1,
#                                            node2,
#                                            "pos",
#                                            null)
#     # Start=20, End = 30
#     Annotation annot2 = createAnnotation (new Integer(2),
#                                             node2,
#                                             node6,
#                                             "pos",
#                                             null)
#     # Start=20, End = 30
#     Annotation annot3 = createAnnotation (new Integer(3),
#                                             node5,
#                                             node6,
#                                             "pos",
#                                             null)
#     # Start=20, End = 20
#     Annotation annot4 = createAnnotation (new Integer(4),
#                                             node2,
#                                             node5,
#                                             "pos",
#                                             null)
#     # Start=10, End = 30
#     Annotation annot5 = createAnnotation (new Integer(5),
#                                             node1,
#                                             node6,
#                                             "pos",
#                                             null)
#     # Start=10, End = 15
#     Annotation annot6 = createAnnotation (new Integer(6),
#                                             node1,
#                                             node4,
#                                             "pos",
#                                             null)
#     # Start=null, End = null
#     Annotation annot7 = createAnnotation (new Integer(7),
#                                             null,
#                                             null,
#                                             "pos",
#                                             null)

#     # MAP
#     # annot1 -> Start=10, End = 20
#     # annot2 -> Start=20, End = 30
#     # annot3 -> Start=20, End = 30
#     # annot4 -> Start=20, End = 20
#     # annot5 -> Start=10, End = 30
#     # annot6 -> Start=10, End = 15

#     # Not overlaping situations
#    assertTrue("Those annotations does not overlap!",!annot1.overlaps(annot3))
#    assertTrue("Those annotations does not overlap!",!annot1.overlaps(annot2))
#    assertTrue("Those annotations does not overlap!",!annot2.overlaps(annot1))
#    assertTrue("Those annotations does not overlap!",!annot3.overlaps(annot1))
#    assertTrue("Those annotations does not overlap!",!annot4.overlaps(annot6))
#    assertTrue("Those annotations does not overlap!",!annot6.overlaps(annot4))

#    assertTrue("Those annotations does not overlap!",!annot6.overlaps(null))
#    assertTrue("Those annotations does not overlap!",!annot1.overlaps(annot7))

#    # Overlaping situations
#    assertTrue("Those annotations does overlap!",annot4.overlaps(annot5))
#    assertTrue("Those annotations does overlap!",annot5.overlaps(annot4))
#    assertTrue("Those annotations does overlap!",annot1.overlaps(annot6))
#    assertTrue("Those annotations does overlap!",annot6.overlaps(annot1))
#    assertTrue("Those annotations does overlap!",annot2.overlaps(annot5))
#    assertTrue("Those annotations does overlap!",annot5.overlaps(annot2))

#    # Not coextensive situations
#    assertTrue("Those annotations are not coextensive!",!annot1.coextensive(annot2))
#    assertTrue("Those annotations are not coextensive!",!annot2.coextensive(annot1))
#    assertTrue("Those annotations are not coextensive!",!annot4.coextensive(annot3))
#    assertTrue("Those annotations are not coextensive!",!annot3.coextensive(annot4))
#    assertTrue("Those annotations are not coextensive!",!annot4.coextensive(annot7))
#    assertTrue("Those annotations are not coextensive!",!annot5.coextensive(annot6))
#    assertTrue("Those annotations are not coextensive!",!annot6.coextensive(annot5))
#    #Coextensive situations
#    assertTrue("Those annotations are coextensive!",annot2.coextensive(annot2))
#    assertTrue("Those annotations are coextensive!",annot2.coextensive(annot3))
#    assertTrue("Those annotations are coextensive!",annot3.coextensive(annot2))

#   }#testOverlapsAndCoextensive

#   """Test Coextensive"""
#   public void testIsPartiallyCompatibleAndCompatible()
#                                                 throws InvalidOffsetException {
#     Node node1 = NodeImpl(new Integer(1),10)
#     Node node2 = NodeImpl(new Integer(2),20)
#     Node node4 = NodeImpl(new Integer(4),15)
#     Node node5 = NodeImpl(new Integer(5),20)
#     Node node6 = NodeImpl(new Integer(6),30)

#     FeatureMap fm1 = SimpleFeatureMapImpl()
#     fm1.put("color","red")
#     fm1.put("Age",25)
#     fm1.put(23, "Cristian")

#     FeatureMap fm2 = SimpleFeatureMapImpl()
#     fm2.put("color","red")
#     fm2.put("Age",25)
#     fm2.put(23, "Cristian")

#     FeatureMap fm4 = SimpleFeatureMapImpl()
#     fm4.put("color","red")
#     fm4.put("Age",26)
#     fm4.put(23, "Cristian")

#     FeatureMap fm3 = SimpleFeatureMapImpl()
#     fm3.put("color","red")
#     fm3.put("Age",25)
#     fm3.put(23, "Cristian")
#     fm3.put("best",new Boolean(true))

#     # Start=10, End = 20
#     Annotation annot1 = createAnnotation(new Integer(1),
#                                            node1,
#                                            node2,
#                                            "pos",
#                                            fm1)
#     # Start=20, End = 30
#     Annotation annot2 = createAnnotation (new Integer(2),
#                                             node2,
#                                             node6,
#                                             "pos",
#                                             fm2)
#     # Start=20, End = 30
#     Annotation annot3 = createAnnotation (new Integer(3),
#                                             node5,
#                                             node6,
#                                             "pos",
#                                             fm3)
#     # Start=20, End = 20
#     Annotation annot4 = createAnnotation (new Integer(4),
#                                             node2,
#                                             node5,
#                                             "pos",
#                                             fm4)
#     # Start=10, End = 30
#     Annotation annot5 = createAnnotation (new Integer(5),
#                                             node1,
#                                             node6,
#                                             "pos",
#                                             fm3)
#     # Start=10, End = 15
#     Annotation annot6 = createAnnotation (new Integer(6),
#                                             node1,
#                                             node4,
#                                             "pos",
#                                             fm1)

# # MAP
#   /*
#    annot1 -> Start=10, End = 20,{color="red",Age="25",23="Cristian"}
#    annot2 -> Start=20, End = 30,{color="red",Age="25",23="Cristian"}
#    annot3 -> Start=20, End = 30,{color="red",Age="25",23="Cristian",best="true"}
#    annot4 -> Start=20, End = 20,{color="red",Age="26",23="Cristian"}
#    annot5 -> Start=10, End = 30,{color="red",Age="25",23="Cristian",best="true"}
#    annot6 -> Start=10, End = 15,{color="red",Age="25",23="Cristian"}
#   */
#   # Not compatible situations
#   assertTrue("Those annotations are not compatible!",!annot3.isCompatible(annot2))

#   # Not partially compatible situations
#   # They don't overlap
#   assertTrue("Those annotations("+ annot1 +" & " +
#                                annot2+ ") are not partially compatible!",
#                                        !annot1.isPartiallyCompatible(annot2))

#   # Again they don't overlap
#   assertTrue("Those annotations("+ annot1 +" & " +
#                                annot3+ ") are not partially compatible!",
#                                        !annot1.isPartiallyCompatible(annot3))
#   # Fails because of the age value
#   assertTrue("Those annotations("+ annot1 +" & " +
#                                annot4+ ") are not partially compatible!",
#                                        !annot1.isPartiallyCompatible(annot4))
#   # Fails because of the value of Age
#   assertTrue("Those annotations("+ annot4 +" & " +
#                                annot5+ ") are not partially compatible!",
#                                        !annot4.isPartiallyCompatible(annot5))
#   # Features from annot6 does not subsumes features annot3
#   assertTrue("Those annotations("+ annot3 +" & " +
#                                annot6+ ") are not partially compatible!",
#                                !annot3.isPartiallyCompatible(annot6,null))
#   # Features from annot2 does not subsumes features annot5
#   assertTrue("Those annotations("+ annot5 +" & " +
#                                annot2+ ") are not partially compatible!",
#                                !annot5.isPartiallyCompatible(annot2,null))
#   Set<Object> keySet = HashSet<Object>()
#   # They don't overlap
#   assertTrue("Those annotations("+ annot2 +" & " +
#                                annot4+ ") are not partially compatible!",
#                                !annot2.isPartiallyCompatible(annot4,keySet))
#   keySet.add("color")
#   keySet.add("Age")
#   keySet.add("best")
#   # Fails because of best feture
#   assertTrue("Those annotations("+ annot5 +" & " +
#                                annot2+ ") are not partially compatible!",
#                                !annot5.isPartiallyCompatible(annot2,keySet))
#   # Fails because start=end in both cases and they don't overlap
#   assertTrue("Those annotations("+ annot4 +" & " +
#                                annot4+ ") are not partially compatible!",
#                                         !annot4.isPartiallyCompatible(annot4))

#   /*
#    annot1 -> Start=10, End = 20,{color="red",Age="25",23="Cristian"}
#    annot2 -> Start=20, End = 30,{color="red",Age="25",23="Cristian"}
#    annot3 -> Start=20, End = 30,{color="red",Age="25",23="Cristian",best="true"}
#    annot4 -> Start=20, End = 20,{color="red",Age="26",23="Cristian"}
#    annot5 -> Start=10, End = 30,{color="red",Age="25",23="Cristian",best="true"}
#    annot6 -> Start=10, End = 15,{color="red",Age="25",23="Cristian"}
#   */

#   # Compatible situations
#   assertTrue("Those annotations("+ annot2 +" & " +
#                                annot3+ ") should be compatible!",
#                                       annot2.isCompatible(annot3))
#   assertTrue("Those annotations("+ annot2 +" & " +
#                                annot3+ ") should be compatible!",
#                                       annot2.isCompatible(annot3,null))
#   assertTrue("Those annotations("+ annot2 +" & " +
#                                annot3+ ") should be compatible!",
#                                      annot2.isCompatible(annot3,new HashSet<String>()))
#   assertTrue("Those annotations("+ annot4 +" & " +
#                                annot4+ ") should be compatible!",
#                                         annot4.isCompatible(annot4))
#   keySet = HashSet<Object>()
#   keySet.add("color")
#   keySet.add(23)
#   assertTrue("Those annotations("+ annot3 +" & " +
#                                annot2+ ") should be compatible!",
#                                       annot3.isCompatible(annot2,keySet))

#   # Partially compatible situations
#   assertTrue("Those annotations("+ annot2 +" & " +
#                                annot3+ ") should be partially compatible!",
#                                         annot2.isPartiallyCompatible(annot3))
#   assertTrue("Those annotations("+ annot2 +" & " +
#                                annot2+ ") should be partially compatible!",
#                                         annot2.isPartiallyCompatible(annot2))
#   assertTrue("Those annotations are partially compatible!",
#                                         annot1.isPartiallyCompatible(annot5))
#   assertTrue("Those annotations are partially compatible!",
#                                         annot1.isPartiallyCompatible(annot6))
#   assertTrue("Those annotations are partially compatible!",
#                                         annot3.isPartiallyCompatible(annot5))
#   assertTrue("Those annotations are partially compatible!",
#                                         annot5.isPartiallyCompatible(annot3))
#   assertTrue("Those annotations are partially compatible!",
#                                         annot6.isPartiallyCompatible(annot5))

#   }# testIsPartiallyCompatibleAndCompatible


#   public void testFeatureSubsumeMethods(){

#     FeatureMap fm1 = Factory.newFeatureMap()
#     fm1.put("k1","v1")
#     fm1.put("k2","v2")

#     FeatureMap fm2 = Factory.newFeatureMap()
#     fm2.put("k1","v1")

#     Set<String> featKeysSet1 = HashSet<String>()
#     featKeysSet1.add("k1")
#     featKeysSet1.add("k2")
#     featKeysSet1.add("k3")
#     featKeysSet1.add("k4")

#     assertTrue(fm1 + " should subsume " + fm2 + " using the key set" +
#                                featKeysSet1,fm1.subsumes(fm2, featKeysSet1))
#     assertTrue(fm1 + " should subsume " + fm2 +
#                             " taking all feat into consideration",
#                             fm1.subsumes(fm2, null))

#     FeatureMap fm3 = Factory.newFeatureMap()
#     fm3.put("k1","v1")
#     fm3.put("k2","v2")
#     fm3.put("k3",new Integer(3))

#     Set<String> featKeysSet2 = HashSet<String>()
#     featKeysSet2.add("k1")

#     assertTrue(fm1 + " should subsume " + fm3 + " using the key set" +
#                           featKeysSet2,fm1.subsumes(fm3, featKeysSet2))
#     assertTrue(fm1 + " should NOT subsume " + fm3 +
#                                 " taking all feats into consideration",
#                                 !fm1.subsumes(fm3,null))

#     FeatureMap fm4 = Factory.newFeatureMap()
#     fm4.put("k1",new Integer(2))
#     fm4.put("k2","v2")
#     fm4.put("k3","v3")

#     Set<String> featKeysSet3 = HashSet<String>()
#     featKeysSet3.add("k2")

#     assertTrue(fm3 + " should subsume " + fm4 + " using the key set" +
#                               featKeysSet3, fm4.subsumes(fm3,featKeysSet3))
#     assertTrue(fm4 + " should NOT subsume " + fm3 +
#                                 " taking all feats into consideration",
#                                 !fm4.subsumes(fm3,null))


#   }# testFeatureSubsumeMethods()

#   protected Annotation createAnnotation
#     (Integer id, Node start, Node end, String type, FeatureMap features) {
#       return new AnnotationImpl(id, start, end, type, features)
#   }

#   """Test inDocumentOrder() and getStartingAt(long)"""
#   public void testDocumentOrder() throws Exception {
#     FeatureMap params = Factory.newFeatureMap()
#     params.put(Document.DOCUMENT_URL_PARAMETER_NAME, new URL(TestDocument.getTestServerName()+"tests/doc0.html"))
#     params.put(Document.DOCUMENT_MARKUP_AWARE_PARAMETER_NAME, "true")
#     Document doc = (Document)Factory.createResource("gate.corpora.DocumentImpl",
#                                                     params)

#     AnnotationSet originals = doc.getAnnotations("Original markups")
#     if(originals instanceof AnnotationSetImpl) {
#       AnnotationSetImpl origimpl = (AnnotationSetImpl)originals
#       List<Annotation> ordered = origimpl.inDocumentOrder()
#       assertNotNull(ordered)
#       assertEquals(20, ordered.size())
#       assertEquals(33, ordered.get(4).getStartNode().getOffset().intValue())
#       for(int i=1;i<ordered.size();i++) {
#         assertTrue("Elements "+(i-1)+"/"+i,
#             ordered.get(i-1).getStartNode().getOffset() <= ordered.get(i).getStartNode().getOffset())
#       }
#       AnnotationSet anns
#       anns = origimpl.getStartingAt(0)
#       assertEquals(4,anns.size())
#       anns = origimpl.getStartingAt(1)
#       assertEquals(0,anns.size())
#       anns = origimpl.getStartingAt(33)
#       assertEquals(4,anns.size())
#       anns = origimpl.getStartingAt(48)
#       assertEquals(1,anns.size())
#       anns = origimpl.getStartingAt(251)
#       assertEquals(1,anns.size())
#     }
#   } # testDocumentOrder()
  
  
  
if __name__ == '__main__':
    Gate.start()
    unittest.main()
    Gate.stop()