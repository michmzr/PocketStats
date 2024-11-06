package eu.cybershu.pocketstats.db;

import com.mongodb.lang.Nullable;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Data
public class MigrationStatus {
    @Id
    private String id;
    private Instant date;
    private Integer migratedItems;

    @Nullable
    private Source source;
}
