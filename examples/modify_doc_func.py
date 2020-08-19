"""Simple example PR to print the document text"""

import sys
from gatenlp import interact, GateNlpPr, Document
from mypackage import helpermodule

@GateNlpPr
def run(doc, **kwargs):
    print("We are running on a doc! kwargs={}".format(kwargs), file=sys.stderr)
    helpermodule.helperfunc()
    set1 = doc.get_annotations("PythonModifyFunc")
    set1.clear()
    set1.add(1,4,"Type1",{"f1":12, "f2": "val2"})
    doc.featuresr["FEAT"] = "VAL"
    doc.features.clear()
    doc.features["feat1"] = 12
    doc.features["feat2"] = "asdf"
    doc.features["feat1"] = 13
    print("changelog", doc.changelog)
    
interact()
