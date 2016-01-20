from gate import ProcessingResource
import twokenize, sys, codecs

class ExamplePR(ProcessingResource):
	def init(self): 
		pass
	def execute(self):
		self.document.text.split(" ")
		for token in self.document.annotationSets[""].getType("Token"):
			print >> sys.stderr, repr(self.document.text[token.start:token.end])

		for token in self.document.text.split(" "):
			print >> sys.stderr, repr(token)
			self.document.annotationSets["PYTHON_TEST"].add(token.source.begin, 
				token.source.end, 
				"Token", 
				{"string":token})

if __name__ == "__main__":
	pr = ExamplePR()
	pr.start()