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
    set1 = doc.get_annotations("PythonModifyClass")
    set1.clear()
    set1.add(1,4,"Type1",{"f1":12, "f2": "val2"})
    doc.set_feature("FEAT", "VAL")
    doc.clear_features()
    doc.set_feature("feat1", 12)
    doc.set_feature("feat2", "asdf")
    doc.set_feature("feat1", 13)

if __name__ == '__main__':
  interact()
