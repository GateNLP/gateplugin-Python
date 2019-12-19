"""Test use with GCP"""

import sys
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
  def __call__(self, doc, **kwargs):
      # self.logger.info("LOGGING FROM CALL")
      print("PRINTING FROM CALL to stderr, kwargs={}".format(kwargs), file=sys.stderr)
      logger.info("LOGGING FROM CALL")
      self.ndocs += 1
      self.len += len(doc.text)

if __name__ == '__main__':
  interact()
