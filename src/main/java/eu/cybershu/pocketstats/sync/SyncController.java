package eu.cybershu.pocketstats.sync;

import eu.cybershu.pocketstats.api.ApiResponse;
import eu.cybershu.pocketstats.db.MigrationStatus;
import eu.cybershu.pocketstats.db.Source;
import eu.cybershu.pocketstats.migration.ApiMigrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/sync/")
public class SyncController {
    private final ApiMigrationService apiMigrationService;

    public SyncController(ApiMigrationService apiMigrationService) {
        this.apiMigrationService = apiMigrationService;
    }


    @GetMapping(value = "/last",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SyncStatus> lastSync(Source source) {
        log.info("Checking last migration for {}", source);

        Optional<MigrationStatus> lastMigrationOpt = apiMigrationService.lastMigration(source);
        log.debug("Last migration {} status {}", source, lastMigrationOpt);

        if (lastMigrationOpt.isPresent()) {
            SyncStatus syncStatus = new SyncStatus(
                    lastMigrationOpt.get().date(),
                    lastMigrationOpt.get().migratedItems(),
                    true
            );

            return new ApiResponse<>(0, "ok", syncStatus);
        } else {
            SyncStatus syncStatus = new SyncStatus(
                    null, 0, false
            );

            return new ApiResponse<>(0, "Not found migration status", syncStatus);
        }
    }

    @PostMapping(value = "/last",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SyncStatus> lastFromLastSync() throws IOException, InterruptedException {
        log.debug("importing from last sync...");

        var result = apiMigrationService.importAllFromSinceLastUpdate();

        // todo better
        return new ApiResponse<>(0, "ok", result);
    }
}
