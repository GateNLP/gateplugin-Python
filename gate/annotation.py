class InstrumentedFeaturesDict(dict): 
	def __init__(self, annotation, logger, features):
		super(InstrumentedFeaturesDict, self).__init__(features)
		self.logger = logger
		self.annotation = annotation

	def clear(self):
		self.logger.append(			{
				"command": "CLEAR_FEATURES", 
				"annotationSet": self.annotation.annotationSet.name, 
				"annotationID": self.annotation.id
			})
		super(InstrumentedFeaturesDict, self).clear()

	def pop(self, key, default = None):
		self.logger.append({
			"command": "REMOVE_FEATURE", 
			"annotationSet": self.annotation.annotationSet.name, 
			"annotationID": self.annotation.id,
			"featureName": key
		})
		return super(InstrumentedFeaturesDict, self).pop(key, default)

	def __setitem__(self, key, value):
		self.logger.append({
			"command": "UPDATE_FEATURE", 
			"annotationSet": self.annotation.annotationSet.name, 
			"annotationID": self.annotation.id,
			"featureName": key,
			"featureValue": value
		})
		super(InstrumentedFeaturesDict, self).__setitem__(key, value)


	def __delitem__(self, key):
		self.logger.append({
			"command": "REMOVE_FEATURE", 
			"annotationSet": self.annotation.annotationSet.name, 
			"annotationID": self.annotation.id,
			"featureName": key
		})		
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
