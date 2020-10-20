# Ready Made Pipeline python-stanford-stanza




After loading the Python plugin, the prepared pipeline `python-stanza` is available from Applications - Ready Made Applications - Python - python-stanford-stanza.

This pipeline allows to annotate a document using the [Stanford Stanza](https://stanfordnlp.github.io/stanza/) NLP software. 

In order to use this pipeline, the following requirements must be met:

* Python package [stanza](https://pypi.org/project/stanza/) must be installed
* The Stanza model for the desired language must be installed


## Installing SpaCy

Run the following command:

```
python -m pip install -U stanza
```

## Install the Model for the Language

See the [Stanza documentation for this](https://stanfordnlp.github.io/stanza/available_models.html) and [Download Models](https://stanfordnlp.github.io/stanza/download_models.html)
For example to install the default English model run the following command:

```
python  -c 'import stanza; stanza.download("en")'~~~~
```

## Running the Pipeline

The following parameters in the  can be set in the programParams runtime parameter for the ready made pipeline.  This is a subset of what can be specified directly in Python when a Stanza Pipeline is created, see the [Stanza Documentation](https://stanfordnlp.github.io/stanza/pipeline.html)

* `lang`: the language for the model to use, e.g. `en`. Default is: `en`
* `dir`: directory where the downloaded model is stored. If not specified the default download location is used.
* `logging_level`: one of "DEBUG", "INFO", "WARN", "ERROR", "CRITICAL", "FATAL", default depends on `verbose`, see below
* `verbose`: if `True`, but now logging level is specified,  loggin level is set to `INFO`, otherwise to`ERROR`
* `use_gpu`: if `True`, attemt to use the GPU, if available. Set to `False` to avoid using the GPU if one is available.