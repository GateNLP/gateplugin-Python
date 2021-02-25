import sys
import re
from collections import Counter
from gatenlp import interact, GateNlpPr, Document

@GateNlpPr
class MyProcessor:

  def __init__(self):
    self.words_total = 0
    self.nr_docs = 0
  def start(self, **kwargs):
    print("Starting MyProcessor, kwargs=", kwargs)
    self.words_total = 0
    self.nr_docs = 0
  def finish(self, **kwargs):
    print("Finishing MyProcessor, kwargs=", kwargs)
    return {"nrdocs": self.nr_docs, "nrwords": self.words_total}
  def reduce(self, resultlist):
    print("Running MyProcessor.reduce with result=", resultlist)
    totals = Counter()
    for r in resultlist:
      totals.update(r)
    print("Total number of documents: ", totals["nrdocs"])
    print("Total number of words: ", totals["nrwords"])
    return totals
  def __call__(self, doc, **kwargs):
    text = doc.text
    whitespaces = [m for m in re.finditer(r"[\s,.!?]+|^[\s,.!?]*|[\s,.!?]*$",text)]
    nrwords = len(whitespaces)-1
    self.words_total += nrwords
    self.nr_docs += 1
    
if __name__ == '__main__':
  interact()
