"""Test use with GCP"""

import sys
from collections import Counter
import json
from gatenlp import interact, GateNlpPr, Document, logger

@GateNlpPr
def myfunc(doc, **kwargs):
      print("PRINTING FROM CALL to stderr, kwargs={}".format(kwargs), file=sys.stderr)
      print("executing function with ", doc, file=sys.stderr)

if __name__ == '__main__':
  interact()
