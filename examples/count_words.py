import sys
import re
from gatenlp import interact, GateNlpPr, Document

@GateNlpPr
class MyProcessor:

  def __init__(self):
    self.words_total = 0
    self.nr_docs = 0
  def start(self, **kwargs):
    self.words_total = 0
    self.nr_docs = 0
  def finish(self, **kwargs):
    print("Total number of words:", self.words_total)
    print("Number of documents:", self.nr_docs)
    return {"nrdocs": self.nr_docs, "nrwords": self.words_total}
  def __call__(self, doc, **kwargs):
    text = doc.text
    whitespaces = [m for m in re.finditer(r"[\s,.!?]+|^[\s,.!?]*|[\s,.!?]*$",text)]
    nrwords = len(whitespaces)-1
    self.words_total += nrwords
    self.nr_docs += 1
    
if __name__ == '__main__':
  interact()
