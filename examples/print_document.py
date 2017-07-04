"""Simple example PR to print the document text"""

from gate import iterate

import sys
for document in iterate():
	print >> sys.stderr, document.text

