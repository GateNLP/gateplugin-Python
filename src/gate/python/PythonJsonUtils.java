package gate.python;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gate.Annotation;
import gate.Document;
import gate.util.GateRuntimeException;
import gate.util.InvalidOffsetException;


/**
 * <p>
 * This class contains utility methods to output GATE documents in a
 * JSON format which is (deliberately) close to the format used by
 * Twitter to represent entities such as user mentions and hashtags in
 * Tweets. Unlike DocumentJsonUtils, no escaping takes place, making it 
 * easier to consume the document content in Python.
 * </p>
 * 
 * <pre>
 * {
 *   "text":"Text of the document",
 *   "entities":{
 *     "Person":[
 *       {
 *         "indices":[startOffset, endOffset],
 *         // other features here
 *       },
 *       { ... }
 *     ],
 *     "Location":[
 *       {
 *         "indices":[startOffset, endOffset],
 *         // other features here
 *       },
 *       { ... }
 *     ]
 *   }
 * }
 * </pre>
 * 
 * <p>
 * The document is represented as a JSON object with two properties,
 * "text" holding the text of the document and "entities" representing
 * the annotations. The "entities" property is an object mapping each
 * "annotation type" to an array of objects, one per annotation, that
 * holds the annotation's start and end offsets as a property "indices"
 * and the other features of the annotation as its remaining properties.
 * Features are serialized using Jackson's ObjectMapper, so
 * string-valued features become JSON strings, numeric features become
 * JSON numbers, Boolean features become JSON booleans, and other types
 * are serialized according to Jackson's normal rules (e.g. Map values
 * become nested JSON objects).
 * </p>
 * 
 * <p>
 * The grouping of annotations into blocks is the responsibility of the
 * caller - annotations are supplied as a Map&lt;String,
 * Collection&lt;Annotation&gt;&gt;, the map keys become the property
 * names within the "entities" object and the corresponding values
 * become the annotation arrays. In particular the actual annotation
 * type of an annotation within one of the collections is ignored - it
 * is allowed to mix annotations of different types within one
 * collection, the name of the group of annotations in the "entities"
 * object comes from the map key. However some overloadings of
 * <code>writeDocument</code> provide the option to write the annotation
 * type as if it were a feature, i.e. as one of the JSON properties of
 * the annotation object.
 * </p>
 * 
 * @author ian
 * @author dom
 */
class PythonJsonUtils {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final JsonFactory JSON_FACTORY = new JsonFactory();
	/**
		 * Write a substring of a GATE document to the specified
		 * JsonGenerator. The specified window of document text will be
		 * written as a property named "text" and the specified annotations
		 * will be written as "entities", with their offsets adjusted to be
		 * relative to the specified window.
		 * 
		 * @param doc the document to write
		 * @param start the start offset of the segment to write
		 * @param end the end offset of the segment to write
		 * @param extraFeatures additional properties to add to the generated
		 *          JSON. If the map includes a "text" key this will be
		 *          ignored, and if it contains a key "entities" whose value
		 *          is a map then these entities will be merged with the
		 *          generated ones derived from the annotationsMap. This would
		 *          typically be used for documents that were originally
		 *          derived from Twitter data, to re-create the original JSON.
		 * @param annotationTypeProperty if non-null, the annotation type will
		 *          be written as a property under this name, as if it were an
		 *          additional feature of each annotation.
		 * @param annotationIDProperty if non-null, the annotation ID will
		 *          be written as a property under this name, as if it were an
		 *          additional feature of each annotation.
		 * @param json the {@link JsonGenerator} to write to.
		 * @throws JsonGenerationException if a problem occurs while
		 *           generating the JSON
		 * @throws IOException if an I/O error occurs.
		 */
	public static void writeDocument(Document doc, Long start, Long end,
					Map<String, Collection<Annotation>> annotationsMap,
					Map<?, ?> extraFeatures, String annotationTypeProperty, 
					String annotationIDProperty, JsonGenerator json) throws JsonGenerationException, IOException,
					InvalidOffsetException {
		ObjectWriter writer = MAPPER.writer();

		json.writeStartObject();
		String text = doc.getContent().getContent(start, end).toString();
		json.writeStringField("text", text);
		json.writeFieldName("entities");
		json.writeStartObject();
		// if the extraFeatures already includes entities, merge them with
		// the new ones we create
		Object entitiesExtraFeature =
						(extraFeatures == null) ? null : extraFeatures.get("entities");
		Map<?, ?> entitiesMap = null;
		if(entitiesExtraFeature instanceof Map) {
			entitiesMap = (Map<?, ?>)entitiesExtraFeature;
		}
		for(Map.Entry<String, Collection<Annotation>> annsByType : annotationsMap
						.entrySet()) {
			String annotationType = annsByType.getKey();
			Collection<Annotation> annotations = annsByType.getValue();
			json.writeFieldName(annotationType);
			json.writeStartArray();
			for(Annotation a : annotations) {
				json.writeStartObject();
				// indices:[start, end], corrected to match the sub-range of
				// text we're writing
				json.writeArrayFieldStart("indices");
				json.writeNumber(a.getStartNode().getOffset());
				json.writeNumber(a.getEndNode().getOffset());
				json.writeEndArray(); // end of indices
				if(annotationTypeProperty != null) {
					json.writeStringField(annotationTypeProperty, a.getType());
				} 
				if (annotationIDProperty != null) {
					json.writeNumberField(annotationIDProperty, a.getId());
				}
				// other features
				for(Map.Entry<?, ?> feature : a.getFeatures().entrySet()) {
					if(annotationTypeProperty != null
									&& annotationTypeProperty.equals(feature.getKey())) {
						// ignore a feature that has the same name as the
						// annotationTypeProperty
						continue;
					}
					json.writeFieldName(String.valueOf(feature.getKey()));
					writer.writeValue(json, feature.getValue());
				}
				json.writeEndObject(); // end of annotation
			}
			// add any entities from the extraFeatures map
			if(entitiesMap != null
							&& entitiesMap.get(annotationType) instanceof Collection) {
				for(Object ent : (Collection<?>)entitiesMap.get(annotationType)) {
					writer.writeValue(json, ent);
				}
			}
			json.writeEndArray();
		}
		if(entitiesMap != null) {
			for(Map.Entry<?, ?> entitiesEntry : entitiesMap.entrySet()) {
				if(!annotationsMap.containsKey(entitiesEntry.getKey())) {
					// not an entity type we've already seen
					json.writeFieldName(String.valueOf(entitiesEntry.getKey()));
					writer.writeValue(json, entitiesEntry.getValue());
				}
			}
		}

		json.writeEndObject(); // end of entities

		if(extraFeatures != null) {
			for(Map.Entry<?, ?> feature : extraFeatures.entrySet()) {
				if("text".equals(feature.getKey())
								|| "entities".equals(feature.getKey())) {
					// already dealt with text and entities
					continue;
				}
				json.writeFieldName(String.valueOf(feature.getKey()));
				writer.writeValue(json, feature.getValue());
			}
		}
		json.writeEndObject(); // end of document

		// Make sure that everything we have generated is flushed to the
		// underlying OutputStream. It seems that not doing this can easily
		// lead to corrupt files that just end in the middle of a JSON
		// object. This occurs even if you flush the OutputStream instance
		// as the data never leaves the JsonGenerator
		json.flush();
	}

}