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
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PocketResponseItemsDeserializer extends JsonDeserializer<Map<String, ListItem>> {
    @Override
    public Map<String, ListItem> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Map<String, ListItem> items = new HashMap<>();

        JsonToken token = p.nextToken();
        if (List.of(JsonToken.START_ARRAY, JsonToken.END_ARRAY)
                .contains(token))
            return items;

        JsonNode node = p.readValueAsTree();
        ObjectCodec codec = p.getCodec();
        if (node.isObject()) {
            TypeReference<HashMap<String, ListItem>> typeRef = new TypeReference<>() {
            };
            try {
                return codec.readValue(node.traverse(), typeRef);
            } catch (NoSuchFieldError e) {
                log.error("Error deserializing field", e);
                throw e;
            }
        } else {
            if(node.isNull()) {
                log.warn("Expected node as object. Got null");
                return items;
            } else {
                throw new IllegalArgumentException("Expected items as object. Got " + node);
            }
        }
    }
}
