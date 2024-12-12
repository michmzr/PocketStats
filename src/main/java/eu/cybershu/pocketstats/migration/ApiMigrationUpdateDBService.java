package eu.cybershu.pocketstats.migration;

import eu.cybershu.pocketstats.db.*;
import eu.cybershu.pocketstats.events.EventsPublisher;
import eu.cybershu.pocketstats.pocket.PocketApiService;
import eu.cybershu.pocketstats.reader.api.ReaderApiService;
import eu.cybershu.pocketstats.sync.SyncStatus;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;


@Slf4j
@Service
public class ApiMigrationUpdateDBService {
    private final MigrationStatusRepository migrationStatusRepository;
    private final ItemRepository itemRepository;

    private final EventsPublisher eventsPublisher;

    private static final int BATCH_SIZE = 1000;

    public ApiMigrationUpdateDBService(MigrationStatusRepository migrationStatusRepository, ItemRepository itemRepository, EventsPublisher eventsPublisher) {
        this.migrationStatusRepository = migrationStatusRepository;
        this.itemRepository = itemRepository;
        this.eventsPublisher = eventsPublisher;
    }

    @Transactional
    SyncStatus completeMigration(Source source, List<Item> importedItems) {
        log.info("Completing migration - got {} items from '{}'", importedItems.size(), source);

        updateDB(source, importedItems);

        SyncStatus syncStatus = new SyncStatus(
                Instant.now(),
                importedItems.size(),
                true
        );

        eventsPublisher.sendUserSynchronizedItems(syncStatus);

        return syncStatus;
    }


    private void updateDB(Source source, List<Item> importedItems) {
        if (!importedItems.isEmpty()) {
            int totalSize = importedItems.size();

            IntStream.range(0, (totalSize + BATCH_SIZE - 1) / BATCH_SIZE)
                    .mapToObj(i -> importedItems.subList(i * BATCH_SIZE, Math.min((i + 1) * BATCH_SIZE, totalSize)))
                    .forEach(itemRepository::saveAll);

        }

        MigrationStatus migrationStatus = new MigrationStatus();
        migrationStatus.id(UUID.randomUUID()
                .toString());
        migrationStatus.date(Instant.now());
        migrationStatus.source(source);
        migrationStatus.migratedItems(importedItems.size());
        migrationStatusRepository.save(migrationStatus);
    }
}
