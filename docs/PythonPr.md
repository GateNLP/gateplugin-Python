## The PythonPr Processing Resource

The `PythonPr` processing resource can be used to run a Python program on documents.

When a pipeline that contains the `PythonPr` processing resource is run, the following main steps happens:

* The Python program is run in a separate process. The Python program must:
  * implement a function or callable class that uses the `@gatenlp.GateNlpPr` decorator 
  * invoke the `gatenlp.interact()` function  (see examples below)
* The processing resource sends each document to the Python program
* The implemented `@GateNlpPr` function or the `__call__` method of the implemented `@GateNlpPr` class is
  invoked and the document is passed to that function. The function can use the `gatenlp` API to modify the document.
  All the changes are recorded.
* The recorded changes are sent back to the `PythonPr` which applies the changes to the GATE document.

Here is a simple example Python program which splits the document into white-space separated tokens using a simple regular expression and creates
an annotation with the type "Token" in the default annotation set for each token. For each token annotation,
a feature "tokennr" is set to the sequence number of the token in the document.
It also sets the total number of tokens as a document feature.

This example implements the code to run for each document as a function with the name `run` which must
take the document to process as a parameter and allow arbitrary additional kwargs. 

To actually invoke the function for each document the `interact()` function has to get invoked at the 
end of the Python script!

```python
import re
from gatenlp import GateNlpPr, interact


@GateNlpPr
def run(doc, **kwargs):
    set1 = doc.annset()
    set1.clear()
    text = doc.text
    whitespaces = [m for m in re.finditer(r"[\s,.!?]+|^[\s,.!?]*|[\s,.!?]*$", text)]
    for k in range(len(whitespaces) - 1):
        fromoff = whitespaces[k].end()
        tooff = whitespaces[k + 1].start()
        set1.add(fromoff, tooff, "Token", {"tokennr": k})
    doc.feature["nr_tokens"] = len(whitespaces) - 1


interact()
```

The function gets the document passed (as its first argument) a `gatenlp.Document` and also gets all the
parameters defined in the `PythonPr` `programParams` parameter as kwargs plus the `_config_file` parameter
as additional kwarg if it was set in the PR. Not that if the function does not have `**kwargs` then 
it gets called without any keyword arguments. 

Instead of a function, a callable class can be implemented with the `@GateNlpPr` decorator.

The class must implement the `__call__` method, but in addition can also
implement the `start`, `finish`, `reduce` and `result` methods. The following
example implements the same tokenizer as above in a class but also counts and prints out
the total number of tokens over all documents. Again the `interact()` call must be 
placed at the end of the Python script.


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

    def __call__(self, doc):
        set1 = doc.annset()
        set1.clear()
        text = doc.text
        whitespaces = [m for m in re.finditer(r"[\s,.!?]+|^[\s,.!?]*|[\s,.!?]*$", text)]
        nrtokens = len(whitespaces) - 1
        for k in range(nrtokens):
            fromoff = whitespaces[k].end()
            tooff = whitespaces[k + 1].start()
            set1.add(fromoff, tooff, "Token", {"tokennr": k})
        doc.features["nr_tokens"] = nrtokens
        self.tokens_total += nrtokens

