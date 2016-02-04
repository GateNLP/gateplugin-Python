"""Utilities to work with a GATE instance from within Python"""
import os, json, sys
from document import Document
from contextlib import contextmanager
from subprocess import Popen, PIPE

class Gate(object):
	def __init__(self, jarLocation = None):
		self.jarLocation = jarLocation
		if self.jarLocation == None:
			self.jarLocation = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
			self.jarLocation = os.path.join(self.jarLocation, "gateplugin-python.jar")
			
	def start(self):
		self.gateProcess = Popen(["java", "-cp",
			os.environ['CLASSPATH']+":"+self.jarLocation, 
			"gate.python.PythonGATEInstance"], stdout=PIPE, stdin=PIPE)

	def stop(self):
		self.gateProcess.terminate()

	def __enter__(self):
		self.start()

	def __exit__(self, type, value, traceback):
		self.stop()

	def load(self, document):
		return self.loadURL("file:///"+os.path.abspath(document))

	def loadURL(self, document):
		command = {
			"command": "LOAD_DOCUMENT", 
			"targetURL": document
		}

		print >> self.gateProcess.stdin, json.dumps(command)

		return Document.load(json.loads(self.gateProcess.stdout.readline()), src=document)

	def save(self, document, output):
		return self.saveURL(document, "file:///"+os.path.abspath(output))

	def saveURL(self, document, output):
		command = {
			"command": "SAVE_DOCUMENT", 
			"targetURL": document.src,
			"outputURL": output, 
			"documentCommands": document.logger
		}

		print >> self.gateProcess.stdin, json.dumps(command)

		response = self.gateProcess.stdout.readline()
		try:
			return Document.load(json.loads(response), src=output)
		except ValueError:
			print >> sys.stderr, response
