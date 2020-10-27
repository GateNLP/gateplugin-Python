"""Demonstrate using an annotator."""

import sys
from gatenlp import logger, interact
from gatenlp.processing.annotator import Annotator



class MyAnnotator(Annotator):
  def start(self,*args,**kwargs):
      logger.info(f"ANNOTATOR start, args: {args}, kwargs: {kwargs}")
  def __call__(self, doc, **kwargs):
      logger.info(f"ANNOTATOR __call__, kwargs: {kwargs}")
  def finish(self, **kwargs):
      logger.info(f"ANNOTATOR finish, kwargs: {kwargs}")
  def reduce(self, results, **kwargs):
      logger.info(f"ANNOTATOR redcue, results: {results}, kwargs: {kwargs}")

annotator = MyAnnotator()

interact(annotator=annotator)
