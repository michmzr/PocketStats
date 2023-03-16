package eu.cybershu.pocketstats.db;

import eu.cybershu.pocketstats.pocket.api.DomainMetadata;
import eu.cybershu.pocketstats.pocket.api.ItemStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "url", "title", "status", "timeAdded", "timeRead"})
public class PocketItem {
    @Id
    private String id;
    private String url;
    private String title;
    private Boolean favorite;
    @NonNull
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
    private DomainMetadata domainMetadata;
    private List<String> tags;
}
