import unittest, gate, sys, os

class TestLoadAwkwardBoundaries(unittest.TestCase):
	def setUp(self):
		self.gate = gate.Gate()
		self.gate.start()

	def tearDown(self):
		self.gate.stop()

	def test_load(self):
		doc = self.gate.load("data/boundaries_broken.xml")

		self.assertIsNotNone(doc.text)
		self.assertNotEqual(doc.text, "")

	def test_tokens(self):
		doc = self.gate.load("data/boundaries_broken.xml")

		for token in doc.annotationSets[""].type("Token"):
			self.assertEqual(token.features["string"], doc.text[token.start: token.end].strip("@"))

if __name__ == '__main__':
    unittest.main()