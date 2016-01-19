from document import Document
from pprint import pprint
import json
import sys


test_log = open("/Users/dominic/Desktop/test_log.txt","a")
# js = json.load(sys.stdin)
line = sys.stdin.readline().strip()
while line:
	print >> test_log, line

	if line:
		js = json.loads(line)

		print >> test_log, js
		test_log.flush()

		logger, doc = Document.load(js)


		NLTK = doc.annotationSets[""]

		m = NLTK.getType("Token")
		for annotation in m:
			try:
				# annotation.features["a_new_feature"] = "someValue3"
				annotation.features["feature2"] = "aTotallyNewValue"
				del annotation.features["a_new_feature"]
			except KeyError:
				pass


		print json.dumps(logger)
		print ""
		sys.stdout.flush()

		line = sys.stdin.readline().strip()

# Mentions = doc.annotationSets["Mentions"]

# userIDs = Mentions.getType("UserID")

# for userID in userIDs:
# 	tokens = Mentions.get(userID.start, userID.end).getType("Token")
# 	print tokens
# tokenizer = nltk.tokenize.TweetTokenizer()

# tokens = tokenizer.tokenize(doc.text)
# tokens = nltk.pos_tag(tokens)

# for token, tag in tokens:
# 	doc.annotationSets["NLTK"].add(token.source.begin, token.source.end, 
# 		"Token", {"string": token, "tag": tag})

# print json.dumps(logger)
# default_set = d.annotationSets[None]

# annotation = default_set.add(10, 30, "testAnnotation", {"a feature": 1, "another feature": []})
