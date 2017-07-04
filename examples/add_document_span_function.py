"""Simple example PR to print the document text"""
from gate import executable
import sys, codecs

@executable
def addDocumentSpan(document, outputAS):
	document.annotationSets[outputAS].add(0, len(document.text), "DocumentSpan",
	    {"text": document.text})

	return document

if __name__ == "__main__":
	addDocumentSpan.start()

