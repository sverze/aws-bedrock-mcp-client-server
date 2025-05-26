package co.verze.genai.mcpclient.utils;

import com.fasterxml.jackson.databind.JsonNode;
import software.amazon.awssdk.core.document.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentUtils {
    public static Document ConvertJsonNodeToDocument(JsonNode node) {
        if (node.isObject()) {
            Map<String, Document> map = new HashMap<>();
            node.fields().forEachRemaining(entry -> {
                map.put(entry.getKey(), ConvertJsonNodeToDocument(entry.getValue()));
            });
            return Document.fromMap(map);
        } else if (node.isArray()) {
            List<Document> list = new ArrayList<>();
            for (JsonNode element : node) {
                list.add(ConvertJsonNodeToDocument(element));
            }
            return Document.fromList(list);
        } else if (node.isTextual()) {
            return Document.fromString(node.asText());
        } else if (node.isNumber()) {
            if (node.isInt()) {
                return Document.fromNumber(node.asInt());
            } else {
                return Document.fromNumber(node.asDouble());
            }
        } else if (node.isBoolean()) {
            return Document.fromBoolean(node.asBoolean());
        } else if (node.isNull()) {
            return Document.fromNull();
        } else {
            return Document.fromString(node.toString());
        }
    }
}
