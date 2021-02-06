@Grab('uk.ac.gate:gate-core:8.5.1') import gate.*
import gate.creole.*

Gate.init();
// TODO: try to load from the properties file that we create ... ?
Gate.getCreoleRegister().registerPlugin(new Plugin.Maven("uk.ac.gate.plugins", "python", "2.4"));

parms = Factory.newFeatureMap();
lr = Factory.createResource("gate.plugin.python.PythonSlaveLr", parms)
