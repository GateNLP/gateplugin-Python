from annotation import Annotation
from functools import partial
import avl


class SearchOffset(object):
	def __init__(self, offset):
		self.start = offset
		self.end = offset

class AnnotationSet(object):
	def __init__(self, doc, logger, name, values = []):
		self.logger = logger
		self.name = name
		self.doc = doc

		def compare_start(a, b):
			return a.start - b.start

		def compare_end(a, b):
			return a.end - b.end

		values = list(set(values))
		self._annot_ids = {a.id for a in values}

		self._annotations_start = avl.new(source = values, compare = compare_start)
		self._annotations_end = avl.new(source = values, compare = compare_end) 

	def append(self, annotation):
		raise NotImplementedError("Cannot append directly to annotation set. Use add() instead.")

	def add(self, start, end, annotType, features, _id = None): 
		if _id and _id in self._annot_ids:
			return None
		elif _id is None:
			if self._annot_ids:
				_id = max(self._annot_ids) + 1
			else:
				_id = 1


		annotation = Annotation(self.logger, self, _id, annotType, start, end, features)

		self.logger.append({
			"command": "ADD_ANNOT", 
			"annotationSet": self.name, 
			"startOffset": start, 
			"endOffset": end, 
			"annotationName": annotType, 
			"featureMap": features, 
			"annotationID": annotation.id}
			)

		self._annot_ids.add(annotation.id)
		self._annotations_start.insert(annotation)
		self._annotations_end.insert(annotation)

		return annotation

	def remove(self, annotation):
		self.logger.append({
			"command": "REMOVE_ANNOT", 
			"annotationSet": self.name, 
			"annotationID": annotation.id})
		self._annotations_start.remove(annotation)
		self._annotations_end.remove(annotation)

	def __iter__(self): 
		return self._annotations_start.iter()

	def firstNode(self):
		return self._annotations_start.min()

	def get(self, startOffset, endOffset):
		lower, upper = self._annotations_start.span(SearchOffset(startOffset), SearchOffset(endOffset))
		result = self._annotations_start[lower:upper]

		lower, upper = self._annotations_end.span(SearchOffset(startOffset), SearchOffset(endOffset))
		result += self._annotations_end[lower:upper]

		return AnnotationSet(self.logger, self.doc, self.name, result)

	def getType(self, annotType):
		if annotType is not None:
			return AnnotationSet(self.logger, self.doc, self.name, 
				[a for a in self if a.type == annotType])
		else:
			return self

	def getCovering(self, annotType, startOffset, endOffset):
		lower, upper = self._annotations_start.span(SearchOffset(0), SearchOffset(startOffset))
		result = set(self._annotations_start[lower:upper])


		lower, upper = self._annotations_end.span(SearchOffset(endOffset),
			SearchOffset(self.doc.size()))
		result.intersection_update(set(self._annotations_end[lower:upper]))

		return AnnotationSet(self.logger, self.doc, self.name, result).getType(annotType)


	def getContained(self, startOffset, endOffset):
		lower, upper = self._annotations_start.span(SearchOffset(startOffset), SearchOffset(endOffset))
		result = set(self._annotations_start[lower:upper])

		lower, upper = self._annotations_end.span(SearchOffset(startOffset), SearchOffset(endOffset))
		result.intersection_update(set(self._annotations_end[lower:upper]))

		return AnnotationSet(self.logger, self.doc, self.name, result)

	def __repr__(self):
		return repr(self._annotations_start)
