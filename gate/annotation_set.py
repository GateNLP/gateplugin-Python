from annotation import Annotation
from functools import partial
from gate_exceptions import InvalidOffsetException
from tree import SliceableTree
from collections import defaultdict

class I(object):
	"""Short for Index, represents a pair of offsets to be used when searchign the tree"""
	def __init__(self, offset):
		self.start = offset
		self.end = offset

class AnnotationSet(object):
	def __init__(self, doc, values = [], name = "", logger = []):
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

	def restrict(self, annotations):
		"""Copies this annotation set, but restricts it to the given values"""
		# I have disabled this check for now so we can do set intersections. If this turns out to be evil I can add it
		# back in 
		# We really don't want to copy annotations that aren't in this set to begin with
		# for annotation in annotations:
		# 	if annotation not in self:
		# 		raise ValueError("Attempted to restrict an annotation set to values which it doesn't contain.")

		return AnnotationSet(self.doc, values = annotations, name = self.name, logger = self.logger)

	def _indexByType(self):
		"""Generates the type index. Only call this when you first need types, cos 
			it's kind of expensive and also can't be used in init."""
		self._annot_types = defaultdict(lambda: self.restrict([]))

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
		"""Remove the selected annotation"""
		self.logger.append({
			"command": "REMOVE_ANNOT", 
			"annotationSet": self.name, 
			"annotationID": annotation.id})

		del self._annots[annotation.id]
		self._annotations_start.remove(annotation)
		self._annotations_end.remove(annotation)

		if self._annot_types:
			self._annot_types[annotation.type].remove(annotation)

	def __iter__(self): 
		"""Allows iteration in document order"""
		return iter(self._annotations_start)

	def __getitem__(self, key):
		"""Gets annotations of the given type"""
		return self.type(key)

	def byID(self, key):
		return self._annots[key]

	def at(self, key):
		result = self._annotations_start[I(key)]
		return self.restrict(result)

	def firstAfter(self, key):
		result = self._annotations_start.nearest_after(I(key))
		return self.restrict(result)

	def overlapping(self, left, right = None):
		"""Gets annotations between the two points"""
		result = self._annotations_start[I(startOffset):I(endOffset)]
		result += self._annotations_end[I(startOffset):I(endOffset)]

		return self.restrict(result)

	def type(self, annotType):
		# Index the types the first time this is called
		if self._annot_types is None:
			self._indexByType()

		if annotType is not None:
			return self._annot_types[annotType]
		else:
			return self.restrict(self)

	def covering(self, startOffset, endOffset):
		result = set(self._annotations_start[I(0):I(startOffset)])

		result.intersection_update(set(self._annotations_end[I(endOffset):
			I(self.doc.size())]))

		return self.restrict(result)

	def within(self, startOffset, endOffset):
		result = set(self._annotations_start[I(startOffset):I(endOffset)])
		result.intersection_update(set(self._annotations_end[I(startOffset):
			I(endOffset)]))

		return self.restrict(result)

	def after(self, offset):
		"""Gets annotations that start after the given offset"""
		return self.restrict(self._annotations_start[I(offset):I(len(self.doc))])

	def before(self, offset):
		"""Gets annotations that start after the given offset"""
		return self.restrict(self._annotations_start[I(0):I(offset)])

	def first(self):
		return self._annotations_start.min()

	def last(self):
		return self._annotations_start.max()

	def __and__(self, other):
		keys = self._annots.viewitems() & other._annots.viewitems()
		return self.restrict([v for k, v in keys])

	def __sub__(self, other):
		keys = self._annots.viewitems() - other._annots.viewitems()
		return self.restrict([v for k, v in keys])

	def __or__(self, other):
		keys = self._annots.viewitems() | other._annots.viewitems()
		return self.restrict([v for k, v in keys])

	def __xor__(self, other):
		keys = self._annots.viewitems() ^ other._annots.viewitems()
		return self.restrict([v for k, v in keys])

	def __contains__(self, value):
		if hasattr(value, "id"): # Annotations have ids, so check those instead.
			return value.id in self._annots
		return value in self._annots # On the off chance someone passed an ID in directly

	contains = __contains__

	def __repr__(self):
		return repr(self._annotations_start)
