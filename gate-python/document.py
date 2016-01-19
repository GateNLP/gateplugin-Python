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
		self._text = text

	@staticmethod
	def load(json):
		"""Loads the document from a dictionary that results from GATE json, 
			returns a document and a change logger"""
		logger = []

		text = SimpleSourcedUnicodeString(json["text"], json["text"])
		text = html_parser.unescape(text)

		doc = Document(logger, text)

		if "entities" in json and len(json["entities"]):
			for entity_key, instances in json["entities"].iteritems():
				annotation_set, annotation_name = entity_key.split(":")
				for entity in instances:	
					start, end = entity.pop("indices")
					_id = entity.pop("annotationID")
					doc.annotationSets[annotation_set].add(start, end, 
						annotation_name, entity, _id)

		# Compensate for text unescape. happens here to take advantage of indices.			
		for real_offset, source in text.sources:
			if source.begin != source.end and real_offset != source.begin:
				for annotationSet in doc.annotationSets.values():
					offset_adjust = source.begin - real_offset
					for annotation in annotationSet.get(source.begin, source.end):
						annotation.start -= offset_adjust
						annotation.end -= offset_adjust

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