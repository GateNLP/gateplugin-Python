# Ready Made Pipeline python-spacy


After loading the Python plugin, the prepared pipeline `python-spacy` is available from Applications - Ready Made Applications - Python - python-spacy.

This pipeline allows to annotate a document using the [SpaCy](https://spacy.io/) NLP software. 

In order to use this pipeline, the following requirements must be met:

* Python package [spacy](https://pypi.org/project/spacy/) must be installed
* The SpaCy model for the language must be installed


## Installing SpaCy

Run the following command:

```
python -m pip install -U spacy
```

## Install the Model for the Language

See the [SpaCy documentation for this](https://spacy.io/models). 
For example to install the default English model run the following command:

```
python -m spacy download en_core_web_sm
```

## Running the Pipeline

The following parameters in the  can be set in the programParams runtime parameter for the ready made pipeline: 

* `spacyModel`: the name of the model to use, e.g. `es_core_news_sm`. Default is: `en_core_web_sm`

