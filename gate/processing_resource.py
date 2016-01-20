from document import Document
from corpus import Corpus
import sys, json, codecs

class ProcessingResource(object):
	def __init__(self):
		self.logger = []
		self.scriptParams = dict()
		self.inputAS = None
		self.outputAS = None
		self.document = None
		self.input_line = ""

	def start(self):
		self.init()

		line = sys.stdin.readline().strip()
		while line:
			line = codecs.decode(line, "utf8")

			if line:
				self.input_line = line
				input_json = json.loads(line)

				if "command" in input_json:
					if input_json["command"] == "BEGIN_EXECUTION":
						self.corpus = Corpus(input_json)
						self.beginExecution()
					elif input_json["command"] == "ABORT_EXECUTION":
						self.abortExecution()
					elif input_json["command"] == "END_EXECUTION":
						self.endExecution()
				else:
					self.logger, self.document = Document.load(input_json)
					self.scriptParams = input_json["scriptParams"]
					self.inputAS  = self.document.annotationSets[input_json["inputAS"]]
					self.outputAS = self.document.annotationSets[input_json["outputAS"]]

					self.execute()

					print json.dumps(self.logger)
					sys.stdout.flush()

			line = sys.stdin.readline().strip()

	def init(self):
		pass

	def execute(self):
		raise NotImplementedError("No execute method for pipeline")

	def beginExecution(self):
		pass

	def endExecution(self):
		pass

	def abortExecution(self):
		pass