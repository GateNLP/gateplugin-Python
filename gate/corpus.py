class Corpus(object):
	def __init__(self, json):
		self.name = json["corpusName"]
		self.features = json["corpusFeatures"]