"""Utilities to work with a GATE instance from within Python"""
import os, json, sys, urllib
from document import Document
from contextlib import contextmanager
from subprocess import Popen, PIPE

class Gate(object):
	def __init__(self, jarLocation = None):
		self.jarLocation = jarLocation
		if self.jarLocation == None:
			self.jarLocation = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
			
	def start(self):
		command = ["java", "-cp",
			os.environ['CLASSPATH']+":"+self.jarLocation+"/gateplugin-python.jar" +
			":"+self.jarLocation+"/lib/*:", 
			"gate.python.PythonGATEInstance"]
		self.gateProcess = Popen(command, stdout=PIPE, stdin=PIPE)

	def stop(self):
		self.gateProcess.terminate()

	def __enter__(self):
		self.start()
		return self

	def __exit__(self, type, value, traceback):
		self.stop()

	def readResponse(self):
		response = self.gateProcess.stdout.readline().strip()

		while not response:
			response = self.gateProcess.stdout.readline().strip()

		try:
			return json.loads(response)
		except ValueError:
			print >> sys.stderr, response
			raise Exception(response)

	def load(self, document):
		return self.loadURL("file:///"+urllib.quote(os.path.abspath(document)))

	def loadURL(self, document):
		command = {
			"command": "LOAD_DOCUMENT", 
			"targetURL": document
		}
		print >> self.gateProcess.stdin, json.dumps(command)

		return Document.load(self.readResponse(), src=document)


	def save(self, document, output):
		return self.saveURL(document, "file:///"+urllib.quote(os.path.abspath(output)))

	def saveURL(self, document, output):
		command = {
			"command": "SAVE_DOCUMENT", 
			"targetURL": document.src,
			"outputURL": output, 
			"documentCommands": document.logger
		}

		print >> self.gateProcess.stdin, json.dumps(command)

		return Document.load(self.readResponse(), src=output)
