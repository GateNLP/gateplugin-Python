"""Test use with GCP"""

import sys
from collections import Counter
import json
from gatenlp import interact, GateNlpPr, Document, logger

@GateNlpPr
class MyProcessor:

  def __init__(self):
      print("PRINTING FROM INIT to stderr", file=sys.stderr)
      self.ndocs = 0
      self.len = 0
  def start(self, **kwargs):
      print("PRINTING FROM START to stderr, kwargs={}".format(kwargs), file=sys.stderr)
      print("PRINTING FROM START to stout")
      logger.info("LOGGING FROM START")
      self.ndocs = 0
      self.len = 0
  def finish(self, **kwargs):
      print("PRINTING FROM FINISH to stderr, kwargs={}".format(kwargs), file=sys.stderr)
      logger.info("LOGGING FROM FINISH")
      print("GOT ndocs={}/len={}".format(self.ndocs, self.len))
      return {"ndocs": self.ndocs, "len":self.len}
  def reduce(self, resultlist):
      print("PRINTING FROM REDUCE to stderr, result="+str(resultlist), file=sys.stderr)
      logger.info("LOGGIN FROM REDUCE")
      cnts = Counter()
      for r in resultlist:
          cnts.update(r)
      # save the total counts as json to the current directory
      with open("test_gcp.result.json", "wt") as outfp:
          json.dump(cnts, outfp)
      return cnts
  def __call__(self, doc, **kwargs):
      # self.logger.info("LOGGING FROM CALL")
      print("PRINTING FROM CALL to stderr, kwargs={}".format(kwargs), file=sys.stderr)
      logger.info("LOGGING FROM CALL")
      self.ndocs += 1
      self.len += len(doc.text)

if __name__ == '__main__':
  interact()
