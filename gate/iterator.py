from __future__ import print_function

import codecs
import json
import sys

from .document import Document
from .corpus import Corpus
from .gate_exceptions import InvalidOffsetException

"""
Create an interface by which you can listen for
GATE documents with an iterator when your program starts,
and have the iterator quit
when GATE gets the appropriate signal to say the pipeline has ended.
"""

# For compatibility in Python 2 and 3
try:
	STDIN = sys.stdin.buffer
except AttributeError:
	# Probably Python 2
	STDIN = sys.stdin


class GateIterator(object):
	def __init__(self):
		self.scriptParams = {}

	def params(self):
		"""Waits for parameters to come down the line and returns them. May block on the client."""
		while True:
			line = STDIN.readline().strip()
			if not line:
				return
			line = codecs.decode(line, "utf8")

			input_json = json.loads(line)

			if "command" in input_json:
				if input_json["command"] == "BEGIN_EXECUTION":
					corpus = Corpus(input_json)
					self.scriptParams = input_json["parameterMap"]
					return self.scriptParams
				elif input_json["command"] == "ABORT_EXECUTION":
					return
				elif input_json["command"] == "END_EXECUTION":
					return


	def __iter__(self):
		while True:
			line = STDIN.readline().strip()
			if not line:
				return
			line = codecs.decode(line, "utf8")

			input_json = json.loads(line)
			if "command" in input_json:
				if input_json["command"] == "BEGIN_EXECUTION":
					corpus = Corpus(input_json)
					self.scriptParams = input_json["parameterMap"]
				elif input_json["command"] == "ABORT_EXECUTION":
					return
				elif input_json["command"] == "END_EXECUTION":
					return
			else:
				try:
					document = Document.load(input_json)

					yield document
					print(json.dumps(document.logger))
				except InvalidOffsetException as e:
					print("InvalidOffsetException prevented reading a document " + e.message, file=sys.stderr)
					print(json.dumps([]))
				sys.stdout.flush()


def iterate():
	"""I can't inherit any of this from ProcessingResource because
		we need to completely change the flow to support iteration"""
	iterator = GateIterator()
	return iterator	
