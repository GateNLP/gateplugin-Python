/**
*   This is an initial prototype of the python integration, intended to be run as a scriptable PR.
*   It is very limited and will start a new process for each document. Use the Java PR instead..
*/

import gate.corpora.DocumentJsonUtils
import static groovy.json.JsonOutput.*
import groovy.json.JsonSlurper
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;


outputFile = new File(scriptParams.outputFolder + "/" + doc.name)


JsonFactory factory = new JsonFactory()

outputFile.withOutputStream { out ->

    allAnnotations = [:]

    doc.getAnnotationSetNames().each { annotationSetName ->

        annotationSet = doc.getAnnotations(annotationSetName)

        allAnnotations.putAll(annotationSet.groupBy {
            (annotationSetName ?: '') + ':' + it.type
        })
    }

    JsonGenerator jsonG = factory.createGenerator(out)
    try {
      DocumentJsonUtils.writeDocument(doc, 0, doc.end(), allAnnotations, null, null,
          "annotationID", jsonG)
    } finally {
        jsonG.close()
    }
}

def process = ("/usr/local/bin/python "+ scriptParams.scriptLocation +" "+ outputFile.absolutePath).execute() 

output = process.in.getText("UTF-8")
process.waitFor()

changes = new JsonSlurper().parseText(output)

changes.each { change ->
    println change
    if (change[0] == "ADD_ANNOT") {
        doc.getAnnotations(change[1]).add(change[2] as long, change[3] as long, change[4], change[5].toFeatureMap())
    }

    if (change[0] == "REMOVE_ANNOT") {
        annotation = doc.getAnnotations(change[1]).get(change[2])
        doc.getAnnotations(change[1]).remove(annotation)
    }

    if (change[0] == "UPDATE_FEATURE") {
        doc.getAnnotations(change[1]).get(change[2]).features.put(change[3], change[4])
    }

    if (change[0] == "CLEAR_FEATURES") {
        doc.getAnnotations(change[1]).get(change[2]).features.clear()
    }
    
    if (change[0] == "REMOVE_FEATURE") {
        doc.getAnnotations(change[1]).get(change[2]).features.remove(change[3])
    }

}