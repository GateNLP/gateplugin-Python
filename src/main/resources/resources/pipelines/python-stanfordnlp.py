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
  def start(self, **kwargs):
    lang = kwargs.get("lang", "en")
    args = dict()
    for k in ["models_dir", "processors", "treebank", "use_gpu"]:
        if k in kwargs:
            args[k] = kwargs[k]
    self.nlp = stanfordnlp.Pipeline(**args)
  def finish(self, **kwargs):
    pass
  def __call__(self, doc, **kwargs):
    outset = ""
    if "outputAnnotationSet" in kwargs:
        outset =  kwargs.get("outputAnnotationSet")
    annset = doc.annset(outset)
    annset.clear()
    apply_stanfordnlp(self.nlp, doc, setname=outset) 
    return doc
    
    
if __name__ == '__main__':
  interact()
