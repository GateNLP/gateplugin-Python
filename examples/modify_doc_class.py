"""Simple example PR to print the document text"""

import sys
from gatenlp import interact, GateNlpPr, Document

@GateNlpPr
class MyProcessor:

  def start(self, **kwargs):
    print("Running start(), kwargs={}".format(kwargs), file=sys.stderr)
  def finish(self, **kwargs):
    print("Running finish(), kwargs={}".format(kwargs), file=sys.stderr)
  def __call__(self, doc, **kwargs):
    print("SOMETHING FOR STDOUT")
    print("SOMETHING FOR STDERR", file=sys.stderr)
    print("We are running on a doc! kwargs={}".format(kwargs), file=sys.stderr)
    set1 = doc.annset("PythonModifyClass")
    set1.clear()
    set1.add(1,4,"Type1",{"f1":12, "f2": "val2"})
    doc.features["FEAT"] = "VAL"
    doc.features.clear()
    doc.features["feat1"] = 12
    doc.features["feat2"] =  "asdf"
    doc.features["feat1"] = 13
    # doc.name = "MyTestDocument"
    set2 = doc.annset("PythonModifClass_Copy")
    set2.clear()
    set_def = doc.annset()
    for ann in set_def:
        annid = set2.add(ann.start, ann.end, ann.type, ann.features)
        ann = set2.get(annid)
        ann.features["python_start"] = ann.start
        ann.features["python_end"] = ann.end
    #print("!!!!!!!! CHANGELOG:")
    #for ch in doc.changelog.changes:
    #    print(ch)

interact()
