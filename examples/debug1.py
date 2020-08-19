
import sys
from gatenlp import interact, GateNlpPr, Document

@GateNlpPr
class MyProcessor:

  def __call__(self, doc, **kwargs):
    set2 = doc.get_annotations("Copy")
    set2.clear()
    set_def = doc.get_annotations()
    for ann in set_def:
        annid = set2.add(ann.start, ann.end, ann.type, ann.features)
        ann = set2.get(annid)
        ann.features["python_start"] = ann.start
        ann.features["python_end"] = ann.end

interact()
