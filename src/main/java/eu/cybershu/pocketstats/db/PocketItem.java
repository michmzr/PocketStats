package eu.cybershu.pocketstats.db;

import eu.cybershu.pocketstats.model.api.ItemStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Data
@EqualsAndHashCode(of = {"id", "url", "title", "status", "timeAdded"})
public class PocketItem {
    @Id
    private String id;

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
    private Integer wordCount;
    private String lang;
//    private DomainMetadata domainMetadata;
//    private Map<String, Tag> tags;
}
