from gatenlp import Document, AnnotationSet, GateNlpPr, interact

@GateNlpPr
class MyAnnotator:
    # the following method is run on every document, this method must exist:
    def __call__(self, doc, **kwargs):
        pass

    # the start and finish methods are optional, if they exist the start
    # method is called before the first document of a corpus and the finish 
    # method is called after the last document.
    # def start(self, **kwargs):
    #     pass
    # def finish(self, **kwargs):
    #     pass

# THE FOLLOWING MUST BE PRESENT SO THAT GATE CAN COMMUNICATE WITH THE PYTHON PROCESS!
if __name__ == "__main__":
    interact()
