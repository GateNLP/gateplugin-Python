from collections import defaultdict
from annotation_set import AnnotationSet
from sourcedstring import SimpleSourcedUnicodeString
import HTMLParser

html_parser = HTMLParser.HTMLParser()

class _AnnotationSetsDict(defaultdict):
	def __init__(self, doc, logger):
		self.logger = logger
		self.doc = doc

	def __missing__(self, key):
		annotationSet = AnnotationSet(self.doc, self.logger, key)
		self[key] = annotationSet
		# self.logger.append(("CREATE_AS", key))
		return annotationSet

class Document(object):
	def __init__(self, logger, text):
		self.logger = logger
		self.annotationSets = _AnnotationSetsDict(self, self.logger)
		text = html_parser.unescape(text)
		self._text = SimpleSourcedUnicodeString(text, text)

	@staticmethod
	def load(json):
		"""Loads the document from a dictionary that results from GATE json, 
			returns a document and a change logger"""
		logger = []

		doc = Document(logger, json["text"])

		if "entities" in json and len(json["entities"]):
			for entity_key, instances in json["entities"].iteritems():
				annotation_set, annotation_name = entity_key.split(":")
				for entity in instances:	
					start, end = entity.pop("indices")
					_id = entity.pop("annotationID")
					doc.annotationSets[annotation_set].add(start, end, 
						annotation_name, entity, _id)

		del logger[:]
		return logger, doc

	@property
	def text(self):
		return self._text

	@text.setter
	def text(self, value):
		raise NotImplementedError("Text cannot be modified in a Python processing resource")

	def size(self):
		return len(self.text)