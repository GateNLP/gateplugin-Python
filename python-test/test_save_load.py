import unittest, gate, sys, os

class TestSaveLoad(unittest.TestCase):
	@classmethod
	def setUpClass(self):
		self.gate = gate.Gate()
		self.gate.start()
	@classmethod
	def tearDownClass(self):
		self.gate.stop()


	def test_load_txt(self):
		doc = self.gate.load("data/ishmael.txt")

		self.assertIsNotNone(doc.text)
		self.assertNotEqual(doc.text, "")

		self.assertEqual(len(doc.text.split()), 2215)

	def test_load_xml(self):
		doc = self.gate.load("data/ishmael.xml")

		self.assertIsNotNone(doc.text)
		self.assertNotEqual(doc.text, "")

		self.assertEqual(len(doc.text.split()), 2215)

	def test_save(self):
		doc = self.gate.load("data/ishmael.xml")

		try:
			os.remove("./data/ishmael_output.xml")
		except OSError:
			pass

		self.gate.save(doc, "data/ishmael_output.xml")

		doc = self.gate.load("data/ishmael_output.xml")

		self.assertIsNotNone(doc.text)
		self.assertNotEqual(doc.text, "")

		self.assertEqual(len(doc.text.split()), 2215)

	def test_annot_save(self):
		doc = self.gate.load("data/ishmael.xml")

		try:
			os.remove("./data/ishmael_output_annots.xml")
		except OSError:
			pass

		for token in doc.text.split():
			doc.annotationSets[""].add(token.source.begin, token.source.end, 
				"Token", {})

		self.gate.save(doc, "data/ishmael_output_annots.xml")

		doc = self.gate.load("data/ishmael_output_annots.xml")

		self.assertIsNotNone(doc.text)
		self.assertNotEqual(doc.text, "")

		self.assertEqual(len(doc.text.split()), 2215)
		self.assertEqual(len(doc.annotationSets[""].type("Token")), 2215)




if __name__ == '__main__':
    unittest.main()
