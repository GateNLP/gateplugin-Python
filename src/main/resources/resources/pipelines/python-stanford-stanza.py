"""Run Stanford Stanza"""

from gatenlp import interact, GateNlpPr, Document
import stanza
from gatenlp.lib_stanza import apply_stanza
import sys
import re

@GateNlpPr
class MyProcessor:

  def __init__(self):
    self.nlp = None
  def start(self, **kwargs):
    args = dict()
    for k in ["lang", "dir", "package", "processors", "logging_level", "verbose", "use_gpu"]:
        if k in kwargs:
            if k in ["verbose", "use_gpu"]:
                if kwargs[k]:
                    args[k] = bool(kwargs[k])
            else:
                if kwargs[k]:
                  args[k] = kwargs[k]
    self.nlp = stanza.Pipeline(**args)
  def finish(self, **kwargs):
    pass
  def __call__(self, doc, **kwargs):
    outset =  kwargs.get("outputAnnotationSet","")
    annset = doc.annset(outset)
    annset.clear()
    apply_stanza(self.nlp, doc, setname=outset) 
    return doc
    
    
if __name__ == '__main__':
  interact()
