# GATE Python Plugin

This plugin provides a processing resource, `PythonPr` which allows the editing and running of python code for processing
GATE documents. The Python API for processing documents is the Python `gatenlp` package, see  https://gatenlp.github.io/python-gat

The plugin provides its own copy of a specific version of the `gatenlp` package which is used by default, but it is possible to
instead use whatever version of the `gatenlp` package is installed on the system.

## Installing / Setting up Python

Before the plugin can be used Python must be installed:

* Python version 3.6 or later must be installed
* The python package sortedcontainers must be installed

## The PythonPr Processing Resource

The `PythonPr` processing resource can be used to run a Python program on documents.

When a pipeline that contains the `PythonPr` processing resource is run, the following main steps happens:

* The python program is run in a separate process. The python program must implement a function or class that 
  uses the gatenlp.@GateNlpPr decorator and it must invoke the gatenlp.interact() function.
  (see examples below)
* The processing resource sends each document to the python program
* the implemented `@GateNlpPr` function or the `execute` or `__call__` method of the implemented `@GateNlpPr` class is 
  invoked and the document is passed to that function. The function can use the `gatenlp` API to modify the document.
  All the changes are recorded.
* The recorded changes are sent back to the `PythonPr` which applies the changes to the GATE document

Here is a simple example Python program which splits the document into white-space separated tokens and creates
an annotation with the type "Token" in the default annotation set for each token. For each token annotation,
a feature "tokennr" is set to the sequence number of the token in the document. 
It also sets the total number of tokens as a document feature:

```python
import re
from gatenlp import @GateNlpPr, interact

@GateNlpPr
def run(doc, **kwargs):
    set1 = doc.get_annotations() 
    set1.clear()  
    text = doc.text  
    whitespaces = [m for m in re.finditer("[\s,.!?]+|^[\s,.!?]*|[\s,.!?]*$",text)]
    for k in range(len(whitespaces)-1):  
        fromoff=whitespaces[k].end() 
        tooff=whitespaces[k+1].start() 
        set1.add(fromoff, tooff, "Token", {"tokennr": k})
    doc.set_feature("nr_tokens", len(whitespaces)-1)

interact()
```

The function gets the document passed as a `gatenlp.Document` and also gets all the 
parameters defined in the `PythonPr` `programParams` parameter. 

### PythonPr Init Parameters

Parameters that have to get set when the processing resource is created:
* `pythonProgram` (ResourceReference, default: empty): this specifies the Python program to run. Since this is 
  a ResourceReference, the selection dialog can be used to select a file from file storage, a plugin resource
  or any other known URL. If something is specified that is not a local file, it is copied to the working directory
  and that copy is used.
* `pythonProgramPath` (String, default: empty): if this is specified it takes precedence over the `pythonProgram` parameter.
  This can be used to specify an absolute or relative local file path. If the path is relative it is resolved against the 
  working directory.
* `workingDirUrl` (URL, default: empty): this can be used to specify a working directory. The working directory is used 
  as the current directory when running the Python process and is also used for creating a copy of the python program, if the 
  python program is not on the local file system. If this is left empty, the current directory of the running Java process is used.

If neither `pythonProgram` nor `pythonProgramPath` is specified, a file with the name `tmpfile.py` is created in the 
working directory and initialized with an initial code template. 
If a file path is specified that does not exist, it is created and initialized with the initial code template.


### PythonPr Runtime Parameters

* `debugMode` (Boolean, default: false): if set to `true` more information about what the PR does is provided in the message pane
* `programParams` (FeatureMap, default: empy): this can be used to pass on arbitrary parameters to the functions run on the 
  Python side, via the `**kwargs` of the invoked method. Though this is a FeatureMap, the type of the key should be String
  and the type of each value should be something that can be serialized as JSON.
* `pythonBinary` (String, default: "python"): the name of the command (the Python interpreter) to invoke from the PATH. On some systems, where 
  the `python` command invokes Python version 2.x, the command `python3` can be used to invoke Python version 3.x.
* `pythonBinaryUrl` (URL, default: empty): If this is specified, it takes precedence over `pythonBinary`. This should be
  the URL of a file that should be invoked as the Python interpreter. 
* `useOwnGatenlpPackage` (Boolean, default: true): to make sure results are reliable between systems, the Python plugin 
  contains its own copy of the Python `gatenlp` package and uses it if this parameter is set to true (By putting the location
  of the package first on the PYTHONPATH). If this is `false` then nothing is put on the PYTHONPATH and whatever version of 
  the `gatenlp` package is installed on the system is used. 


