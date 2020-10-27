# Python installation

In order to run the Python plugin on your system it is necessary:

* to install Python version 3.6 or later, 3.7 or later recommended
* to install the minimum Python packages required 

The actual Python package used for integrating Python with 
Java GATE is included in the plugin in a minimal version. However, it is also possible to install this package separately and use it instead of the one contained with the plugin. See the section "Installing Python gatenlp" below

There are many different ways how python can be installed, and
the options different between operating systems, operating system flavours
and versions.

If you have a preferred way for how to install python, the Python plugin
will work if your installation satisfies the following conditions:

* Python version 3.6 or later (3.7 or later highly recommended!)
* the required package `sortedcontainers` is installed
* the python interpreter is on the PATH as "python", or you specify the  name or actual path to the interpreter via the `pythonBinary` or `pythonBinaryUrl` parameters

Otherwise, we recommend to install Python using the Anaconda or Miniconda distribution for your operating system.

The following describes the recommended steps to install the Miniconda distribution for Windows, MacOS and Linux. This mode of installation can  be performed (as of this writing,  October 2020) for Python versions 3.7 or later.

It is also possible to install versions of Python into a Conda environment which are older than the default Python version for that conda version.  

In order to use environments from the Python plugin, simply specify the  path the the "python" binary in the environment directory as the Python binary for the PR.

## Windows

* Install the 64 bit distribution for Python 3.6 or later (ideally 3.7 or later) from https://conda.io/miniconda.html
  * Download the file
  * Run it to start the installation process
  * At the prompt "Install for:" choose "Just Me"
  * Confirm the default installation location (probably "C:\Users\THEUSERNAME\Miniconda3") and
    take a note of that location!
  * IMPORTANT: On Windows, it is not always possible to later easily figure out where Python is installed,
    if you copy or take a note of the location shown by the installer you can later configure it as the `pythonBinaryUrl` in the PythonPr
  * In the "Advanced Options" screen, enable both checkboxes:
    * "Add Anaconda to my PATH": when you enable this, a warning will be shown,
      but it makes using Python from GATE much easier if enable this!
    * "Register Anaconda ..." is not necessary but probably useful to enable
* After the installation completes successfully you should find an entry "Anaconda3" in your Start menu
  which contains the entry "Anaconda Prompt"
* Click "Anaconda Prompt" to start a terminal window
* In the terminal window enter and run the following commands  (NOTE: a working internet connection is
  quired for this! The following steps will automatically download the required packages and all dependencies
  and install them into your Miniconda environment)
  * `pip install -U sortedcontainers`


## MacOS

* Install the 64 bit distribution for Python 3.6 or later (3.7 or later recommended) from https://conda.io/miniconda.html
  * Download the file
  * Start a terminal and ...
  * Change permissions to make it executable:
    `chmod 700 Miniconda3-latest-Linux-x86_64.sh`
  * Run the file:
    `./Miniconda3-latest-Linux-x86_64.sh`
  * Agree to the license by typing in "yes" and ENTER
  * Confirm or change the location where to install to and take a note of that path (e.g. /Users//username/miniconda3)
  * Agree to initialize Miniconda3 in the .bashrc file (this will add everything to your binary path)
  * Start a new terminal and check: `which python` should show the location of the python command inside the miniconda installation directory (e.g. /Users//username/miniconda3/bin/python)
* Start a new terminal
* In the terminal window enter and run the following commands (NOTE: a working internet connection is required for this! The following steps will automatically download the required packages and all dependencies and install them into your Miniconda environment)
  * `pip install -U sortedcontainers`

## Linux and Linux-like Operating Systems

* Install the 64 bit distribution for Python 3.6 or later (3.7 or later recommended) from https://conda.io/miniconda.html
  * Download the file
  * Start a terminal and ...
  * Change permissions to make it executable:
    `chmod 700 Miniconda3-latest-Linux-x86_64.sh`
  * Run the file:
    `./Miniconda3-latest-Linux-x86_64.sh`
  * Agree to the license by typing in "yes" and ENTER
  * Confirm or change the location where to install to and take a note of that path (e.g. /home/username/miniconda3)
  * Agree to initialize Miniconda3 in the .bashrc file (this will add everything to your binary path)
  * Start a new terminal and check: `which python` should show the location of the python command inside
    the miniconda installation directory (e.g. /home/username/miniconda3/bin/python)
* Start a new terminal
* In the terminal window enter and run the following commands    (NOTE: a working internet connection is required for this! The following steps will automatically download the required packages and all dependencies  and install them into your Miniconda environment)
  * `pip install -U sortedcontainers`

## Installing Python gatenlp

The Python package gatenlp is used to for the Python plugin but is also software which can be used completely on its own. The version that comes with the Python plugin only contains the basic components. 

In order to use a separately installed gatenlp package instead of the one that comes with the plugin: set the runtime-parameter `useOwnGatenlpPackage` of the GATE PythonPr resource to `false`.

For more information on the Python gatenlp package see:

* [The Main Documentation Page](https://gatenlp.github.io/python-gatenlp/)
* [Installation](https://gatenlp.github.io/python-gatenlp/installation.html)
* [The PyPi page](https://pypi.org/project/gatenlp/)
* [The GitHub Repository](https://github.com/GateNLP/python-gatenlp)
