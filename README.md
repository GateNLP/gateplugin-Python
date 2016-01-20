# Python compatibility for GATE

The aim of this project is to allow the writing of GATE processing resources in Python. This is achieved using interprocess communication rather than running Python within the JVM, so it is possible to use popular Python research software such as Gensim and NLTK within GATE.

The compatibliity layer consists of both a Python PR for GATE which can be included in applications, and a Python library containing objects with methods that read and modify documents in a way that closely resembles the GATE embedded API.

## The GATE PythonPR

This processing resource will be provided as part of a plugin. The PythonPR is configured with the name of a script to run and a Python binary. The script is launched once and then kept running until GATE is exited or a problem occurs, allowing for large resources to be loaded in the Python script then reused for multiple documents.

The PythonPR transmits GATE documents in JSON format to the client, and waits for a response, also in JSON, consisting of a series of commands to change the document.

## The python GATE library

This library consists of code to convert the JSON formatted document into a representation similar to that used within GATE itself. It allows for the modification of annotation sets and features, which will then be reflected in GATE. The following is a simple example:

```python
for token in self.document.text.split(" "):
	self.document.annotationSets["PYTHON_TEST"].add(token.source.begin, 
		token.source.end, 
		"Token", 
		{"string":token})
```

## Usage

This plugin is very preliminary, so heavy usage is not yet recommended. However, to install the library, first clone this repository:

> git clone https://github.com/GateNLP/gateplugin-python.git

And compile the code, making sure that $GATEHOME is set to the location of your GATE installation.

> ant

Add the plugin within GATE (Creole Plugin Manager then click '+' then select the directory of the plugin)

Create a new PythonPR and add it to an application, configure the location of your Python executable and the location of a new script.

The script should use the following template:

```python
from gate import ProcessingResource

class TemplatePR(ProcessingResource):
	def init(self): 
		pass

	def execute(self):
		pass

if __name__ == "__main__":
	pr = TemplatePR()
	pr.start()
```
