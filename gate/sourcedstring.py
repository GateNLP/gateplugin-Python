# sourcedstring for Python 2 or 3.

# This modules selects either sourcedstring2 or sourcedstring3
# according to the Python version

import sys

if sys.version_info >= (3,):
    if __package__:
        from .sourcedstring3 import *
    else:
        from sourcedstring3 import *
else:
    if __package__:
        from .sourcedstring2 import *
    else:
        from sourcedstring2 import *

del sys
