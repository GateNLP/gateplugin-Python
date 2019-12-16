"""Run spacy"""

from gatenlp import interact, GateNlpPr, Document
import sys
import re
import spacy

@GateNlpPr
class MyProcessor:

  def __init__(self):
    self.nlp = None
    self.tokens_total = 0
    self.nr_docs = 0
  def start(self, **kwargs):
    if "spacyModel" in kwargs:
        self.nlp = spacy.load(kwargs.get("spacyModel"))
    else:
        self.nlp = spacy.load("en_core_web_sm")
    self.tokens_total = 0
    self.nr_docs = 0
  def finish(self, **kwargs):
    print("Total number of tokens:", self.tokens_total)
    print("Number of documents:", self.nr_docs)
  def __call__(self, doc, **kwargs):
    if "outputAnnotationSet" in kwargs:
        set1 =  doc.get_annotations(kwargs.get("outputAnnotationSet"))
    else:
        set1 = doc.get_annotations()
    set1.clear()   
    text = doc.text  
    doc = self.nlp(text)
    for tok in doc:
        from_off = tok.idx
        to_off = tok.idx + len(tok)
        is_space = tok.is_space
        if not is_space:
            fm = {
                    "ent_type": tok.ent_type_,
                    "dep": tok.dep_,
                    "is_alpha": tok.is_alpha,
                    "is_bracket": tok.is_bracket,
                    "is_currency": tok.is_currency,
                    "is_digit": tok.is_digit,
                    "is_left_punct": tok.is_left_punct,
                    "is_lower": tok.is_lower,
                    "is_oov": tok.is_oov,
                    "is_punct": tok.is_punct,
                    "is_quote": tok.is_quote,
                    "is_right_punct": tok.is_right_punct,
                    "is_sent_start": tok.is_sent_start,
                    "is_space": tok.is_space,
                    "is_stop": tok.is_stop,
                    "is_title": tok.is_title,
                    "is_upper": tok.is_upper,
                    "lang": tok.lang_,
                    "lemma": tok.lemma_,
                    "like_email": tok.like_email,
                    "like_num": tok.like_num,
                    "like_url": tok.like_url,
                    "orth": tok.orth,
                    "pos": tok.pos_,
                    "prefix": tok.prefix_,
                    "prob": tok.prob,
                    "rank": tok.rank,
                    "sentiment": tok.sentiment,
                    "shape": tok.shape_,
                    "suffix": tok.suffix_,
            }
            set1.add(from_off, to_off, "Token", fm)  
            ws = tok.whitespace_
            # TODO: if ws is not length 0, could add space token here
        else:
            pass # could add space token here
        for ent in doc.ents:
            set1.add(ent.start_char, ent.end_char, ent.label_, {"lemma": ent.lemma_})
    self.tokens_total += len(doc)    
    self.nr_docs += 1
    
    
if __name__ == '__main__':
  interact()
