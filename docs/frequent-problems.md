# List of Frequent or Likely Problems


### Exception "sortedcontainers cannot be imported!"

When running a pipeline you get an exception saying "sortedcontainers cannot be imported!"

Solution: 
* see the [installation instructions](python-install)
* the "sortedcontainers" package must be installed into the Python you use
  * Open a terminal window and install the package using `pip install -U sortedcontainers`
* Make sure the Python and Python environment used for the Python plugin is the same as the one you set up 
  * In a terminal enter `where python` (on Windows) or `which python` (MacOs/Linux): check if this is the python you want to use
  * (Windows)  Alternately do the same in an Anaconda Python terminal window
  * If in doubt, use the full path to the Python binary for the `pythonBinary` runtime parameter of the PythonPr processing recource

* For using a specific Anaconda environment, make sure to first create and activate the environment before installing the necessary packages for the Python plugin. 
* After this, find the path to the Python binary for the environment by activating the environment and then running `where python` (on Windows) or `which python` (on MacOS/Linux)


### Exception `No module XY` or `XY cannot be imported`

When running a pipeline you get an exception saying "No module XY" where XY is the name of a package, e.g. "spacy" or a similar message about a module that cannot be imported.

Solution:
* Some parts of the Python `gatenlp` package which is used for running Python code in the Python plugin require additional packages to be installed. Install them into the Python or Python environment you are using (similar to the previous problem)
* You can also use a separate installation of the gatenlp package from the Python plugin by setting the PythonPr runtime parameter `usePluginGatenlpPackage` to false. 
  * See https://gatenlp.github.io/python-gatenlp/installation.html for how to install the gatenlp package in Python
  
