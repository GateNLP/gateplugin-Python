# GATE Python Plugin

User Documentation: http://gatenlp.github.io/gateplugin-python/

## Building

You'll need `git` and Apache Maven and a bunch of other stuff probably.

As per "the usual GATE developer way" you will need to
[add repo.gate.ac.uk] to your Maven `settings.xml` file.
See [GATE 8.5 development documentation] for a suggested `settings.xml` file.

Clone the repo and update its submodules:

    git clone https://github.com/GateNLP/gateplugin-python
    cd gateplugin-python
    git submodule update --init

Compile using Apache Maven:

    mvn compile

