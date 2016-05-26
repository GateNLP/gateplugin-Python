import unittest, gate, sys, os

class TestUnicode(unittest.TestCase):
	@classmethod
	def setUpClass(self):
		self.gate = gate.Gate()
		self.gate.start()
		self.doc = self.gate.load("data/unicode.xml")
	@classmethod
	def tearDownClass(self):
		self.gate.stop()


	def test_load(self):
		self.assertIsNotNone(self.doc.text)
		self.assertNotEqual(self.doc.text, "")
		self.assertEqual(len(self.doc.text), 51)

	def test_emoji(self):
		emoji = self.doc.annotationSets[""].type("EMOJI").first()
		self.assertEqual(u"\U0001f64f\U0001f3fb", self.doc.text[emoji.start: emoji.end])

	def test_domino(self):
		emoji = self.doc.annotationSets[""].type("DOMINO").first()
		self.assertEqual(u"\U0001f053", self.doc.text[emoji.start: emoji.end])

	def test_unicode(self):
		emoji = self.doc.annotationSets[""].type("UNICODE").first()
		self.assertEqual(u"unicode", self.doc.text[emoji.start: emoji.end])

	def test_m(self):
		emoji = self.doc.annotationSets[""].type("M").first()
		self.assertEqual(u"\u2133", self.doc.text[emoji.start: emoji.end])

if __name__ == '__main__':
    unittest.main()