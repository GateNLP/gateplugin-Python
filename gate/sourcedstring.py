
"""String that remember where they came from when you perform operations on them.

This is important because it allows us to know where tokens etc came from after tokenization.

We don't need the full implementation of SourcedString from NLTK, and not having it will make 
the python3 port easier!."""

import re
class Source(object):
	def __init__(self, source, 
			start = 0, end = None,
			source_start = 0, source_end = None):
		if source_end == None:
			source_end = len(source)

		if end == None:
			end = source_end

		self.start = start
		self.end = end

		self.source_start = source_start
		self.source_end = source_end

		self.source = source

	def __repr__(self):
		return "'%s'[%d:%d]->[%d:%d]" % (self.source, 
			self.source_start, self.source_end,
			self.start, self.end)

class SourcedString(unicode):
	def __new__(cls, source, sources = None):
		self = super(SourcedString, cls).__new__(cls, source)

		if sources == None:
			self.sources = [Source(source)]
		else:
			self.sources = sources

		return self

	def __getslice__(self, start, end):
		result = super(SourcedString, self).__getslice__(start, end)
		
		if start < 0:
			start = len(self) + start
		if end < 0:
			end = len(end) + end

		if start < end:
			sources = []
			lastOffset = 0
			for source in self.sources:
				# If the source overlaps with the new boundaries
				# use it.
					
				# There has got to be a better way to do this!
				if not (source.end <= start or source.start > end):
					# Work out where the new start and end are
					toTake=min(source.end - source.start, end - start)
					newStart = source.source_start + max(start, source.start) - source.start
					sources.append(Source(
							source.source,
							start=lastOffset,
							end=lastOffset + toTake,
							source_start=newStart,
							source_end=min(source.source_end, newStart + toTake)
						))
					lastOffset += toTake
		else:
			sources = []
		return SourcedString(result, sources)


	WHITESPACE_RE = re.compile(r"\s+")
	def split(self, sep=WHITESPACE_RE, maxsplit=None):
		if isinstance(sep, re.RegexObject):
			return sep.split(maxsplit)

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

	def rsplit(self, sep=WHITESPACE_RE, maxsplit=None):
		if isinstance(sep, re.RegexObject):
			parts = sep.split(maxsplit)
			if maxsplit:
				return parts[len(parts) - maxsplit:]
			else:
				return parts

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
		newString = SourcedString(reduce(unicode.__add__, substrings))
		
		lastOffset = 0
		sources = []
		for substring in substrings:
			if not isinstance(substring, SourcedString):
				substring = SourcedString(substring)
			for source in substring.sources:
				sources.append(Source(
						source=source.source,
						start=lastOffset,
						end=lastOffset + source.end - source.start,
						source_start=source.source_start,
						source_end=source.source_end
					))
				lastOffset += source.end - source.start
		newString.sources = sources

		return newString
	def __add__(self, other):
		return SourcedString.concat([self, other])
