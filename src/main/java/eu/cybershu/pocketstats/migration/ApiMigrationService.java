package eu.cybershu.pocketstats.migration;

import eu.cybershu.pocketstats.db.*;
import eu.cybershu.pocketstats.events.EventsPublisher;
import eu.cybershu.pocketstats.pocket.PocketApiService;
import eu.cybershu.pocketstats.reader.api.Category;
import eu.cybershu.pocketstats.reader.api.Location;
import eu.cybershu.pocketstats.reader.api.ReaderApiService;
import eu.cybershu.pocketstats.reader.api.ReadwiseFetchParams;
import eu.cybershu.pocketstats.sync.SyncStatus;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@Service
public class ApiMigrationService {
    private final MigrationStatusRepository migrationStatusRepository;
    private final PocketApiService pocketApiService;
    private final ReaderApiService readerApiService;
    private final ItemRepository itemRepository;

    private final EventsPublisher eventsPublisher;

    private static final int BATCH_SIZE = 1000;

    public ApiMigrationService(MigrationStatusRepository migrationStatusRepository,
                               PocketApiService pocketApiService, ReaderApiService readerApiService,
                               ItemRepository itemRepository, EventsPublisher eventsPublisher) {
        this.migrationStatusRepository = migrationStatusRepository;
        this.pocketApiService = pocketApiService;
        this.readerApiService = readerApiService;
        this.itemRepository = itemRepository;
        this.eventsPublisher = eventsPublisher;
    }

    @Transactional
    public SyncStatus importAllFromSinceLastUpdate(Source source) {
        log.info("Migrating ALL sources from last sync for {}", source);

        CompletableFuture<SyncStatus> syncFuture;
        if(source.equals(Source.POCKET)){
            syncFuture = CompletableFuture.supplyAsync(() -> syncSource(Source.POCKET));
        } else if(source.equals(Source.READER)){
            syncFuture = CompletableFuture.supplyAsync(() -> syncSource(Source.READER));
        } else {
            throw new IllegalArgumentException("Unknown source " + source);
        }

        CompletableFuture.allOf(syncFuture).join();

        try {
            SyncStatus syncStatus = syncFuture.get();

            log.info("{} migration result: {}", source, source);

            return new SyncStatus(Instant.now(),
                    syncStatus.records(),
                    true);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting parallelized results", e);

            throw new RuntimeException("Error getting parallelized results", e);
        }
    }

    @Transactional
    public SyncStatus importAllFromSinceLastUpdate() {
        log.info("Migrating ALL sources from last sync");

        CompletableFuture<SyncStatus> pocketSyncFuture = CompletableFuture.supplyAsync(() -> syncSource(Source.POCKET));
        CompletableFuture<SyncStatus> readerSyncFuture = CompletableFuture.supplyAsync(() -> syncSource(Source.READER));

        CompletableFuture.allOf(pocketSyncFuture, readerSyncFuture).join();

        try {
            SyncStatus pocketSyncStatus = pocketSyncFuture.get();
            SyncStatus readerSyncStatus = readerSyncFuture.get();

            log.info("Pocket migration result: {}", pocketSyncStatus);
            log.info("Reader migration result: {}", readerSyncStatus);

            return new SyncStatus(Instant.now(),
                    pocketSyncStatus.records() + readerSyncStatus.records(),
                    true);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting parallelized results", e);

            throw new RuntimeException("Error getting parallelized results", e);
        }
    }

    private SyncStatus syncSource(Source source) {
        Optional<Instant> lastOpt = lastMigrationDate(source);

        try {
            if (lastOpt.isPresent()) {
                return this.migrateSource(source, lastOpt.get());
            } else {
                return this.migrateAllFromSource(source);
            }
        } catch (Exception e) {
            log.error("Error migrating source {}", source, e);

            throw new RuntimeException("Error migrating source " + source, e);
        }
    }

    private SyncStatus migrateAllFromSource(Source source) throws Exception {
        log.info("Migrating all sources from beginings of time...");

        List<Item> importedItems = new LinkedList<>();

        if (Objects.requireNonNull(source) == Source.POCKET) {
            importedItems = this.pocketApiService.importAll();
        } else if (source == Source.READER) {
            importedItems = migrateFromReader(Optional.empty());
        }

        return completeMigration(source, importedItems);
    }

    @SneakyThrows
    @Transactional
    public SyncStatus migrateSource(Source source, Instant sinceWhen) {
        log.info("Migrating pocket stats from {} to {}", source, sinceWhen);

        List<Item> importedItems  = new LinkedList<>();
        if (Objects.requireNonNull(source) == Source.POCKET) {
            importedItems = migrateFromPocket(Optional.of(sinceWhen));
        } else if (source == Source.READER) {
            importedItems = migrateFromReader(Optional.of(sinceWhen));
        } else {
            throw new IllegalArgumentException("Unknown source " + source);
        }

        return completeMigration(source, importedItems);
    }

    private SyncStatus completeMigration(Source source, List<Item> importedItems) {
        log.info("Compliting migration - got {} items from '{}'", importedItems.size(), source);

        updateDB(source, importedItems);

        SyncStatus syncStatus = new SyncStatus(
                Instant.now(),
                importedItems.size(),
                true
        );

        eventsPublisher.sendUserSynchronizedItems(syncStatus);

        return syncStatus;
    }

    private List<Item> migrateFromPocket(Optional<Instant> sinceWhen) throws IOException, InterruptedException {
        log.info("Using pocket for migration - when {}", sinceWhen);

        if (sinceWhen.isPresent()) {
            return pocketApiService.importAllSinceWhen(sinceWhen.get());
        } else {
            return pocketApiService.importAll();
        }
    }

    private List<Item> migrateFromReader(Optional<Instant> sinceWhen) throws Exception {
        log.info("Using reader for migration - when {}", sinceWhen);

        String accessToken = System.getenv("READER_ACCESS_TOKEN");

        List<Item> items = new LinkedList<>();
        if (sinceWhen.isPresent()) {
            items =  readerApiService.importAllSinceWhen(accessToken, sinceWhen.get());
        } else {
            items = readerApiService.importAll(accessToken);
        }

        log.debug("Items before filtering: {}", items.size());
        items = items.stream()
                .filter(it -> allowForReaderCategories()
                        .contains(it.category()))
                .collect(Collectors.toList());
        log.debug("Items after filtering: {}", items.size());

        return items;
    }

    private static List<String> allowForReaderCategories() {
        return Stream
                .of(Category.ARTICLE,
                        Category.EMAIL,
                        Category.RSS,
                        Category.EPUB,
                        Category.PDF,
                        Category.TWEET,
                        Category.VIDEO
                ).map(Category::getValue)
                .toList();
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

    public Optional<Instant> lastMigrationDate(Source source) {
        return migrationStatusRepository
                .findBySourceOrderByDateDesc(source)
                .map(it -> it.date());
    }

    public Optional<MigrationStatus> lastMigration(Source source) {
        return migrationStatusRepository
                .findBySourceOrderByDateDesc(source);
    }
}
