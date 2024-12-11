package eu.cybershu.pocketstats.db;

import eu.cybershu.pocketstats.pocket.api.DomainMetadata;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "url", "title", "status", "timeAdded", "timeRead"})
public class Item {
    @Id
    private String id;
    private String url;
    private String title;
    private Boolean favorite;
    @NonNull
    private ItemStatus status;
    private Instant timeAdded;
    private Instant timeUpdated;
    @Nullable
    private Instant timeRead;
    @Nullable
    private Instant timeFavorited;
    private String excerpt;
    private Integer wordCount;
    @Nullable
    private String lang;
    @Nullable
    private DomainMetadata domainMetadata;
    private List<String> tags;

    /**
     * article, email, document...
     **/
    private String category; // todo as a enum
    private Source source;
}
