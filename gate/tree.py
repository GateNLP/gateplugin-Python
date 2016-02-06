"""Basic Python implementation of Red-Black tree for annotation lookups.

Implementation based on pseudo-code from:

Cormen, Thomas H. Introduction to algorithms. MIT press, 2009."""

class SliceableTree(object):
	def __init__(self, values = [], compare = None):
		self.compare = compare
		if self.compare is None:
			self.compare = lambda a, b: a - b
		
		value_iter = iter(values)
		self.root = None

		for value in value_iter:
			self.insert(value)

	class Node(object):
		BLACK = False
		RED = True

		def __init__(self, value, parent = None, left = None, right = None, colour = RED):
			self.value = value
			self.left = left 
			self.right = right 
			self.parent = parent
			self.colour = colour


		def grandparent(self):
			if self.parent != None:
				return self.parent.parent
			else:
				return None

		def auntie(self):
			grandparent = self.grandparent()

			if grandparent:
				if grandparent.left == self.parent:
					return grandparent.right
				else:
					return grandparent.left

		def is_left(self):
			return self.parent and self.parent.left == self

		def is_right(self):
			return self.parent and self.parent.right == self


		def par_colours(self, node):
			if node.parent:
				return [node.colour] + self.par_colours(node.parent)
			else:
				return [node.colour]

	ROOT_NODE = -1

	def insert(self, value):
		y = None
		x = self.root

		while x is not None:
			y = x
			if self.compare(value, x.value) < 0:
				x = x.left
			else:
				x = x.right

		z = self.Node(value, y)

		if y is None:
			self.root = z
		elif self.compare(value, y.value) < 0:
			y.left = z
		else:
			y.right = z

		self.insert_fixup(z)


	def insert_fixup(self, z):
		while z.parent and z.parent.colour is z.RED:
			if z.parent == z.parent.parent.left:
				y = z.parent.parent.right
				if y and y.colour is z.RED:
					z.parent.colour = z.BLACK
					y.colour = z.BLACK
					z.parent.parent.colour = z.RED
					z = z.parent.parent
				else:
					if z == z.parent.right:
						z = z.parent
						self._rotate_left(z)
					z.parent.colour = z.BLACK
					z.parent.parent.colour = z.RED
					self._rotate_right(z.parent.parent)
			else:
				y = z.parent.parent.left
				if y and y.colour is z.RED:
					z.parent.colour = z.BLACK
					y.colour = z.BLACK
					z.parent.parent.colour = z.RED
					z = z.parent.parent
				else:
					if z == z.parent.left:
						z = z.parent
						self._rotate_right(z)
					z.parent.colour = z.BLACK
					z.parent.parent.colour = z.RED
					self._rotate_left(z.parent.parent)
		self.root.colour = z.BLACK

	def sanity_test(self):
		is_sane = True
		seen_nodes = set()

		iter_stack = []
		current_node = self.root

		while iter_stack or current_node:
			if current_node:
				iter_stack.append(current_node)
				current_node = current_node.left
			else:
				current_node = iter_stack.pop(-1)
				
				if current_node:
					if current_node in seen_nodes:
						return False
					else:
						seen_nodes.add(current_node)

					if current_node.left and current_node.left.parent != current_node:
						return False
					if current_node.right and current_node.right.parent != current_node:
						return False

					colours = current_node.par_colours(current_node)
					last_colour = not colours[0]
					for colour in colours:
						if colour == last_colour:
							print colours
							return False
				current_node = current_node.right

		return True

	def print_colours(self, node = None, depth = 0):
		if node is None:
			node = self.root

		print " " * depth + ("B" if node.colour is node.BLACK else "R")

		if node.left:
			self.print_colours(node.left, depth + 1)
		if node.right:
			self.print_colours(node.right, depth + 1)


	def _rotate_right(self, x):
		y = x.left
		x.left = y.right

		if y.right is not None:
			y.right.parent = x

		y.parent = x.parent

		if x.parent is None:
			self.root = y
		elif x == x.parent.right:
			x.parent.right = y
		else:
			x.parent.left = y

		y.right = x
		x.parent = y


	def _rotate_left(self, x):
		y = x.right
		x.right = y.left

		if y.left is not None:
			y.left.parent = x

		y.parent = x.parent

		if x.parent is None:
			self.root = y
		elif x == x.parent.left:
			x.parent.left = y
		else:
			x.parent.right = y

		y.left = x
		x.parent = y

	def __iter__(self):
		iter_stack = []
		current_node = self.root

		while (iter_stack or current_node):
			if current_node:
				iter_stack.append(current_node)
				current_node = current_node.left
			else:
				current_node = iter_stack.pop(-1)
				yield current_node.value
				current_node = current_node.right

	def __getitem__(self, key):
		if isinstance(key, slice):
			return self._slice(key.start, key.stop)
		else:
			current_node = self.root

			while current_node is not None:
				comp_val = self.compare(key, current_node.value)

				if comp_val == 0:
					return current_node.value
				elif comp_val < 0:
					current_node = current_node.left
				else:
					current_node = current_node.right

			return None

	def _slice(self, left, right):
		newTree = SliceableTree([], self.compare)

		# Deal with edge cases.
		if self.compare(left, right) > 0:
			left, right = right, left

		iter_stack = []
		current_node = self.root
		self.iteration_counter = 0
		while (iter_stack or current_node):
			self.iteration_counter += 1
			if current_node:
				iter_stack.append(current_node)

				if self.compare(left, current_node.value) <= 0:
					current_node = current_node.left
				else:
					current_node = None
			else:
				current_node = iter_stack.pop(-1)

				if self.compare(right, current_node.value) >= 0:
					if (self.compare(left, current_node.value) <= 0):
						newTree.insert(current_node.value)

					current_node = current_node.right
				else:
					current_node = None

		return newTree


