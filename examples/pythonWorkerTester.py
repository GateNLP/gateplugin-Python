from py4j.java_gateway import JavaGateway, GatewayParameters
gateway = JavaGateway(gateway_parameters=GatewayParameters(port=25333))                  
doc1 = gateway.jvm.gate.Factory.newDocument("initial text")
print(doc1.getContent().toString())

doc2 = gateway.jvm.gate.plugin.python.PythonWorker.loadDocument("docs/doc1.xml")
print(doc2.getContent().toString())

js1 = gateway.jvm.gate.plugin.python.PythonWorjer.getBdocDocumentJson(doc2)
print(js1)


gateway.close()
