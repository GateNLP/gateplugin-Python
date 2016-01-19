from document import Document
from pprint import pprint
import json
import nltk
import sys

with open(sys.argv[1]) as f:
	js = json.load(f)

logger, doc = Document.load(js)

NLTK = doc.annotationSets["NLTK"]

m = NLTK.getType("Token")
for annotation in m:
	try:
		annotation.features["a_new_feature"] = "someValue3"
		del annotation.features["feature1"]
		annotation.features["feature2"] = "aTotallyNewValue"
	except KeyError:
		pass

m = NLTK.getType("Toke")
for annotation in m:
	annotation.features.clear()


print json.dumps(logger)
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
