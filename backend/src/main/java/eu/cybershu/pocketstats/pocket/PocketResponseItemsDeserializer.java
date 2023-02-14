package eu.cybershu.pocketstats.pocket;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import eu.cybershu.pocketstats.pocket.api.ListItem;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PocketResponseItemsDeserializer extends JsonDeserializer<Map<String, ListItem>> {
    @Override
    public Map<String, ListItem> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        Map<String, ListItem> items = new HashMap<>();

        if (List.of(JsonToken.START_ARRAY, JsonToken.END_ARRAY).contains(p.nextToken())) return items;

        JsonNode node = p.readValueAsTree();
        ObjectCodec codec = p.getCodec();
        if (node.isObject()) {
            TypeReference<HashMap<String, ListItem>> typeRef = new TypeReference<>() {
            };
            return codec.readValue(node.traverse(), typeRef);
        } else {
            throw new IllegalArgumentException("Expected items as object. Got " + node);
        }
    }
}
