# GATE Python Plugin

This plugin provides a processing resource, `PythonPr` which allows the editing and running of python code for processing
GATE documents. The Python API for processing documents is the [Python `gatenlp` package](https://gatenlp.github.io/python-gatenlp).

The plugin provides its own copy of a specific version of the `gatenlp` package which is used by default, but it is possible to
instead use whatever version of the `gatenlp` package is installed on the system.

## Using the Python Plugin

* Requires GATE 8.6.1 or later
* In the Plugin manager, click the "+" button then enter the following Maven coordinates
  * uk.ac.gate.plugins
  * python
  * 2.4-SNAPSHOT

## Installing / Setting up Python

Before the plugin can be used Python must be installed:

* Python version 3.6 or later (3.7 or later recommended)  must be installed
* The python package [sortedcontainers](https://pypi.org/project/sortedcontainers/) must be installed
* [Detailed installation instructions](python-install.md)

The plugin should be compatible with GATE 8.5 or later and
should run on Windows, MacOS and Linux-like operating systems.

## Reporting problems

If you encounter problems please:

* first check if your problem is described in the [Frequent Problems](frequent-problems) list
* check github [issue tracker](https://github.com/GateNLP/gateplugin-Python/issues)
  * also check if the problem is in the [list of closed issues](https://github.com/GateNLP/gateplugin-Python/issues?q=is%3Aissue+is%3Aclosed)
  * please give as much detail as possible about your OS, GATE version, plugin version, Java version etc.
  * please use the issue tracker only to report bugs, other problems and feature requests, for questions about how to use the plugin or other general questions use the GATE mailing list:
* for more general questions the [GATE mailing list](https://groups.io/g/gate-users/topics)
  * please mention "Python plugin" in the subject
  * please give as much detail as possible about your OS, GATE version, plugin version, Java version etc.

Please give as much details as possible about your operating system,
GATE version, Java version, Python version and whatever else might be relevant.

## Plugin components and help topics

Main help topics:

* [Detailed installation instructions](python-install.md)
* [PythonPr](PythonPr): Processing Resource to process documents with Python, using the [gatenlp](https://gatenlp.github.io/python-gatenlp/) package.
* [Pipeline python-spacy](pipeline-python-spacy): a ready made application that creates  annotations for a document from the result of running Python   [spaCy](https://spacy.io/) on the text.
* [Pipeline python-stanford-stanza](pipeline-python-stanford-stanza): a ready made application that runs Stanford Stanza and creates annotations from the result.
* [Python Editor](python-editor.md)
* [Command Line Debugging of Python Scripts](debug-scripts): how to debug the scripts   outside of GATE from the command line
* [Frequent Problems](frequent-problems): if you encounter problems, please check here first!

Other help topics: 

* [PythonPrResult](PythonPrResult): Language Resource to store over-the-corpus  processing results as features
* [Multiprocessing](multiprocessing): Running the `PythonPr` processing in  parallel and combining over-the-corpus results from all processes.
* [PythonSlaveLr](PythonSlaveLr): Language Resource that allows `gatenlp` to  control GATE from Python and call the GATE API from Python.
