"""Simple example PR to print the document text"""

from gatenlp import interact, GateNlpPr, Document

@GateNlpPr
def run(doc, **kwargs):
    print("We are running on a doc!", file=sys.stderr)

interact()
