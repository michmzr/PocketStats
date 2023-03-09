package eu.cybershu.pocketstats.sync;

import eu.cybershu.pocketstats.api.ApiResponse;
import eu.cybershu.pocketstats.db.MigrationStatus;
import eu.cybershu.pocketstats.db.PocketItemRepository;
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
    private final PocketItemRepository repository;

    public SyncController(PocketApiService pocketApiService, PocketItemRepository repository) {
        this.pocketApiService = pocketApiService;
        this.repository = repository;
    }

    @GetMapping(value = "/last",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SyncStatus> lastSync() {
        MigrationStatus lastMigration = pocketApiService.lastMigration();

        log.debug("import from last - status={}", lastMigration);

        return new ApiResponse<>(0, "ok", new SyncStatus(
                lastMigration.date(),
                lastMigration.migratedItems()
        ));
    }

    @PostMapping(value = "/last",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SyncStatus> lastFromLastSync() throws IOException, InterruptedException {
        log.debug("importing from last sync...");

        Integer records = pocketApiService.importFromSinceLastUpdate();

        return new ApiResponse<>(0, "ok", new SyncStatus(
                Instant.now(),
                records
        ));
    }
}
