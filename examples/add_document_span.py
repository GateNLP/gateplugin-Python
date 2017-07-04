"""Simple example PR to print the document text"""

from gate import iterate

for document in iterate():
	document.annotationSets["python"].add(0, len(document.text), "DocumentSpan",
	    {"text": document.text})

