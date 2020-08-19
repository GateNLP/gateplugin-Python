"""Run StanfordNLP"""

from gatenlp import interact, GateNlpPr, Document
import stanfordnlp
from gatenlp.lib_stanfordnlp import apply_stanfordnlp
import sys
import re

@GateNlpPr
class MyProcessor:

  def __init__(self):
    self.nlp = None
    self.tokens_total = 0
    self.nr_docs = 0
  def start(self, **kwargs):
    self.nlp = stanfordnlp.Pipeline()
    self.tokens_total = 0
    self.nr_docs = 0
  def finish(self, **kwargs):
    print("Total number of tokens:", self.tokens_total)
    print("Number of documents:", self.nr_docs)
  def __call__(self, doc, **kwargs):
    outset = ""
    if "outputAnnotationSet" in kwargs:
        outset =  kwargs.get("outputAnnotationSet")
    annset = doc.annset(outset)
    annset.clear()
    apply_stanfordnlp(self.nlp, doc, setname=outset) 
    
    self.tokens_total += len(doc)    
    self.nr_docs += 1
    return doc
    
    
if __name__ == '__main__':
  interact()
