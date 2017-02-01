import gate, sys

if __name__ == '__main__':
	gate = gate.Gate()
	gate.start()
	result = gate.loadAsJSON(sys.argv[1])
	gate.stop()
	with open(sys.argv[2], "w") as f:
		print >> f, result