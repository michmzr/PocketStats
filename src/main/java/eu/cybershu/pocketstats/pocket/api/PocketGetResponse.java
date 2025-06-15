
package eu.cybershu.pocketstats.pocket.api;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.cybershu.pocketstats.pocket.PocketResponseItemsDeserializer;
import eu.cybershu.pocketstats.utils.LongToInstantConverter;
import lombok.Data;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * @link <a href="https://getpocket.com/developer/docs/v3/retrieve">get pocket api retrieve</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PocketGetResponse {

    @JsonProperty("status")
    private Integer status;
    @JsonProperty("complete")
    private Integer complete;

    @JsonProperty("list")
    @JsonDeserialize(using = PocketResponseItemsDeserializer.class)
    @Nullable
    private Map<String, ListItem> items;
    @JsonProperty("error")
    private Object error;
    @JsonProperty("search_meta")
    private SearchMeta searchMeta;
    @JsonProperty("since")
    @JsonDeserialize(converter = LongToInstantConverter.class)
    private Instant since;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        return "PocketGetResponse{" + "status=" + status + ", complete=" + complete + ", items=" + (items != null ? items.size() : "empty") + ", " + "error" + "=" + error + ", since=" + since + '}';
    }
}
