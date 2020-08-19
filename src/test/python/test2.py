"""Simple example PR to print the document text"""

import sys
print("SYSTEM PATH:", sys.path, file=sys.stderr)
from gatenlp import interact, GateNlpPr, Document

@GateNlpPr
class MyProcessor:
  def __call__(self, doc, **kwargs):
    set1 = doc.annset("Set1")
    set1.clear()
    set1.add(1,4,"Type1",{"f1":12, "f2": "val2"})
    doc.features["FEAT"] = "VAL"
    doc.features.clear()
    doc.features["feat1"] = 12
    doc.features["feat2"] = "asdf"
    doc.features["feat1"] = 13

interact()