if __name__ == "__main__":
	from annotation_set import SearchOffset
	from annotation import Annotation

	def compare_start(a, b):
		return a.start - b.start

	def compare_end(a, b):
		return a.end - b.end

	class Index(object):
		def __init__(self, start = None, end = None):
			self.start = start
			self.end = end

	import random, avl
	annotations_list = []
	search_for_annot = None
	for i in range(50):
		left = random.randint(0, 100)
		right = random.randint(min(left, 99)+1, 100)

		if i == 4:
			search_for_annot = Annotation(None, None, 1, "TestAnnot", left, 0, {})

		annotations_list.append(Annotation(None, None, 1, "TestAnnot", left, right, {}))

	start_tree = SliceableTree(annotations_list, compare_start)
	print "Sanity:",start_tree.sanity_test()
	start_tree.print_colours()
	ordered_list = [a.start for a in start_tree]
	avl_start_tree = avl.new(source = annotations_list, compare = compare_start)
	print ordered_list
	# import ipdb; ipdb.set_trace()

	iteration_counts = []
	for i in range(1000):
		left = random.randint(0, 100)
		right = random.randint(0, 100)
		list_slice = [v for v in ordered_list if v >= left and v <= right]
		tree_slice = [a.start for a in start_tree[Index(start = left):Index(start = right)]]
		iteration_counts.append(start_tree.iteration_counter)

		lower, upper = avl_start_tree.span(Index(start = left), Index(start = right))
		avl_slice = [a.start for a in avl_start_tree[lower:upper]]

		if list_slice != tree_slice and left < right: # My naive slicing here doesn't work with reversed indices
			print "BROKEN:"
			print ordered_list
			print left, right
			print "Expected:", list_slice
			print "Got:",tree_slice


		if avl_slice != tree_slice:
			print "BROKEN:"
			print ordered_list
			print left, right
			print "Expected:", avl_slice
			print "Got:",tree_slice

	print float(sum(iteration_counts))/len(iteration_counts)
	# end_tree = build(annotations_list, compare_end)

	# import ipdb; ipdb.set_trace()