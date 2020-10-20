# Command Line Debugging

It is possible to run and debug the Python programs used with the Python plugin
from the command line, provided the Python gatenlp package is installed separately, ideally in the same version as the what is included with the plugin. 

When running python on the program like this:

```
python programname.py
```

only the syntax of the program is checked (mode: check), and if no problems are found, no output is produced. 

Additional ways to run the script can be invoked by passing parameters, in order to get usage information you can run:

```
python programname.py -h
```

This produces the output:

```
usage: programname.py [-h] [--mode MODE] [--format FORMAT] [--path PATH]
                      [--out OUT] [-d] [--log_lvl LOG_LVL]

optional arguments:
  -h, --help         show this help message and exit
  --mode MODE        Interaction mode: pipe|http|websockets|file|dir|check
                     (default: check)
  --format FORMAT    Exchange format: json|json.gz|cjson
  --path PATH        File/directory path for modes file/dir
  --out OUT          Output file/directory path for modes file/dir
  -d                 Enable debugging: log to stderr
  --log_lvl LOG_LVL  Log level to use: DEBUG|INFO|WARNING|ERROR|CRITICAL
```

In order to run the program on a single file `infile.xml` and store the result as `outfile.xml` use:

```
python programname.py --mode file --path infile.xml --out outfile.xml 
```

Similarly, the program can be run on a while directory. 

Note that modes `http`, and `websockets` are not implemented, and
mode `pipe` is for developers of the Python plugin mainly.