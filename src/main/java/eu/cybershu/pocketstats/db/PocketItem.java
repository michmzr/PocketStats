package eu.cybershu.pocketstats.db;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.cybershu.pocketstats.model.api.DomainMetadata;
import eu.cybershu.pocketstats.model.api.ItemStatus;
import eu.cybershu.pocketstats.model.api.Tag;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.Map;

@Data
@EqualsAndHashCode(of = {"itemId", "url", "title", "status", "timeAdded"})
public class PocketItem {
    @Id
    private String itemId;

    private String url;
    private String title;

    private Boolean favorite;

    private ItemStatus status;

    private Instant timeAdded;

    private Instant timeUpdated;

    private Instant timeRead;

    private Instant timeFavorited;

    private String resolvedTitle;
    private String resolvedUrl;

    private String excerpt;

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
}
