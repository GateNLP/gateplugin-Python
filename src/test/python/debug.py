import sys
import os
print("PYTHON DEBUG OUTPUT:", file=sys.stderr)
print("PYTHONHOME:", os.environ.get("PYTHONHOME"))
print("PYTHON VERSION:", sys.version_info)
print("PYTHON PATH:", sys.path)
import sortedcontainers
print("SORTEDCONTAINERS:", sortedcontainers.__version__)
