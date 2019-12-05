"""Simple example PR to print the document text"""

import re
from gatenlp import interact, GateNlpPr

@GateNlpPr
def run(doc, **kwargs):
    set1 = doc.get_annotations()  # get default annotation set 
    set1.clear()   # remove all current annotations from the set
    text = doc.text  # get the document text
    # find whitespace and some punctuation
    whitespaces = [m for m in re.finditer("[\s,.!?]+|^[\s,.!?]*|[\s,.!?]*$",text)] 
    for k in range(len(whitespaces)-1):  # tokens between all whitespace/begin/end
        fromoff=whitespaces[k].end()   # token starts at the end of current whitespace
        tooff=whitespaces[k+1].start()  # token ends at beginning of next whitespace
        set1.add(fromoff, tooff, "Token", {"tokennr": k})
    doc.set_feature("nr_tokens", len(whitespaces)-1)

interact()
