"""Simple example to do very simple whitespace-tokenization"""

import sys
import re
from gatenlp import interact, GateNlpPr, Document

@GateNlpPr
class MyProcessor:

  def __init__(self):
    self.tokens_total = 0
    self.nr_docs = 0
  def start(self, **kwargs):
    self.tokens_total = 0
    self.nr_docs = 0
  def finish(self, **kwargs):
    print("Total number of tokens:", self.tokens_total)
    print("Number of documents:", self.nr_docs)
  def __call__(self, doc, **kwargs):
    set1 = doc.get_annotations("PythonTokenizeClass")  
    set1.clear()   
    text = doc.text
    whitespaces = [m for m in re.finditer(r"[\s,.!?]+|^[\s,.!?]*|[\s,.!?]*$",text)]
    nrtokens = len(whitespaces)-1
    for k in range(nrtokens):  
        fromoff=whitespaces[k].end()   
        tooff=whitespaces[k+1].start() 
        # cause an exception 
        weird = fromoff/(k-2)   # this wil raise divide by zero for the 3rd token
        set1.add(fromoff, tooff, "Token", {"tokennr": k, "python_start": fromoff, "python_end": tooff})
    doc.set_feature("nr_tokens", nrtokens)
    self.tokens_total += nrtokens
    self.nr_docs += 1

    
if __name__ == '__main__':
  interact()
