"""Basic Python implementation of Red-Black tree for annotation lookups"""
import avl

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

		def __init__(self, value, parent = None, left = None, right = None, colour = BLACK):
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
				if grandparent.left == self:
					return grandparent.right
				else:
					return grandparent.left

		def rotate_left(self):
			grandparent = self.grandparent()
			grandparent.left, self.left, grandparent.left.right = self, grandparent.left, self.left

		def rotate_right(self):
			grandparent = self.grandparent()
			grandparent.right, self.right, grandparent.right.left = self, grandparent.right, self.right


		def post_insert(self):
			node = self

			auntie = node.auntie()
			grandparent = node.grandparent()
			
			if node.parent is None:
				node.colour = node.BLACK
			elif node.parent.colour == node.BLACK:
				pass # DO nothing, tree is valid.
			elif auntie and auntie.colour == node.RED:
				node.parent.colour = node.BLACK
				node.colour = node.BLACK
				grandparent.colour = node.RED 
				# WE may have broken the tree doing this, so do this again recursively.
				grandparent.post_insert()
			else:
				import ipdb; ipdb.set_trace()
				if node == node.parent.right and node.parent == grandparent.left: # Rotate to prevent unbalancing.
					node.parent.rotate_left()
					node = node.left
				elif node == node.parent.left and node.parent == grandparent.right:
					node.parent.rotate_right()
					node = node.right

				auntie = node.auntie()
				grandparent = node.grandparent()

				node.parent.colour = node.BLACK
				grandparent.colour = node.RED

				if (node == node.parent.left):
					grandparent.rotate_left()
				else:
					grandparent.rotate_right

				
	ROOT_NODE = -1
	def insert(self, value, target = ROOT_NODE):
		if (target is self.ROOT_NODE):
			target = self.root

		if target is None:
			node = self.Node(value, None, colour = self.Node.RED)
		elif self.compare(value, target.value) < 0:
			if target.left is None:
				node = self.Node(value, target, colour = self.Node.RED)
				target.left = node
			else:
				node = self.insert(value, target.left)
		else:
			if target.right is None:
				node = self.Node(value, target, colour = self.Node.RED)
				target.right = node
			else:
				node = self.insert(value, target.right)

		if self.root is None:
			self.root = node

		node.post_insert()

		return node


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

	import random
	annotations_list = []
	search_for_annot = None
	for i in range(50):
		left = random.randint(0, 100)
		right = random.randint(min(left, 99)+1, 100)

		if i == 4:
			search_for_annot = Annotation(None, None, 1, "TestAnnot", left, 0, {})

		annotations_list.append(Annotation(None, None, 1, "TestAnnot", left, right, {}))

	start_tree = SliceableTree(annotations_list, compare_start)
	ordered_list = [a.start for a in start_tree]
	avl_start_tree = avl.new(source = annotations_list, compare = compare_start)

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