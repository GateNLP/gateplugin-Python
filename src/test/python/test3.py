import sys
from gatenlp import interact, GateNlpPr, Document

@GateNlpPr
class MyProcessor:
  def __call__(self, doc, **kwargs):
    # get Set1 and Set2 and add one annotation to each set
    set = doc.annset("Set1")
    # print("*************** SET1 in Python:", set)
    set.add(1,4,"Type3",{"f1":1})
    set = doc.annset("Set2")
    # print("*************** SET2 in Python:", set)
    set.add(1,4,"Type4",{"f1":2})
    set = doc.annset("Set3")
    set.add(1,4,"Type5",{"f1":3})

interact()
