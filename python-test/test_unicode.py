import unittest, gate, sys, os

class TestUnicode(unittest.TestCase):
	@classmethod
	def setUpClass(self):
		self.gate = gate.Gate()
		self.gate.start()
	@classmethod
	def tearDownClass(self):
		self.gate.stop()


	def test_load(self):
		doc = self.gate.load("data/unicode.xml")

		self.assertIsNotNone(doc.text)
		self.assertNotEqual(doc.text, "")
		self.assertEqual(len(doc.text), 51)

	def test_emoji(self):
		doc = self.gate.load("data/unicode.xml")

		emoji = doc.annotationSets[""].type("EMOJI").first()
		self.assertEqual(u"\U0001f64f\U0001f3fb", doc.text[emoji.start: emoji.end])

if __name__ == '__main__':
    unittest.main()