"""Simple example to do very simple whitespace-tokenization"""

import sys
from gatenlp import interact, GateNlpPr, Document

@GateNlpPr
class MyProcessor:

  def __init__(self):
    print("DEBUG: running __init__")
    self.tokens_total = 0
  def start(**kwargs):
    print("DEBUG: running start")
    self.tokens_total = 0
  def finish(**kwargs):
    print("DEBUG: running finish")
  def __call__(doc, **kwargs):
    set1 = doc.get_annotations()  
    set1.clear()   
    text = doc.text  
    whitespaces = [m for m in re.finditer(r"[\s,.!?]+|^[\s,.!?]*|[\s,.!?]*$",text)] 
    for k in range(len(whitespaces)-1):  
        fromoff=whitespaces[k].end()   
        tooff=whitespaces[k+1].start() 
        set1.add(fromoff, tooff, "Token", {"tokennr": k})
    doc.set_feature("nr_tokens", len(whitespaces)-1)

    
if __name__ == '__main__':
  interact()
