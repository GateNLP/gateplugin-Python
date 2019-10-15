from collections import defaultdict
import sys

from .annotation import Annotation
from .gate_exceptions import InvalidOffsetException
from .tree import SliceableTree

class I(object):
	"""Short for Index, represents a pair of offsets to be used when searching the tree"""
	def __init__(self, offset):
		self.start = offset
		self.end = offset

def support_annotation(method):
	"""Decorator to allow a method that normally takes a start and end
		offset to take an annotation instead."""
	def _support_annotation(self, *args):
		if len(args) == 1:
			# Assume we have an annotation
			try:
				left, right = args[0].start, args[0].end
			except AttributeError:
				raise ValueError("Supplied argument is not a range or an annotation")
		else:
			left, right = args

		return method(self, left, right)

	return _support_annotation

def support_single(method):
	"""Decorator to allow a method that normally takes a start and end
		offset to take a single argument that represents both."""
	def _support_annotation(self, *args):
		if len(args) == 1:
			# Assume we have an annotation
			if isinstance(args[0], int):
				return method(self, args[0], args[0])
		else:
			return method(self, *args)

	return _support_annotation

class AnnotationSet(object):
	def __init__(self, doc, values = [], name = "", logger = []):
		self.logger = logger
		self.name = name
		self.doc = doc

		# I've commented this out because it's expensive and I don't understand it.
		# values = list(set(values))

		for value in values: # Ensure that there are no invalid annotations
			self._check_offsets(value)

		self._annots = {a.id: a for a in values}
		self._annot_types = None # Will only be populated when needed

		self._annotations_start = None
		self._annotations_end = None

		self._index_by_offset()

	def restrict(self, annotations):
		"""Copies this annotation set, but restricts it to the given values"""
		# I have disabled this check for now so we can do set intersections. If this turns out to be evil I can add it
		# back in
		# We really don't want to copy annotations that aren't in this set to begin with
		# for annotation in annotations:
		# 	if annotation not in self:
		# 		raise ValueError("Attempted to restrict an annotation set to values which it doesn't contain.")

		return AnnotationSet(self.doc, values = annotations, name = self.name, logger = self.logger)

	def _index_by_offset(self):
		"""(Re)generates the offset index which is stored in the form of two red black trees"""
		def compare_start(a, b):
			return a.start - b.start

		def compare_end(a, b):
			return a.end - b.end

		self._annotations_start = SliceableTree(self._annots.values(), compare=compare_start)
		self._annotations_end   = SliceableTree(self._annots.values(), compare=compare_end)

	def _index_by_type(self):
		"""Generates the type index. Only call this when you first need types, cos
			it's kind of expensive and also can't be used in init."""
		self._annot_types = defaultdict(lambda: self.restrict([]))

		for annotation in self._annots.values():
			self._annot_types[annotation.type].append(annotation, log = False)

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
			print >> sys.stderr, annotation.start, doc_size, self.doc.text, annotation
			raise InvalidOffsetException("Annotation starts after document ends")
		if annotation.end > doc_size:
			raise InvalidOffsetException("Annotation ends after document ends")

		return annotation

	def append(self, annotation, check_offsets = True, log= True):
		"""Appends an annotation to the annotation set. Do not try to add annotations
			from another annotation set, as one annotation can belong to only one set,
			or a child of that set"""
		if annotation.id and annotation.id in self._annots: # Prevents duplicate annotations
			return None
		elif annotation.id is None:
			# Populate the annotation ID if there is none.
			if self._annots:
				annotation.id = max(self._annots.keys()) + 1
			else:
				annotation.id = 1

		if check_offsets:
			self._check_offsets(annotation) # Will raise exception if the annotation is out of range

		# Log the new annotation
		if log:
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
			self._annot_types[annotation.type].append(annotation, check_offsets)

		return annotation

	def add(self, start, end, annotType, features, _id = None, check_offsets = True):
		"""Adds an new annotation with the given values"""
		return self.append(Annotation(self.logger, self, _id, annotType, start, end, features), check_offsets)

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
		"""Gets the annotation with the given ID"""
		return self._annots[key]

	def type(self, annotType):
		"""Gets annotations of the specified type"""
		# Index the types the first time this is called
		if self._annot_types is None:
			self._index_by_type()

		if annotType is not None:
			return self._annot_types[annotType]
		else:
			return self.restrict(self)

	def typeNames(self):
		"""Gets the names of all types in this set"""
		if self._annot_types is None:
			self._index_by_type()

		return self._annot_types.keys()

	def types(self):
		"""Returns the dictionary index of types of annotation in this set"""
		if self._annot_types is None:
			self._index_by_type()

		return self._annot_types

	def at(self, offset):
		"""Gets all annotations at the given offset (empty if none)"""
		result = self._annotations_start[I(offset)]
		return self.restrict(result)

	def firstAfter(self, offset):
		"""Gets all annotations at the first valid position after the given offset"""
		result = self._annotations_start.nearest_after(I(offset))
		return self.restrict(result)

	@support_single
	@support_annotation
	def overlapping(self, left, right):
		"""Gets annotations overlapping with the two points"""
		result = self._annotations_start[I(left):I(right)]
		result += self._annotations_end[I(left+1):I(max(right, left+1))] # Must not end at the left offset.

		return self.restrict(result)

	@support_single
	@support_annotation
	def covering(self, left, right):
		"""Gets annotations that completely cover the span given"""
		result = set(self._annotations_start[I(0):I(left)])
		result.intersection_update(set(self._annotations_end[I(right):I(self.doc.size())]))
		return self.restrict(result)

	@support_annotation
	def within(self, left, right):
		"""Gets annotations that fall completely within the left and right given"""
		result = set(self._annotations_start[I(left):I(right)])
		result.intersection_update(set(self._annotations_end[I(left):I(right)]))

		return self.restrict(result)

	def after(self, offset):
		"""Gets annotations that start after the given offset"""
		return self.restrict(self._annotations_start[I(offset):I(self.doc.size())])

	def before(self, offset):
		"""Gets annotations that start after the given offset"""
		return self.restrict(self._annotations_start[I(0):I(offset)])

	def first(self):
		"""Gets the first annotation within the annotation set"""
		return self._annotations_start.min()

	def last(self):
		"""Gets the last annotation within the annotation set"""
		return self._annotations_start.max()

	def __contains__(self, value):
		"""Provides annotation in annotation_set functionality"""
		if hasattr(value, "id"): # Annotations have ids, so check those instead.
			return value.id in self._annots and value in self._annots.viewvalues()
		return value in self._annots # On the off chance someone passed an ID in directly

	contains = __contains__

	def __repr__(self):
		return repr([annotation for annotation in self])
