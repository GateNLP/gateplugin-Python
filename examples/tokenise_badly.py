import gate, sys

@gate.executable
def tokenize(document, outputAS):
	for token in document.text.split(" "):
		document.annotationSets[""].add(token.source.begin,
			token.source.end,
			"Token",
			{"string":"FUN"})

	return document

if __name__ == "__main__":
	tokenize.start()
