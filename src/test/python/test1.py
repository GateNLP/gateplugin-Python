"""Simple example PR to print the document text"""

import sys
print("SYSTEM PATH:", sys.path, file=sys.stderr)
from gatenlp import interact, GateNlpPr, Document

@GateNlpPr
def run(doc, **kwargs):
    docfeature_null = doc.features.get("docfeature_null");
    docfeature_str = doc.features.get("docfeature_str")
    setdef = doc.get_annotations()
    ann = next(iter(setdef))
    annfeature_null = ann.features.get("annfeature_null");
    annfeature_str = ann.features.get("annfeature_str");
    # print("DEBUG (python): doc features we got: ", doc.features, file=sys.stderr)
    # print("DEBUG (python): ann features we got: ", ann.features, file=sys.stderr)
    set1 = doc.get_annotations("Set1")
    set1.clear()
    set1.add(1,4,"Type1",{"f1":12, "f2": "val2"})
    doc.features["FEAT"] = "VAL"
    doc.features.clear()
    doc.features["copy_docfeature_null"] = docfeature_null
    doc.features["copy_docfeature_str"] = docfeature_str
    doc.features["copy_annfeature_null"] = annfeature_null
    doc.features["copy_annfeature_str"] = annfeature_str
    doc.features["feat1"] = 12
    doc.features["feat2"] = "asdf"
    doc.features["feat1"] = 13

interact()
