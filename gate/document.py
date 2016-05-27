from collections import defaultdict
from annotation_set import AnnotationSet
from sourcedstring import SourcedString
import HTMLParser, re

class _AnnotationSetsDict(defaultdict):
	def __init__(self, doc, logger):
		self.logger = logger
		self.doc = doc

	def __missing__(self, key):
		annotationSet = AnnotationSet(self.doc, name = key, logger = self.logger)
		self[key] = annotationSet
		# self.logger.append(("CREATE_AS", key))
		return annotationSet

class Document(object):
	def __init__(self, logger, text, features, src=None):
		self.logger = logger
		self.annotationSets = _AnnotationSetsDict(self, self.logger)
		self._text = text
		self.src = src
		self.features = features

	@staticmethod
	def load(json, src=None):
		"""Loads the document from a dictionary that results from GATE json, 
			returns a document and a change logger"""
		logger = []

		text = json["text"]

		features = json["documentFeatures"]

		doc = Document(logger, text, features, src)

		if "entities" in json and len(json["entities"]):
			for entity_key, instances in json["entities"].iteritems():
				annotation_set, annotation_name = entity_key.split(":")
				for entity in instances:	
					start, end = entity.pop("indices")
					_id = entity.pop("annotationID")

					doc.annotationSets[annotation_set].add(start, end, 
						annotation_name, entity, _id)


		del logger[:]
		return doc



	@property
	def text(self):
		return self._text

	@text.setter
	def text(self, value):
		raise NotImplementedError("Text cannot be modified in a Python processing resource")

	def size(self):
		return int(len(self.text))

	def __len__(self):
		return self.size()