## Calculating and Returning Over-The-Corpus Results

Most of the times the python program is just used to modify the document in some way, usually by adding annotations. 

But sometimes it may be useful to also calculate results over a whole
corpus. For example find the total number of tokens or sentences in the corpus. 

This can be done easily with the PythonPR when using a class to 
process the documents, storing over-the corpus data in the instance,
 and defining the `finish` method to do something with the overall
 results.
 
 The following examples shows how to calculate the number of 
 white-space separated tokens  over the whole corpus and  print out the total number of tokens and the number of documents processed:
 
 ```
 import sys
import re
from gatenlp import interact, GateNlpPr, Document

@GateNlpPr
class MyProcessor:

  def __init__(self):
    self.words_total = 0
    self.nr_docs = 0
  def start(self, **kwargs):
    self.words_total = 0
    self.nr_docs = 0
  def finish(self, **kwargs):
    print("Total number of words:", self.words_total)
    print("Number of documents:", self.nr_docs)
    return {"nrdocs": self.nr_docs, "nrwords": self.words_total}
  def __call__(self, doc, **kwargs):
    text = doc.text
    whitespaces = [m for m in re.finditer(r"[\s,.!?]+|^[\s,.!?]*|[\s,.!?]*$",text)]
    nrwords = len(whitespaces)-1
    self.words_total += nrwords
    self.nr_docs += 1
    
if __name__ == '__main__':
  interact()
```

It is also possible to return the data into GATE. If the `finish`method
returns a map, then by default the map gets stored as features of the PythonPr resource. After running the following Python program, 
the feature map of the PythonPr resource should contain features `nrdocs` and `nrwords` with their respective values. 

```
import sys
import re
from gatenlp import interact, GateNlpPr, Document

@GateNlpPr
class MyProcessor:

  def __init__(self):
    self.words_total = 0
    self.nr_docs = 0
  def start(self, **kwargs):
    self.words_total = 0
    self.nr_docs = 0
  def finish(self, **kwargs):
    print("Total number of words:", self.words_total)
    print("Number of documents:", self.nr_docs)
    return {"nrdocs": self.nr_docs, "nrwords": self.words_total}
  def __call__(self, doc, **kwargs):
    text = doc.text
    whitespaces = [m for m in re.finditer(r"[\s,.!?]+|^[\s,.!?]*|[\s,.!?]*$",text)]
    nrwords = len(whitespaces)-1
    self.words_total += nrwords
    self.nr_docs += 1
    
if __name__ == '__main__':
  interact()
```

It is also possible to do some further processing or visualization in 
GATE by implementing a special Language Resource which inherits from PythonPrResult. 



