import unittest

try:
    import _sourcedstring as sourcedstring
except SyntaxError:
    # Assume that we get here when when we are in Python 2,
    # attempting to import the Python 3-only _sourcedstring
    import sourcedstring

class TestSomething(unittest.TestCase):
    def test_split(self):
        s = sourcedstring.SimpleSourcedUnicodeString("banana", None)
        l = s.split("n")
        print(l)
        assert l[0].source.begin == 0

    def test_split_default(self):
        s = sourcedstring.SimpleSourcedUnicodeString("a ban na", None)
        l = s.split()
        print(l)
        assert l[1].source.begin == 2

    def test_rsplit(self):
        s = sourcedstring.SimpleSourcedUnicodeString("banana", None)
        l = s.rsplit("n")
        print(l)
        assert l[0].source.begin == 0

    def test_rsplit_default(self):
        s = sourcedstring.SimpleSourcedUnicodeString("b n na", None)
        l = s.rsplit()
        print(l)
        assert l[1].source.begin == 2
