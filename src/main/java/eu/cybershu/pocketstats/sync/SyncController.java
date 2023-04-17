package eu.cybershu.pocketstats.sync;

import eu.cybershu.pocketstats.api.ApiResponse;
import eu.cybershu.pocketstats.db.MigrationStatus;
import eu.cybershu.pocketstats.pocket.PocketApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/sync/")
public class SyncController {
    private final PocketApiService pocketApiService;

    public SyncController(PocketApiService pocketApiService) {
        this.pocketApiService = pocketApiService;
    }

    @GetMapping(value = "/last",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SyncStatus> lastSync() {
        MigrationStatus lastMigration = pocketApiService.lastMigration();

        log.debug("import from last - status={}", lastMigration);

        SyncStatus syncStatus = new SyncStatus(
                lastMigration != null ? lastMigration.date() : null,
                lastMigration != null ? lastMigration.migratedItems() : null
        );

        return new ApiResponse<>(0, "ok", syncStatus);
    }

    @PostMapping(value = "/last",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SyncStatus> lastFromLastSync() throws IOException, InterruptedException {
        log.debug("importing from last sync...");

        return new ApiResponse<>(0, "ok", pocketApiService.importFromSinceLastUpdate());
    }
}
