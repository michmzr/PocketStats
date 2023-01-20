package eu.cybershu.pocketstats.pocket.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.cybershu.pocketstats.utils.LongToInstantConverter;
import eu.cybershu.pocketstats.utils.StringBooleanToBoolean;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ListItem(@JsonProperty("item_id") String id, @JsonProperty("resolved_id") String resolvedId,
                       @JsonProperty("given_url") String url, @JsonProperty("given_title") String title,
                       @JsonDeserialize(converter = StringBooleanToBoolean.class) @JsonProperty("favorite") Boolean favorite,
                       @JsonProperty("status") ItemStatus status,
                       @JsonProperty("time_added") @JsonDeserialize(converter = LongToInstantConverter.class) Instant timeAdded,
                       @JsonProperty("time_updated") @JsonDeserialize(converter = LongToInstantConverter.class) Instant timeUpdated,
                       @JsonDeserialize(converter = LongToInstantConverter.class) @JsonProperty("time_read") Instant timeRead,
                       @JsonProperty("time_favorited") @JsonDeserialize(converter = LongToInstantConverter.class) Instant timeFavorited,
                       @JsonProperty("resolved_title") String resolvedTitle,
                       @JsonProperty("resolved_url") String resolvedUrl, @JsonProperty("excerpt") String excerpt,
                       @JsonDeserialize(converter = StringBooleanToBoolean.class) @JsonProperty("is_article") Boolean isArticle,
                       @JsonDeserialize(converter = StringBooleanToBoolean.class) @JsonProperty("is_index") Boolean isIndex,
                       @JsonDeserialize(converter = StringBooleanToBoolean.class) @JsonProperty("has_video") Boolean hasVideo,
                       @JsonDeserialize(converter = StringBooleanToBoolean.class) @JsonProperty("has_image") Boolean hasImage,
                       @JsonProperty("word_count") Integer wordCount, @JsonProperty("lang") String lang,
                       @JsonProperty("amp_url") String ampUrl, @JsonProperty("top_image_url") String topImageUrl,
                       @JsonProperty("domain_metadata") DomainMetadata domainMetadata,
                       @JsonProperty("tags") Map<String, Tag> tags,
                       @JsonProperty("listen_duration_estimate") Integer listenDurationEstimate) {
    @Override
    public String toString() {
        return "ListItem{" + "id='" + id + '\'' + ", resolvedId='" + resolvedId + '\'' + ", favorite=" + favorite + ", status=" + status + ", timeAdded=" + timeAdded + ", timeRead=" + timeRead + '}';
    }
}
