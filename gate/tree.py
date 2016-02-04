"""Basic Python implementation of Red-Black tree for annotation lookups"""

class RBTreeNode(object):
	def __init__(self, value, parent, left = None, right = None):
		self.value = value
		self.left = left
		self.right = right
		self.parent = parent

def search(key, node, comp):
	current_node = node

	while current_node is not None:
		comp_val = comp(key, current_node.value)

		if comp_val == 0:
			return current_node
		elif comp_val < 0:
			current_node = current_node.left
		else:
			current_node = current_node.right

	return None

def insert(parent, value, comp):
	if parent is None:
		return RBTreeNode(value, parent, None, None)

	comp_val = comp(value, parent.value)

	if comp_val == 0:
		return RBTreeNode(value, parent, parent.left, parent.right)
	elif comp_val < -1:
		return RBTreeNode(value, parent, insert(parent.left, value, comp), parent.right)
	else:
		return RBTreeNode(value, parent, parent.left, insert(parent.right, value, comp))

def build(values, comp):
	root = None

	for value in values:
		root = insert(root, value, comp)

	return root

# def slice(left_key, right_key, node, comp):
# 	current_node = search(left_key, node, comp)

# 	result = []
# 	while current_node is not None and comp(right_key, current_node) >= 0:
# 		result += current_node

# 		if current_node.right:
# 			current_node = current_node.right
# 		elif current_node.parent and current_node.parent.
# 	return result


if __name__ == "__main__":
	from annotation_set import SearchOffset
	from annotation import Annotation

	def compare_start(a, b):
		return a.start - b.start

	def compare_end(a, b):
		return a.end - b.end

	annotations_list = [
		Annotation(None, None, 1, "TestAnnot", 0, 10, {}),
		Annotation(None, None, 1, "TestAnnot", 5, 15, {}),
		Annotation(None, None, 1, "TestAnnot", 10, 17, {}),
		Annotation(None, None, 1, "TestAnnot", 12, 18, {}),
		Annotation(None, None, 1, "TestAnnot", 19, 30, {})
	]
	start_tree = build(annotations_list, compare_start)
	end_tree = build(annotations_list, compare_end)

	import ipdb; ipdb.set_trace()