interact()
```

Advantages of using a callable class:

* the `start(self, **kwargs)` method, when implemented, gets invoked when processing of a corpus is started
  in Java GATE. The `**kwargs` are taken from the PythonPR `programParams` runtime parameter. 
  This allows to parametrize and initialize the class depending on the `programParams` settings, and to 
  initialize data to update or use during the processing of a whole corpus.
* the `__call__(self, doc, **kwargs)` method is invoked for each document and gets the same kwargs as the start method. 
  If the method is defined as `__call__(self, doc)` (no kwargs) then no kwargs are passed to the method. 
* the `finish(self, **kwargs)` method is invoked when processing the corpus ends or is aborted, the same kwargs are passed as to the 
  `start` method. This can be used to calculate final over-the-corpus results after the corpus has been processed. 
  If this method returns a dictionary, the key/value pairs of the dictionary are stored either in a PythonResult object or 
  as the features for the PythonPr used. 
* it is also possible to implement a `reduce(self, resultList)` method. This method is invoked if multiprocessing duplicates
  are used and is used to combine the partial results from each process into one overal resalt. 
  See [multiprocessing](multiprocessing).


### PythonPr Init Parameters

Parameters that have to get set when the processing resource is created:
* `pythonProgram` (ResourceReference, default: empty): this specifies the Python program, i.e. the Python source code, to run. 
  Since this is
  a ResourceReference, the selection dialog can be used to select a file from file storage, or a plugin resource.
  If a plugin resource is used, or the file selected file is read-only, the editor is disabled.
  Only file or creole/jar URLs are allowed. If a file URL is specified for a file that does not exist, it
  is created and filled with a python code template.
  NOTE: in order to specify a file URL, either first click the button to open the file selection dialog and enter
  the name of the file there, or enter a full absolute `file:///` URL in the input field for this parameter? 
  If a name is entered without the `file:` scheme a `creole:` scheme is assumed which will look for a file 
  in the plugin, not on the file system. 

If file URL is specified and the file is writable, the file can be changed and edited within GATE by double clicking
the processing resource in the GUI. See [Python Editor](python-editor)

### PythonPr Runtime Parameters

* `loggingLevel` (drop down selection, default: INFO): choose the logging level to use in python. If DEBUG is used, then
  some additional information is also logged as info on the Java side.
* `outputResultResource` A ResultLr to store corpus processing results in. See [PythonPrResult](PythonPrResult)
* `configFile` (URL, default: empty): if this is set to some File, the absolute path to the file gets passed to the python 
  `start(self, **kwargs)` method as kwarg `_config_file`. If the `configFile` parameter is not set, the kwarg
  is not set either.
* `programParams` (FeatureMap, default: empy): this can be used to pass on arbitrary parameters to the functions run on the
  Python side, via the `**kwargs` of the invoked method. Though this is a `FeatureMap`, the type of the key should be `String`
  and the type of each value should be something that can be serialized as JSON. In addition to the parameters specified here, the following
  default parameters will always get passed as well:
  * `_nrDuplicates`: the number of duplicates if multiprocessing is done
  * `_duplicateId` : the duplicate id (0 to nrDuplicates-1) of this PR.
  * `_workingDir` : the effective working directory used by the PR
  * `_pythonFile`: the effective Python program file used if program was loaded from a file
  * `_pythonPath` and `_pythonModule`: path and module in Jar, if the program was loaded from a JAR
* `pythonBinary` (String, default: "python"): the name of the command (the Python interpreter) to invoke from the PATH. On some systems, where
  the `python` command invokes Python version 2.x, the command `python3` can be used to invoke Python version 3.x.
* `pythonBinaryUrl` (URL, default: empty): If this is specified, it takes precedence over `pythonBinary`. This should be
  the URL of a file that should be invoked as the Python interpreter.
* `setsToUse`(Set, default: `[*]`): the names of annotation sets to include with the document when it is passed on to the Python
  program. If any of the names is `*`, all sets are passed on. In order to specify the default annotation set, use null or 
  a String of only spaces. If no names are specified, no annotations are passed on. NOTE: if a set is excluded but the Python program
  adds annotations to a set with that name, the annotation ids will still be assinged so they are higher than the highest id in the 
  existing set in the GATE document. In order to limit the sets to use, make sure the default `*` entry is first removed!
  NOTE: leading and trailing spaces are removed from all names, and even if the dialog shows 
  different names with leading or trailing spaces for the same set, internally only one cleaned name is used.
* `useOwnGatenlpPackage` (Boolean, default: true): to make sure results are reliable between systems, the Python plugin
  contains its own copy of the Python `gatenlp` package and uses it if this parameter is set to `true` (by putting the location
  of the package first on the `PYTHONPATH`). If this is `false` then nothing is put on the `PYTHONPATH` and whatever version of
  the `gatenlp` package is installed on the system is used.

