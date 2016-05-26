
"""String that remember where they came from when you perform operations on them.

This is important because it allows us to know where tokens etc came from after tokenization.

We don't need the full implementation of SourcedString from NLTK, and not having it will make 
the python3 port easier!."""

class Source(object):
	def __init__(self, source, start = 0, end = None):
		if end == None:
			end = len(source)

		self.start = start
		self.end = end
		self.source = source

	def __repr__(self):
		return "'%s'[%d:%d]" % (self.source, self.start, self.end)

class SourcedString(unicode):
	def __new__(cls, source, sources = None):
		self = super(SourcedString, cls).__new__(cls, source)

		if sources == None:
			self.sources = [Source(source)]
		else:
			self.sources = sources

		return self

	def __getslice__(self, start, stop):
		result = super(SourcedString, self).__getslice__(start, stop)
		
		if start < 0:
			start = len(self) + start
		if stop < 0:
			stop = len(stop) + stop

		if start < stop:
			sources = [Source(self, start, min(stop, len(self)))]
		else:
			sources = []
		return SourcedString(result, sources)

	def split(self, sep=" ", maxsplit=None):
		parts = []
		remainder = self
		while len(parts) != maxsplit and remainder:
			splitLocation = remainder.rfind(sep)
			
			if splitLocation == -1:
				parts.insert(0, remainder)
				remainder = ""
			else:	
				parts.insert(0, remainder[splitLocation+1:])
				remainder = remainder[:splitLocation]
		return parts

	def rsplit(self, sep=" ", maxsplit=None):
		parts = []
		remainder = self
		while len(parts) != maxsplit and remainder:
			splitLocation = remainder.rfind(sep)
			
			if splitLocation == -1:
				parts.insert(0, remainder)
				remainder = ""
			else:	
				parts.insert(0, remainder[splitLocation+1:])
				remainder = remainder[:splitLocation]
		return parts

	def split(self, sep=" ", maxsplit=None):
		parts = []
		remainder = self
		while len(parts) != maxsplit and remainder:
			splitLocation = remainder.find(sep)
			
			if splitLocation == -1:
				parts.append(remainder)
				remainder = ""
			else:	
				parts.append(remainder[:splitLocation])
				remainder = remainder[splitLocation+1:]
		return parts

    @staticmethod
    def concat(substrings):
		substrings = 

    def __add__(self, other):
        return SourcedString.concat([self, other])
