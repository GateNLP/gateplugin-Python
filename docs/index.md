# GATE Python Plugin

This plugin provides a processing resource, `PythonPr` which allows the editing and running of python code for processing
GATE documents. The Python API for processing documents is the [Python `gatenlp` package](https://gatenlp.github.io/python-gatenlp).

The plugin provides its own copy of a specific version of the `gatenlp` package which is used by default, but it is possible to
instead use whatever version of the `gatenlp` package is installed on the system.

## Installing / Setting up Python

Before the plugin can be used Python must be installed:

* Python version 3.4 or later (3.5 or later recommended)  must be installed
* The python package [sortedcontainers](https://pypi.org/project/sortedcontainers/) must be installed
* [Detailed installation instructions](python-install.md)

## The PythonPr Processing Resource

The `PythonPr` processing resource can be used to run a Python program on documents.

When a pipeline that contains the `PythonPr` processing resource is run, the following main steps happens:

* The Python program is run in a separate process. The Python program must implement a function or class that
  uses the `@gatenlp.GateNlpPr` decorator and it must invoke the `gatenlp.interact()` function.
  (see examples below)
* The processing resource sends each document to the Python program
* The implemented `@GateNlpPr` function or the `execute` or `__call__` method of the implemented `@GateNlpPr` class is
  invoked and the document is passed to that function. The function can use the `gatenlp` API to modify the document.
  All the changes are recorded.
* The recorded changes are sent back to the `PythonPr` which applies the changes to the GATE document.

Here is a simple example Python program which splits the document into white-space separated tokens and creates
an annotation with the type "Token" in the default annotation set for each token. For each token annotation,
a feature "tokennr" is set to the sequence number of the token in the document.
It also sets the total number of tokens as a document feature:

```python
import re
from gatenlp import GateNlpPr, interact


@GateNlpPr
def run(doc, **kwargs):
    set1 = doc.get_annotations()
    set1.clear()
    text = doc.text
    whitespaces = [m for m in re.finditer(r"[\s,.!?]+|^[\s,.!?]*|[\s,.!?]*$", text)]
    for k in range(len(whitespaces) - 1):
        fromoff = whitespaces[k].end()
        tooff = whitespaces[k + 1].start()
        set1.add(fromoff, tooff, "Token", {"tokennr": k})
    doc.set_feature("nr_tokens", len(whitespaces) - 1)


interact()
```

The function gets the document passed (as its first argument) a `gatenlp.Document` and also gets all the
parameters defined in the `PythonPr` `programParams` parameter.

Instead of a function, a class can be implemented with the `@GateNlpPr` decorator.
The class must implement an `execute` or `__call__` method, but in addition can also
implement the `start`, `finish`, `reduce` and `result` methods. The following
example implements the same tokenizer as above in a class but also counts and prints out
the total number of tokens over all documents:


```python
import re
from gatenlp import GateNlpPr, interact, logger

@GateNlpPr
class MyProcessor:
    def __init__(self):
        self.tokens_total = 0

    def start(self, **kwargs):
        self.tokens_total = 0

    def finish(self, **kwargs):
        logger.info("Total number of tokens: {}".format(self.tokens_total))

    def __call__(self, doc, **kwargs):
        set1 = doc.get_annotations()
        set1.clear()
        text = doc.text
        whitespaces = [m for m in re.finditer(r"[\s,.!?]+|^[\s,.!?]*|[\s,.!?]*$", text)]
        nrtokens = len(whitespaces) - 1
        for k in range(nrtokens):
            fromoff = whitespaces[k].end()
            tooff = whitespaces[k + 1].start()
            set1.add(fromoff, tooff, "Token", {"tokennr": k})
        doc.set_feature("nr_tokens", nrtokens)
        self.tokens_total += nrtokens

interact()
```


### PythonPr Init Parameters

Parameters that have to get set when the processing resource is created:
* `pythonProgram` (ResourceReference, default: empty): this specifies the Python program to run. Since this is
  a ResourceReference, the selection dialog can be used to select a file from file storage, or a plugin resource.
  If a plugin resource is used, or the file selected file is read-only, the editor is disabled.
  Only file or creole/jar URLs are allowed. If a file URL is specified for a file that does not exist, it
  is created and filled with a python code template.

If file URL is specified and the file is writable, the file can be changed and edited within GATE by double clicking
on the processing resource. See [PythonFileEditor]

### PythonPr Runtime Parameters

* `loggingLevel` (drop down selection, default: INFO): choose the logging level to use in python. If DEBUG is used, then
  some additional information is also logged as info on the Java side.
* `programParams` (FeatureMap, default: empy): this can be used to pass on arbitrary parameters to the functions run on the
  Python side, via the `**kwargs` of the invoked method. Though this is a `FeatureMap`, the type of the key should be `String`
  and the type of each value should be something that can be serialized as JSON. In addition to the parameters specified here, the following
  default parameters will always get passed as well:
  * `gate_plugin_python_nrDuplicates`: the number of duplicates if multiprocessing is done
  * `gate_plugin_python_duplicateId` : the duplicate id (0 to nrDuplicates-1) of this PR.
  * `gate_plugin_python_workingDir` : the effective working directory used by the PR
  * `gate_plugin_python_pythonFile`: the effective Python program file used
* `pythonBinary` (String, default: "python"): the name of the command (the Python interpreter) to invoke from the PATH. On some systems, where
  the `python` command invokes Python version 2.x, the command `python3` can be used to invoke Python version 3.x.
* `pythonBinaryUrl` (URL, default: empty): If this is specified, it takes precedence over `pythonBinary`. This should be
  the URL of a file that should be invoked as the Python interpreter.
* `useOwnGatenlpPackage` (Boolean, default: true): to make sure results are reliable between systems, the Python plugin
  contains its own copy of the Python `gatenlp` package and uses it if this parameter is set to `true` (by putting the location
  of the package first on the `PYTHONPATH`). If this is `false` then nothing is put on the `PYTHONPATH` and whatever version of
  the `gatenlp` package is installed on the system is used.

NOTE: The document name is passed on to the Python code via the document feature `gate.plugin.python.docName`.

## Calculating and Returning Over-The-Corpus Results

TBD
