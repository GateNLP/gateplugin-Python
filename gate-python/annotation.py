import uuid

class InstrumentedFeaturesDict(dict): 
	def __init__(self, annotation, logger, features):
		super(InstrumentedFeaturesDict, self).__init__(features)
		self.logger = logger
		self.annotation = annotation

	def clear(self):
		self.logger.append(("CLEAR_FEATURES", 
			self.annotation.annotationSet.name, 
			self.annotation.id))
		super(InstrumentedFeaturesDict, self).clear()

	def pop(self, key, default = None):
		self.logger.append(("REMOVE_FEATURE", 
			self.annotation.annotationSet.name, 
			self.annotation.id, 
			key))
		return super(InstrumentedFeaturesDict, self).pop(key, default)

	def __setitem__(self, key, value):
		self.logger.append(("UPDATE_FEATURE", 
			self.annotation.annotationSet.name, 
			self.annotation.id, 
			key,
			value))
		super(InstrumentedFeaturesDict, self).__setitem__(key, value)


	def __delitem__(self, key):
		self.logger.append(("REMOVE_FEATURE", 
			self.annotation.annotationSet.name, 
			self.annotation.id, 
			key))
		super(InstrumentedFeaturesDict, self).__delitem__(key)


class Annotation(object): 
	def __init__(self, logger, annotationSet, _id, annotType, start, end, features):
		self.logger = logger
		self.type = annotType
		self.start = start
		self.end = end 
		self.features = InstrumentedFeaturesDict(self, logger, features)
		self.annotationSet = annotationSet
		self.id = _id

	def __eq__(self, other):
		if hasattr(other, "id"):
			return self.id == other.id
		else:
			return False

	def __hash__(self):
		return hash(self.id)

	def __repr__(self):
		return "<%s annotation %d at (%d, %d) in %s>" % (self.type, self.id,
			self.start, self.end, self.annotationSet.name)
