"""Simple example PR to print the document text"""

import sys
from gatenlp import interact, GateNlpPr, Document

@GateNlpPr
class MyProcessor:

  def start(**kwargs):
    print("Running start(), kwargs={}".format(kwargs), file=sys.stderr)
  def finish(**kwargs):
    print("Running finish(), kwargs={}".format(kwargs), file=sys.stderr)
  def __call__(doc, **kwargs):
    set1 = doc.get_annotations()  # get default annotation set 
    set1.clear()   # remove all current annotations from the set
    text = doc.text  # get the document text
    whitespaces = [m for m in re.finditer("\s+|^|$",text)] # find whitespace
    for k in range(len(ms)-1):  # tokens between all whitespace/begin/end
        fromoff=ms[k].end()   # token starts at the end of current whitespace
        tooff=ms[k+1].start()  # token ends at beginning of next whitespace
        set1.add(fromoff, tooff, "Token", {"tokennr": k})
    doc.set_feature("nr_tokens", len(ms)-1)
    
if __name__ == '__main__':
  interact()
