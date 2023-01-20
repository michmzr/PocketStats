package eu.cybershu.pocketstats.db;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Data
public class MigrationStatus {
    @Id
    private String id;
    private Instant date;
    private Integer migratedItems;
}
