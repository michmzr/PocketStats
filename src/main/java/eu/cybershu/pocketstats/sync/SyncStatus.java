package eu.cybershu.pocketstats.sync;

import java.time.Instant;

public record SyncStatus(
        Instant date,
        Integer records,
        Boolean success
) {
}
