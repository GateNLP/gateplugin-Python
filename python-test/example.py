from gate import executable
import twokenize, sys, codecs

@executable
def tokenize(document, outputAS, addText):
	document.text.split(" ")

	for token in document.text.split(" "):
		outputAS.add(token.source.begin, 
			token.source.end, 
			"Token", 
			{"string":token + addText})

	return document
		
if __name__ == "__main__":
	tokenize.start()
