import unittest, sys, os, nltk
from gate.sourcedstring import SourcedString 

class TestSourcedString(unittest.TestCase):
	def testCreation(self):
		testString = SourcedString("This is a test string")
		self.assertIsNotNone(testString)
		self.assertIsNotNone(testString.sources)

		self.assertEqual(testString.sources[0].source_start, 0)
		self.assertEqual(testString.sources[0].source_end, len(testString))

	def testSlice(self):
		testString = SourcedString("This is a test string")

		subString = testString[5:10]

		source = subString.sources[0]
		self.assertEqual(source.source, testString)
		self.assertEqual(source.source_start, 5)
		self.assertEqual(source.source_end, 10)
		self.assertEqual(subString, "is a ")

	def testSliceOOB(self):
		testString = SourcedString("This is a test string")

		subString = testString[19:25]

		source = subString.sources[0]
		self.assertEqual(source.source, testString)
		self.assertEqual(source.source_start, 19)
		self.assertEqual(source.source_end, 21)
		self.assertEqual(subString, "ng")

		subString = testString[-3:25]

		source = subString.sources[0]
		self.assertEqual(source.source, testString)
		self.assertEqual(source.source_start, 18)
		self.assertEqual(source.source_end, 21)
		self.assertEqual(subString, "ing")

	def testSliceNegative(self):
		testString = SourcedString("This is a test string")

		subString = testString[-6:-3]

		source = subString.sources[0]
		self.assertEqual(source.source, testString)
		self.assertEqual(source.source_start, 15)
		self.assertEqual(source.source_end, 18)
		self.assertEqual(subString, "str")

	def testSliceEmpty(self):
		testString = SourcedString("This is a test string")

		subString = testString[-2:3]

		self.assertEqual(subString.sources, [])

	def testSplit(self):
		testString = SourcedString("This is a test string")

		splitString = testString.split(" ")
		self.assertEqual(splitString, ["This", "is", "a", "test", "string"])

		self.assertEqual(splitString[0].sources[0].source_start, 0)
		self.assertEqual(splitString[0].sources[0].source_end, 4)

		self.assertEqual(splitString[1].sources[0].source_start, 5)
		self.assertEqual(splitString[1].sources[0].source_end, 7)

	def testSplitRetainsSource(self):
		testString = SourcedString("This is a test string")

		splitString = testString.split(" ")

		for subString in splitString:
			self.assertEqual(subString.sources[0].source, testString)


	def testJoinTwoSourcedString(self):
		a = SourcedString("This is a ")
		b =  SourcedString("test string")

		testString = a + b

		self.assertEqual(testString, "This is a test string")
		self.assertEqual(len(testString.sources), 2)

		self.assertEqual(testString.sources[0].start, 0)
		self.assertEqual(testString.sources[0].end, 10)

		self.assertEqual(testString.sources[0].source_start, 0)
		self.assertEqual(testString.sources[0].source_end, 10)

		self.assertEqual(testString.sources[1].start, 10)
		self.assertEqual(testString.sources[1].end, 21)

		self.assertEqual(testString.sources[1].source_start, 0)
		self.assertEqual(testString.sources[1].source_end, 11)

	def testJoinDeepSourcedString(self):
		a = SourcedString("This is a ")
		b = SourcedString("test string")
		c = SourcedString("yes")

		testString = (a + b) + c

		self.assertEqual(testString, "This is a test stringyes")
		self.assertEqual(len(testString.sources), 3)

		self.assertEqual(testString.sources[0].start, 0)
		self.assertEqual(testString.sources[0].end, 10)

		self.assertEqual(testString.sources[0].source_start, 0)
		self.assertEqual(testString.sources[0].source_end, 10)

		self.assertEqual(testString.sources[1].start, 10)
		self.assertEqual(testString.sources[1].end, 21)

		self.assertEqual(testString.sources[1].source_start, 0)
		self.assertEqual(testString.sources[1].source_end, 11)

		self.assertEqual(testString.sources[2].start, 21)
		self.assertEqual(testString.sources[2].end, 24)

		self.assertEqual(testString.sources[2].source_start, 0)
		self.assertEqual(testString.sources[2].source_end, 3)

	def testJoinSlicedSourcesString(self):
		a = SourcedString("This is a ")
		b = SourcedString("test string")[:4]

		testString = a + b

		self.assertEqual(testString, "This is a test")

		self.assertEqual(testString.sources[0].start, 0)
		self.assertEqual(testString.sources[0].end, 10)

		self.assertEqual(testString.sources[0].source_start, 0)
		self.assertEqual(testString.sources[0].source_end, 10)

		self.assertEqual(testString.sources[1].start, 10)
		self.assertEqual(testString.sources[1].end, 14)

		self.assertEqual(testString.sources[1].source_start, 0)
		self.assertEqual(testString.sources[1].source_end, 4)


	def testJoinSourceWithPlainString(self):
		a = SourcedString("This is a ")
		b =  "test string"

		testString = a + b

		self.assertEqual(testString, "This is a test string")
		self.assertEqual(len(testString.sources), 2)

		self.assertEqual(testString.sources[0].start, 0)
		self.assertEqual(testString.sources[0].end, 10)

		self.assertEqual(testString.sources[0].source_start, 0)
		self.assertEqual(testString.sources[0].source_end, 10)

		self.assertEqual(testString.sources[1].start, 10)
		self.assertEqual(testString.sources[1].end, 21)

		self.assertEqual(testString.sources[1].source_start, 0)
		self.assertEqual(testString.sources[1].source_end, 11)

	def testNLTK(self):
		source = SourcedString("This is a string, which I am using to test")
		tags = nltk.pos_tag(source.split(" "))
		sources = source.split(" ")
		self.assertEqual(tags[0][0].sources[0].source_start, 0)
		self.assertEqual(tags[0][0].sources[0].source_end, 4)
		self.assertEqual(tags[1][0].sources[0].source_start, 5)
		self.assertEqual(tags[1][0].sources[0].source_end, 7)

		
if __name__ == '__main__':
    unittest.main()
