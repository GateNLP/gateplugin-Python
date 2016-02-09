from annotation import Annotation
from functools import partial
from tree import SliceableTree
from exceptions import InvalidOffsetException
from collections import defaultdict

class SearchOffset(object):
	def __init__(self, offset):
		self.start = offset
		self.end = offset

class SliceProxy(object):
	"""Provides alternative slice operator functionality so you can write things like
		annotationSet.convering[startOffset:endOffset]"""
	def __init__(self, function):
		self.function = function

	def __getitem__(self, key):
		if isinstance(key, slice):
			return self.function(key.start, key.stop)
		else:
			return self.function(key)


class AnnotationSet(object):
	def __init__(self, doc, name = "", logger = [], values = []):
		self.logger = logger
		self.name = name
		self.doc = doc

		def compare_start(a, b):
			return a.start - b.start

		def compare_end(a, b):
			return a.end - b.end

		# I've commented this out because it's expensive and I don't understand it.
		# values = list(set(values))

		for value in values: # Ensure that there are no invalid annotations
			self._check_offsets(value)

		self._annots = {a.id: a for a in values}
		self._annot_types = None # Will only be populated when needed

		# Using itervalues for dict to populate the indices - this prevents duplicate annotations
		self._annotations_start = SliceableTree(self._annots.itervalues(), compare = compare_start)
		self._annotations_end = SliceableTree(self._annots.itervalues(), compare = compare_end) 

		# Accessors for alternative slice operations. This is pure syntactic sugar.
		self.covering = SliceProxy(self.getCovering)
		self.contained = SliceProxy(self.getContained)
		self.offsets = SliceProxy(self.getByOffset)


	def _indexByType(self):
		"""Generates the type index. Only call this when you first need types, cos 
			it's kind of expensive and also can't be used in init."""
		self._annot_types = defaultdict(lambda: AnnotationSet(self.doc))

		for annotation in self._annots.itervalues():
			self._annot_types[annotation.type].append(annotation)


	def __len__(self):
		return len(self._annots)

	"""Just calls __len__. This exists to make the API more like GATE"""
	size = __len__

	def _check_offsets(self, annotation):
		"""Checks the offsets for the given annotation against the document boundaries"""
		doc_size = self.doc.size()  

		if annotation.start < 0:
			raise InvalidOffsetException("Annotation starts before 0")
		if annotation.end < 0:
			raise InvalidOffsetException("Annotation ends before 0")
		if annotation.start > annotation.end:
			raise InvalidOffsetException("Annotation ends before it starts")
		if annotation.start > doc_size:
			raise InvalidOffsetException("Annotation starts after document ends")
		if annotation.end > doc_size: 
			raise InvalidOffsetException("Annotation ends after document ends")

		return annotation

	def constrain(self, features):
		"""Finds items that contain the given features + values.
		Does not support the special ANNIE values from GATE Java.
		"""
		def subsumes(d1, d2):
			for key, value in d2.iteritems():
				if key not in d1 or d1[key] != value:
					return False
			return True
		return AnnotationSet(self.doc, 
							self.name, 
							self.logger,
							[a for a in self if subsumes(a.features, features)])
		
	def append(self, annotation):
		if annotation.id and annotation.id in self._annots: # Prevents duplicate annotations
			return None
		elif annotation.id is None:
			# Populate the annotation ID if there is none.
			if self._annots:
				annotation.id = max(self._annots.keys()) + 1
			else:
				annotation.id = 1

		self._check_offsets(annotation) # Will raise exception if the annotation is out of range


		# Log the new annotation
		self.logger.append({
			"command": "ADD_ANNOT", 
			"annotationSet": self.name, 
			"startOffset": annotation.start, 
			"endOffset": annotation.end, 
			"annotationName": annotation.type, 
			"featureMap": annotation.features, 
			"annotationID": annotation.id}
			)

		# Add the annotation to the required indices
		self._annots[annotation.id] = annotation
		self._annotations_start.insert(annotation)
		self._annotations_end.insert(annotation)

		if self._annot_types:
			self._annot_types[annotation.type].append(annotation)

		return annotation

	def add(self, start, end, annotType, features, _id = None): 
		return self.append(Annotation(self.logger, self, _id, annotType, start, end, features))

	def remove(self, annotation):
		self.logger.append({
			"command": "REMOVE_ANNOT", 
			"annotationSet": self.name, 
			"annotationID": annotation.id})

		del self._annots[annotation.id]
		self._annotations_start.remove(annotation)
		self._annotations_end.remove(annotation)

		if self._annot_types:
			self._annot_types[annotation.type].remove(annotation)

	def removeAll(self, values = None):
		if values is None:
			# No need to do expensive deletion operation on the index, just throw them away.

			for annotation_id in self._annots.iterkeys(): # Log each deletion first.
				self.logger.append({
					"command": "REMOVE_ANNOT", 
					"annotationSet": self.name, 
					"annotationID": annotation_id})

			self._annots = dict()
			self._annot_types = None # Will only be populated when needed

			# Using itervalues for dict to populate the indices - this prevents duplicate annotations
			self._annotations_start = SliceableTree(compare = compare_start)
			self._annotations_end = SliceableTree(compare = compare_end) 
		else:
			for value in values:
				self.remove(value)

	def __iter__(self): 
		return iter(self._annotations_start)

	def firstNode(self):
		return self._annotations_start.min()

	def __getitem__(self, key):
		"""Multi-purpose [] operator for this annotation set"""
		if isinstance(key, slice):
			return self.getSpan(key.start, key.stop)
		elif isinstance(key, str):
			return self.getType(key)
		elif key is not None: # IDs cannot be None, strings or slices
			return self.getByID(key)
		else: # None doesn't really mean anything, so just return a copy of myself.
			return AnnotationSet(self.doc, name=self.name, logger=self.logger, values=self)

	def get(self, left, right = None): 
		"""Multi-purpose get function roughly as provided in GATE.

			Does not differentiate between long and int. Numbers are assumed to 
			be annotation IDs, and slices are assumed to be offsets.

			Does not let you simultaniously filter by type and offset, 
			instead split this into a two step process.
			"""
		# Construct the slice if needed.
		if right:
			return self.__getitem__(slice(left, right))
		else:
			return self.__getitem__(left)

	def getByID(self, key):
		return self._annots[key]

	def getByOffset(self, key):
		result = self._annotations_start[SearchOffset(key)]
		return AnnotationSet(self.doc, name = self.name, logger = self.logger, values = result)

	def getByOffsetAfter(self, key):
		result = self._annotations_start.nearest_after(SearchOffset(key))
		return AnnotationSet(self.doc, name = self.name, logger = self.logger, values = result)

	def getSpan(self, startOffset, endOffset):
		if endOffset is None:
			endOffset = self.doc.size()

		result = self._annotations_start[SearchOffset(startOffset):SearchOffset(endOffset)]
		result += self._annotations_end[SearchOffset(startOffset):SearchOffset(endOffset)]

		return AnnotationSet(self.doc, name = self.name, logger = self.logger, values = result)

	def getType(self, annotType):
		# Index the types the first time this is called
		if self._annot_types is None:
			self._indexByType()

		if annotType is not None:
			return self._annot_types[annotType]
		else:
			return AnnotationSet(self.doc, name = self.name, logger = self.logger, values = self)

	def getCovering(self, startOffset, endOffset):
		if endOffset is None:
			endOffset = self.doc.size()

		result = set(self._annotations_start[SearchOffset(0):SearchOffset(startOffset)])

		result.intersection_update(set(self._annotations_end[SearchOffset(endOffset):
			SearchOffset(self.doc.size())]))

		return AnnotationSet(self.doc, name = self.name, logger = self.logger, values = result)

	def getContained(self, startOffset, endOffset):
		if endOffset is None:
			endOffset = self.doc.size()

		result = set(self._annotations_start[SearchOffset(startOffset):SearchOffset(endOffset)])
		result.intersection_update(set(self._annotations_end[SearchOffset(startOffset):
			SearchOffset(endOffset)]))

		return AnnotationSet(self.doc, name = self.name, logger = self.logger, values = result)

	def __contains__(self, value):
		if hasattr(value, "id"): # Annotations have ids, so check those instead.
			return value.id in self._annots
		return value in self._annots # On the off chance someone passed an ID in directly

	contains = __contains__

	def __repr__(self):
		return repr(self._annotations_start)
