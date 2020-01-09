"""Run spacy"""

from gatenlp import interact, GateNlpPr, Document
from gatenlp.spacy import apply_nlp
import sys
import re
import spacy

@GateNlpPr
class MyProcessor:

  def __init__(self):
    self.nlp = None
    self.tokens_total = 0
    self.nr_docs = 0
  def start(self, **kwargs):
    if "spacyModel" in kwargs:
        self.nlp = spacy.load(kwargs.get("spacyModel"))
    else:
        self.nlp = spacy.load("en_core_web_sm")
    self.tokens_total = 0
    self.nr_docs = 0
  def finish(self, **kwargs):
    print("Total number of tokens:", self.tokens_total)
    print("Number of documents:", self.nr_docs)
  def __call__(self, doc, **kwargs):
    # print("PROCESSING: ",doc.get_feature("gate.plugin.python.docName"))
    outset = ""
    if "outputAnnotationSet" in kwargs:
        outset =  kwargs.get("outputAnnotationSet")
    annset = doc.get_annotations(outset)
    annset.clear()
    apply_nlp(self.nlp, doc, setname=outset) 
    
    self.tokens_total += len(doc)    
    self.nr_docs += 1
    return doc
    
    
if __name__ == '__main__':
  interact()
