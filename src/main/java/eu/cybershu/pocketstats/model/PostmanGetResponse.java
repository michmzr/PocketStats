
package eu.cybershu.pocketstats.model;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @link <a href="https://getpocket.com/developer/docs/v3/retrieve">get pocket api retrieve</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostmanGetResponse {

    @JsonProperty("status")
    private Integer status;
    @JsonProperty("complete")
    private Integer complete;
    @JsonProperty("list")
    private Map<String, ListItem> items;
    @JsonProperty("error")
    private Object error;
    @JsonProperty("search_meta")
    private SearchMeta searchMeta;
    @JsonProperty("since")
    private Integer since;
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
}
