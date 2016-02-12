"""Basic Python implementation of Red-Black tree for annotation lookups.

This code still contains a lot of debugging apparatus, because I don't quite trust it yet.

Implementation based on pseudo-code from:

Cormen, Thomas H. Introduction to algorithms. MIT press, 2009."""

import itertools

BLACK = False # These aren't settings, they're constants to help readability.
RED = True
CHECK_SANITY = False

def check_sane(function):
	"""This is useful for debugging - will help to identify which function ruins things"""
	if not CHECK_SANITY:
		return function

	def do_check(self, *args, **kwargs):
		start_sanity = self.sanity_test()
		result = function(self, *args,**kwargs)
		if start_sanity and not self.sanity_test():
			print "Sanity broken by function", repr(function)
			import ipdb; ipdb.set_trace()
		else:
			return result

	return do_check


class SliceableTree(object):
	"""A binary search tree with additional support for slicing and nearest miss finding.

	This tree expects items to be unique (by the standard python semantics). Duplicate items will not
	be added."""
	def __init__(self, values = [], compare = None):
		self.compare = compare
		if self.compare is None:
			self.compare = lambda a, b: a - b
		
		value_iter = iter(values)
		self.root = None

		for value in value_iter:
			self.insert(value)

	class Node(object):
		def __init__(self, value, parent = None, left = None, right = None, colour = RED):
			self.value = value
			self.values = [value]
			self.left = left if left else SliceableTree.nil
			self.right = right if right else SliceableTree.nil
			self.parent = parent if parent else SliceableTree.nil
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

	class Leaf(Node):
		def __init__(self):
			self.left = self
			self.right = self
			self.parent = self
			self.colour = BLACK
			self.values = []
			self.value = None

		def __nonzero__(self):
			return False

	nil = Leaf()

	@check_sane
	def insert(self, value):
		x = self.root
		y = x

		x_comp = self.compare(value, x.value) if x else 0

		while x and x_comp != 0:
			x_comp = self.compare(value, x.value)
			y = x
			if x_comp < 0:
				x = x.left
			elif x_comp > 0:
				x = x.right


		if y and self.compare(value, y.value) == 0:
			if value not in y.values: # Don't allow duplicate items in the tree
				y.values.append(value)
		else:
			z = self.Node(value, y)

			if not y:
				self.root = z
			elif self.compare(value, y.value) < 0:
				y.left = z
			else:
				y.right = z

			self._insert_fixup(z)

	@check_sane
	def _insert_fixup(self, z):
		while z.parent and z.parent.colour is RED:
			if z.parent == z.parent.parent.left:
				y = z.parent.parent.right
				if y and y.colour is RED:
					z.parent.colour = BLACK
					y.colour = BLACK
					z.parent.parent.colour = RED
					z = z.parent.parent
				else:
					if z == z.parent.right:
						z = z.parent
						self._rotate_left(z)
					z.parent.colour = BLACK
					z.parent.parent.colour = RED
					self._rotate_right(z.parent.parent)
			else:
				y = z.parent.parent.left
				if y and y.colour is RED:
					z.parent.colour = BLACK
					y.colour = BLACK
					z.parent.parent.colour = RED
					z = z.parent.parent
				else:
					if z == z.parent.left:
						z = z.parent
						self._rotate_right(z)
					z.parent.colour = BLACK
					z.parent.parent.colour = RED
					self._rotate_left(z.parent.parent)
		self.root.colour = BLACK

	@check_sane
	def _transplant(self, u, v):
		"""Swaps the positions in the tree of u and v"""
		if self.root == u:
			self.root = v
		elif u == u.parent.left:
			u.parent.left = v
		else:
			u.parent.right = v
		v.parent = u.parent

	def min(self):
		node = self._min_node()
		return node.value if node else None

	def _min_node(self, x = None):
		x = x if x else self.root

		while x.left:
			x = x.left
		return x

	def max(self):
		node = self._max_node()
		return node.value if node else None


	def _max_node(self, x = None):
		x = x if x else self.root

		while x.right:
			x = x.right
		return x

	@check_sane
	def _delete_node(self, z):
		y = z 
		y_orig_colour = y.colour

		if not z.left:
			x = z.right
			self._transplant(z, z.right)
		elif not z.right:
			x = z.left
			self._transplant(z, z.left)
		else:
			y = self._min_node(z.right)
			y_orig_colour = y.colour
			x = y.right

			if y.parent == z:
				x.parent = y
			else:
				self._transplant(y, y.right)
				y.right = z.right
				y.right.parent = y
			self._transplant(z, y)
			y.left = z.left
			y.left.parent = y
			y.colour = z.colour

		if y_orig_colour == BLACK:
			self._delete_fixup(x)

	@check_sane
	def _delete_fixup(self, x):
		start_size = len(list(self))
		while x != self.root and x.colour == BLACK:
			if len(list(self)) != start_size: import ipdb;ipdb.set_trace()
			if x == x.parent.left:
				w = x.parent.right

				if w.colour == RED:
					w.colour = BLACK
					x.parent.colour = RED
					self._rotate_left(x.parent)
					w = x.parent.right
					if len(list(self)) != start_size: import ipdb;ipdb.set_trace()

				if w.left.colour == BLACK and w.right.colour == BLACK:
					w.colour = RED
					x = x.parent
				else:
					if w.right.colour == BLACK:
						w.left.colour = BLACK
						w.colour = RED
						self._rotate_right(w)
						if len(list(self)) != start_size: import ipdb;ipdb.set_trace()
						w = x.parent.right
					w.colour = x.parent.colour
					x.parent.colour = BLACK
					w.right.colour = BLACK
					self._rotate_left(x.parent)
					if len(list(self)) != start_size: import ipdb;ipdb.set_trace()

					x = self.root
			else:
				w = x.parent.left

				if w.colour == RED:
					w.colour = BLACK
					x.parent.colour = RED
					if len(list(self)) != start_size: import ipdb;ipdb.set_trace()
					self._rotate_right(x.parent)
					if len(list(self)) != start_size: import ipdb;ipdb.set_trace()
					w = x.parent.left
				if w.right.colour == BLACK and w.left.colour == BLACK:
					w.colour = RED
					x = x.parent
					if len(list(self)) != start_size: import ipdb;ipdb.set_trace()
				else:
					if w.left.colour == BLACK:
						w.right.colour = BLACK
						w.colour = RED
						self._rotate_left(w)
						if len(list(self)) != start_size: import ipdb;ipdb.set_trace()
						w = x.parent.left
					w.colour = x.parent.colour
					x.parent.colour = BLACK
					w.left.colour = BLACK
					self._rotate_right(x.parent)
					if len(list(self)) != start_size: import ipdb;ipdb.set_trace()
					x = self.root		
					if len(list(self)) != start_size: import ipdb;ipdb.set_trace()		
		x.colour = BLACK
		if len(list(self)) != start_size: import ipdb;ipdb.set_trace()

	@check_sane
	def remove(self, value):
		node  = self._search_node(value)
		if node:
			if len(node.values) == 1:
				self._delete_node(node)
			else:
				node.values.remove(value)
		else:
			raise ValueError("Value is not in tree")

	def sanity_test(self):
		is_sane = True
		if not self.root:
			return True
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
						print "Seen current node twice"
						return False
					else:
						seen_nodes.add(current_node)

					if current_node.left and current_node.left.parent != current_node:
						print "Node %s thinks its parent is %s, but it's actually %s (left)." % \
							(current_node.value, current_node.parent.value, current_node.left.value) 
						return False
					if current_node.right and current_node.right.parent != current_node:
						print "Node %s thinks its parent is %s, but it's actually %s (right)." % \
							(current_node.value, current_node.parent.value, current_node.right.value) 

						return False

					# colours = current_node.par_colours(current_node)
					# last_colour = not colours[0]
					# for colour in colours:
					# 	if colour == last_colour:
					# 		return False
				current_node = current_node.right

		return True

	def print_colours(self, node = None, depth = 0):
		if not node:
			node = self.root

		print " " * depth + ("B" if node.colour is BLACK else "R")

		if node.left:
			self.print_colours(node.left, depth + 1)
		if node.right:
			self.print_colours(node.right, depth + 1)

	def __str__(self, node = None, depth = 0):
		if not node:
			node = self.root

		_buffer = " " * depth + repr(node.value) + "\n"

		if node.left:
			_buffer += self.__str__(node.left, depth + 1)
		if node.right:
			_buffer += self.__str__(node.right, depth + 1)

		return _buffer

	@check_sane
	def _rotate_right(self, x):
		start_size = len(list(self))

		y = x.left
		x.left = y.right

		if y.right:
			y.right.parent = x

		y.parent = x.parent

		if not x.parent:
			self.root = y
		elif x == x.parent.right:
			x.parent.right = y
		else:
			x.parent.left = y

		y.right = x
		x.parent = y
		if len(list(self)) != start_size: import ipdb; ipdb.set_trace()

	@check_sane
	def _rotate_left(self, x):
		y = x.right
		x.right = y.left

		if y.left:
			y.left.parent = x

		y.parent = x.parent

		if self.root == x:
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

				for value in current_node.values:
					yield value

				current_node = current_node.right

	def __getitem__(self, key):
		if isinstance(key, slice):
			return self._slice(key.start, key.stop)
		else:
			node = self._search_node(key)
			if node:
				return node.values
			else:
				return []

	@check_sane
	def _search_node(self, value):
		current_node = self.root

		while current_node:
			comp_val = self.compare(value, current_node.value)

			if comp_val == 0:
				return current_node
			elif comp_val < 0:
				current_node = current_node.left
			else:
				current_node = current_node.right

		return None

	@check_sane
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
						for value in current_node.values:
							newTree.insert(value)

					current_node = current_node.right
				else:
					current_node = None

		return newTree

	def nearest_after(self, left):
		iter_stack = []

		current_node = self.root

		while (iter_stack or current_node):
			if current_node:
				iter_stack.append(current_node)
				current_node = current_node.left
			else:
				current_node = iter_stack.pop(-1)

				if (self.compare(left, current_node.value) <= 0):
					return current_node.values
						
				current_node = current_node.right

	def add(self, other):
		"""Adds this tree to the other tree to form a new tree"""
		return SliceableTree(itertools.chain(self, other), self.compare)


	__add__ = add

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

		def __repr__(self):
			return "%d:%d" % (self.start, self.end)

	import sys

	def save_sequence(filename, to_delete_idx, original_annotation_list):
		with open(sys.argv[1], "w") as f:
			print >> f, ",".join(map(str, to_delete_idx))
			print >> f, ",".join(map(repr, original_annotation_list))

	def replay_sequence(filename):
		with open(filename) as f:
			to_delete, original_annotations_list = f.readlines()

			annotations_list = [Index(*map(int, annot.split(":"))) for annot in original_annotations_list.split(",")]
			to_delete = [(int(annot), annotations_list[int(annot)]) for annot in to_delete.split(",")]
			to_delete = sorted(to_delete, reverse = True)
			print to_delete
			start_tree = SliceableTree(annotations_list, compare_start)

			for index, annotation in to_delete:

				del annotations_list[index]

				if not start_tree.sanity_test():
					print "Tree is insane prior to copying"
				old_start_tree = SliceableTree(start_tree, compare_start)
				start_tree.remove(annotation)

				if sorted(annotations_list, key=lambda x: x.start) != list(start_tree):
					print "Expected:".ljust(10), sorted(annotations_list, key=lambda x: x.start)
					print "Got:".ljust(10), list(start_tree)
					print "Failed to remove:", annotation
					import ipdb; ipdb.set_trace()


	if len(sys.argv) == 3:
		replay_sequence(sys.argv[2])
		sys.exit()

	import random, avl
	for i in range(100):
		annotations_list = []
		search_for_annot = None
		for i in range(100):
			left = random.randint(0, 100)
			right = random.randint(min(left, 99)+1, 100)

			if i == 4:
				search_for_annot = Index(left, 0)

			annotations_list.append(Index(left, right))


		to_delete = random.sample(list(enumerate(annotations_list)), 10) # Choose ten random nodes to remove.
		to_delete = sorted(to_delete, reverse = True)
		save_sequence(sys.argv[1], [x for (x,y) in to_delete], annotations_list)

		start_tree = SliceableTree(annotations_list, compare_start)

		for index, annotation in to_delete:
			# print "Current list:", sorted(annotations_list, key=lambda x: x.start)
			# print "Current tree:", list(start_tree)
			# print "Deleting %s (%s):" % (annotation, annotations_list[index])
			del annotations_list[index]
			if not start_tree.sanity_test():
				print "Tree is insane prior to copying"
			old_start_tree = SliceableTree(start_tree, compare_start)
			start_tree.delete(annotation)

			if sorted(annotations_list, key=lambda x: x.start) != list(start_tree):
				print "Expected:".ljust(10), sorted(annotations_list, key=lambda x: x.start)
				print "Got:".ljust(10), list(start_tree)
				print "Failed to remove:", annotation
				import ipdb; ipdb.set_trace()
			# print "New list:", sorted(annotations_list, key=lambda x: x.start)
			# print "New tree:", list(start_tree)

		# start_tree.print_colours()
		ordered_list = [a.start for a in start_tree]
		avl_start_tree = avl.new(source = annotations_list, compare = compare_start)
		print "Searching in list", ordered_list
		# import ipdb; ipdb.set_trace()

		iteration_counts = []
		for i in range(100):
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
				print "Expected:".ljust(10), list_slice
				print "Got:".ljust(10),tree_slice


			if avl_slice != tree_slice:
				print "BROKEN:"
				print ordered_list
				print left, right
				print "Expected:".ljust(10), avl_slice
				print "Got:".ljust(10),tree_slice

		print "Done! Iterations needed:", float(sum(iteration_counts))/len(iteration_counts)
		# end_tree = build(annotations_list, compare_end)

		# import ipdb; ipdb.set_trace()