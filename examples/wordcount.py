"""Simple example to illustrate over the corpus results"""

import sys
import re
from gatenlp import interact, GateNlpPr, Document
from collections import Counter

# get the Token annotations from GATE, count the number of tokens per kind 
# and category
@GateNlpPr
class MyProcessor:
  def start(self, **kwargs):
    self.counter = Counter()
  def finish(self, **kwargs):
    return self.counter  
  def __call__(self, doc, **kwargs):
    anns = doc.get_annotations()  
    for ann in anns:
        cat = ann.get_feature("category")
        if cat is None:
            cat = "(None)"
        kind = ann.get_feature("kind")
        if kind is None:
            kind = "(None)"
        self.counter["cat_"+cat] += 1
        self.counter["kind_"+kind] += 1
    self.counter["nr_docs"] += 1
  def reduce(self, resultlist):
    sums = Counter()
    for c in resultlist:
       sums.update(c)
    return sums

interact()
