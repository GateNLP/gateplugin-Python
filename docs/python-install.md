# Python installation

There are many different ways how python can be installed, and
the options different between operating systems, operating system flavours
and versions.

If you have a preferred way for how to install python, the Python plugin
will work if your installation satisfies the following conditions:
* Python version 3.4 or later (3.5 or later recommended)
* the required package `sortedcontainers` is installed
* the python interpreter is on the PATH as "python", or you specify the
  name or actual path to the interpreter via the `pythonBinary` or
  `pythonBinaryUrl` parameters

Otherwise, we recommend to install Python using the Anaconda or Miniconda
distribution for your operating system.

The following describes the recommended steps to install the Miniconda
distribution for Windows, MacOS and Linux. This mode of installation can 
be performed (as of this writing, January 2020) for Python versions 3.5 
or later.

It is also possible to install versions of Python into a Conda environment
which are older than the default Python version for that conda version
(though the earliest Python version for current Conda versions is 3.5). 
In order to use environments from the Python plugin, simply specify the 
path the the "python" binary in the environment directory as the 
Python binary for the PR.

## Windows

* Install the 64 bit distribution for Python 3.5 or later from https://conda.io/miniconda.html
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

* Install the 64 bit distribution for Python 3.5 or later from https://conda.io/miniconda.html
  * Download the file
  * Start a terminal and ...
  * Change permissions to make it executable:
    `chmod 700 Miniconda3-latest-Linux-x86_64.sh`
  * Run the file:
    `./Miniconda3-latest-Linux-x86_64.sh`
  * Agree to the license by typing in "yes" and ENTER
  * Confirm or change the location where to install to and take a note of that path (e.g. /Users//username/miniconda3)
  * Agree to initialize Miniconda3 in the .bashrc file (this will add everything to your binary path)
  * Start a new terminal and check: `which python` should show the location of the python command inside
    the miniconda installation directory (e.g. /Users//username/miniconda3/bin/python)
* Start a new terminal
* In the terminal window enter and run the following commands  (NOTE: a working internet connection is
  quired for this! The following steps will automatically download the required packages and all dependencies
  and install them into your Miniconda environment)
  * `pip install -U sortedcontainers`

## Linux and Linux-like Operating Systems

* Install the 64 bit distribution for Python 3.5 or later from https://conda.io/miniconda.html
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
* In the terminal window enter and run the following commands  
  (NOTE: a working internet connection is required for this! The following steps will automatically download the required packages and all dependencies
  and install them into your Miniconda environment)
  * `pip install -U sortedcontainers`
