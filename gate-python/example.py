from document import Document
from pprint import pprint
import json, time
import sys
# import nltk

time.sleep(5)

test_log = open("/Users/dominic/Desktop/test_log.txt","a")
# js = json.load(sys.stdin)
line = sys.stdin.readline().strip()
while line:
	print >> test_log, line

	if line:
		js = json.loads(line)

		test_log.flush()

		logger, doc = Document.load(js)

		# tokenizer = nltk.tokenize.TweetTokenizer()
		# tokens = tokenizer.tokenize(doc.text)
		# tokens = nltk.pos_tag(tokens)

		# for token, tag in tokens:
		# 	doc.annotationSets["NLTK"].add(token.source.begin, token.source.end, 
		# 		"Token", {"string": token, "tag": tag})

		for token in doc.text.split(" "):
			doc.annotationSets["NLTK"].add(token.source.begin, token.source.end, "Token", {"string":token})
		print json.dumps(logger)
		print ""
		sys.stdout.flush()

		line = sys.stdin.readline().strip()


