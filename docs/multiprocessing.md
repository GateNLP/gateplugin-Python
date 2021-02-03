# Multiprocessing - Running PythonPr in parallel


PythonPr can be run in parallel using duplication, e.g. by running 
a pipeline that contains the PythonPr processing resource via 
gcp or gcp-direct (see https://github.com/GateNLP/gcp)

When running under gcp, the pipeline gets duplicated as many times as
needed so each parallel process has its own copy. 
Each duplication has its own duplicate of the PythonPr resource and each PythonPr resource starts its own Python process. So the code run for each duplicate is completely separate and variables cannot be shared
between them.

Each duplicate gets passed the following keyword arguments to
the  `start`, `_call__` and `finish` methods:

* `_duplicateId`: the duplicate number of the process
* `_nrDuplicates`: the total number of duplicates

When each of the process finishes, the `finish` method is invoked. 
If the `finish` method returns a map/dictionary, processing is a bit different if there are duplicates: since there are several duplicates, the maps returned by each of them are collected in a list and then passed on method `reduce` or the first duplicate, if that method is defined in the class. 

The `reduce` method is expected to use all the results from all the processes to calculate and return a map of the overall result for the 
whole processing which is then treated as a result returned from `finish` in the single process case. 

The following code uses the `reduce` method to calculate, print
and return the overall results of counting words (as in [PythonPrResult](PythonPrResult)) when running multiple duplicates:

```
 import sys
import re
from collections import Counter
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
    return {"nrdocs": self.nr_docs, "nrwords": self.words_total}
  def reduce(self, resultlist):
    totals = Counter()
    for r in resultlist:
      totals.update(r)
    print("Total number of documents: ", totals["nrdocs"])
    print("Total number of words: ", totals["nrwords"])
    return totals
  def __call__(self, doc, **kwargs):
    text = doc.text
    whitespaces = [m for m in re.finditer(r"[\s,.!?]+|^[\s,.!?]*|[\s,.!?]*$",text)]
    nrwords = len(whitespaces)-1
    self.words_total += nrwords
    self.nr_docs += 1

if __name__ == '__main__':
  interact()
```
