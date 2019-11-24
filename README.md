# GATE Python Plugin

DOCUMENTATION: http://gatenlp.github.io/gateplugin-python/

This plugin provides a processing resource, `PythonPr` which allows the editing and running of python code for processing 
GATE documents. The Python API for processing documents is the Python `gatenlp` package, see  https://gatenlp.github.io/python-gatenlp/

The plugin provides its own copy of a specific version of the `gatenlp` package which is used by default, but it is possible to 
instead use whatever version of the `gatenlp` package is installed on the system.

## Preparing Python

The plugin needs Python 3.6 or later to be installed on the system and the following packages to be installed:
* numpy, version 1.15.4 or higher
* loguru, version 0.2.5 or higher
* sortedcontainers, 2.1.0 or higher

## The `PythonPr` Processing Resource



