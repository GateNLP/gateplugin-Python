from __future__ import print_function

# https://docs.python.org/2.7/library/unittest.html
import unittest


if __package__:
    from . import sourcedstring
else:
    import sourcedstring


class TestSomething(unittest.TestCase):
    def test_split(self):
        s = sourcedstring.SimpleSourcedUnicodeString("banana", None)
        l = s.split("n")
        print(l)
        bs = begins(l)
        assert bs == [0, 3, 5]

    def test_split_default(self):
        s = sourcedstring.SimpleSourcedUnicodeString("a ban na", None)
        l = s.split()
        print(l)
        bs = begins(l)
        assert bs == [0, 2, 6]

    def test_rsplit(self):
        s = sourcedstring.SimpleSourcedUnicodeString("banana", None)
        l = s.rsplit("n")
        print(l)
        bs = begins(l)
        assert bs == [0, 3, 5]

    def test_rsplit_0(self):
        s = sourcedstring.SimpleSourcedUnicodeString("banana", None)
        l = s.rsplit("n", 0)
        print(l)
        bs = begins(l)
        self.assertEqual(bs, [0])

    def test_rsplit_default(self):
        s = sourcedstring.SimpleSourcedUnicodeString("b n na", None)
        l = s.rsplit()
        print(l)
        bs = begins(l)
        assert bs == [0, 2, 4]


def begins(seq):
    """
    Collect all the .begin values for a sequence of sourced strings.
    """
    return [ss.source.begin for ss in seq]


# A series of parameterised tests
# See https://stackoverflow.com/a/32939/242457

def instantiate_test_split(n):
    def test_split_n(self):
        s = sourcedstring.SimpleSourcedUnicodeString("banana", None)
        l = s.split("n", n)
        print(l)
        bs = begins(l)
        self.assertEqual(bs, [0] + [3, 5][:n])
    return test_split_n


def instantiate_test_rsplit(n):
    def test_rsplit_n(self):
        s = sourcedstring.SimpleSourcedUnicodeString("banana", None)
        l = s.rsplit("n", n)
        print(l)
        bs = begins(l)
        self.assertEqual(bs, [0] + [3, 5][-n:])
    return test_rsplit_n


for n in range(4):
    name = "test_split_n_{}".format(n)
    test_fun = instantiate_test_split(n)
    setattr(TestSomething, name, test_fun)

for n in range(1, 4):
    name = "test_rsplit_n_{}".format(n)
    test_fun = instantiate_test_rsplit(n)
    setattr(TestSomething, name, test_fun)
