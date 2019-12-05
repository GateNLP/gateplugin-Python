"""Simple example PR to print the document text"""

import re
from gatenlp import interact, GateNlpPr

@GateNlpPr
def run(doc, **kwargs):
    set1 = doc.get_annotations()  
    set1.clear()   
    text = doc.text  
    whitespaces = [m for m in re.finditer(r"[\s,.!?]+|^[\s,.!?]*|[\s,.!?]*$",text)] 
    for k in range(len(whitespaces)-1): 
        fromoff=whitespaces[k].end()  
        tooff=whitespaces[k+1].start()  
        set1.add(fromoff, tooff, "Token", {"tokennr": k})
    doc.set_feature("nr_tokens", len(whitespaces)-1)

interact()
