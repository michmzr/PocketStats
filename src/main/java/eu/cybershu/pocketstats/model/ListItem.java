
package eu.cybershu.pocketstats.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.cybershu.pocketstats.utils.LongToInstantConverter;
import eu.cybershu.pocketstats.utils.StringBooleanToBoolean;
import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/*
@todo standaride time to one time zone
 */

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListItem {
    @JsonProperty("item_id")
    private String itemId;
    @JsonProperty("resolved_id")
    private String resolvedId;
    @JsonProperty("given_url")
    private String url;
    @JsonProperty("given_title")
    private String title;

    @JsonDeserialize(converter = StringBooleanToBoolean.class)
    @JsonProperty("favorite")
    private Boolean favorite;

    /*
    0, 1, 2 - 1 if the item is archived - 2 if the item should be deleted
     */
    @JsonProperty("status")
    private ItemStatus status;

    @JsonProperty("time_added")
    @JsonDeserialize(converter = LongToInstantConverter.class)
    private Instant timeAdded;

    @JsonProperty("time_updated")
    @JsonDeserialize(converter = LongToInstantConverter.class)
    private Instant timeUpdated;

    @JsonDeserialize(converter = LongToInstantConverter.class)
    @JsonProperty("time_read")
    private Instant timeRead;

    @JsonProperty("time_favorited")
    @JsonDeserialize(converter = LongToInstantConverter.class)
    private Instant timeFavorited;

    @JsonProperty("sort_id")
    private Integer sortId;
    @JsonProperty("resolved_title")
    private String resolvedTitle;
    @JsonProperty("resolved_url")
    private String resolvedUrl;
    @JsonProperty("excerpt")
    private String excerpt;

    @JsonDeserialize(converter = StringBooleanToBoolean.class)
    @JsonProperty("is_article")
    private Boolean isArticle;
    @JsonDeserialize(converter = StringBooleanToBoolean.class)
    @JsonProperty("is_index")
    private Boolean isIndex;
    @JsonDeserialize(converter = StringBooleanToBoolean.class)
    @JsonProperty("has_video")
    private Boolean hasVideo;
    @JsonDeserialize(converter = StringBooleanToBoolean.class)
    @JsonProperty("has_image")
    private Boolean hasImage;
    @JsonProperty("word_count")
    private Integer wordCount;
    @JsonProperty("lang")
    private String lang;
    @JsonProperty("amp_url")
    private String ampUrl;
    @JsonProperty("top_image_url")
    private String topImageUrl;
    @JsonProperty("domain_metadata")
    private DomainMetadata domainMetadata;
    private Map<String, Tag> tags;

    @JsonProperty("listen_duration_estimate")
    private Integer listenDurationEstimate;
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
