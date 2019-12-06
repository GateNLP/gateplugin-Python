"""Simple example PR to print the document text"""

import sys
print("SYSTEM PATH:", sys.path, file=sys.stderr)
from gatenlp import interact, GateNlpPr, Document

@GateNlpPr
def run(doc, **kwargs):
    docfeature_null = doc.get_feature("docfeature_null");
    docfeature_str = doc.get_feature("docfeature_str")
    setdef = doc.get_annotations()
    ann = next(iter(setdef))
    annfeature_null = ann.get_feature("annfeature_null");
    annfeature_str = ann.get_feature("annfeature_str");
    print("DEBUG (python): doc features we got: ", doc.features, file=sys.stderr)
    print("DEBUG (python): ann features we got: ", ann.features, file=sys.stderr)
    set1 = doc.get_annotations("Set1")
    set1.clear()
    set1.add(1,4,"Type1",{"f1":12, "f2": "val2"})
    doc.set_feature("FEAT", "VAL")
    doc.clear_features()
    doc.set_feature("copy_docfeature_null", docfeature_null);
    doc.set_feature("copy_docfeature_str", docfeature_str);
    doc.set_feature("copy_annfeature_null", annfeature_null);
    doc.set_feature("copy_annfeature_str", annfeature_str);
    doc.set_feature("feat1", 12)
    doc.set_feature("feat2", "asdf")
    doc.set_feature("feat1", 13)

interact()
