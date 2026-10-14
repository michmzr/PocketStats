package eu.cybershu.pocketstats.migration;

import eu.cybershu.pocketstats.db.*;
import eu.cybershu.pocketstats.events.EventsPublisher;
import eu.cybershu.pocketstats.reader.ReaderSyncConfiguration;
import eu.cybershu.pocketstats.reader.api.Category;
import eu.cybershu.pocketstats.reader.api.ReaderApiService;
import eu.cybershu.pocketstats.sync.SyncStatus;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@Service
public class ApiMigrationService {
    private final MigrationStatusRepository migrationStatusRepository;
    private final ReaderSyncConfiguration readerSyncConfiguration;
    private final ReaderApiService readerApiService;
    private final ItemRepository itemRepository;

    private final EventsPublisher eventsPublisher;

    private static final int BATCH_SIZE = 1000;

    public ApiMigrationService(MigrationStatusRepository migrationStatusRepository,
                               ReaderApiService readerApiService,
                               ItemRepository itemRepository, EventsPublisher eventsPublisher,
                               ReaderSyncConfiguration readerSyncConfiguration) {
        this.migrationStatusRepository = migrationStatusRepository;
        this.readerSyncConfiguration = readerSyncConfiguration;
        this.readerApiService = readerApiService;
        this.itemRepository = itemRepository;
        this.eventsPublisher = eventsPublisher;
    }

    @SneakyThrows
    @Transactional
    public SyncStatus importAllFromSinceLastUpdate(Source source) {
        requireReaderSync(source);
        Optional<Instant> sinceWhen = lastMigrationDate(Source.READER);
        return completeMigration(Source.READER, migrateFromReader(sinceWhen));
    }

    @Transactional
    public SyncStatus importAllFromSinceLastUpdate() {
        return importAllFromSinceLastUpdate(Source.READER);
    }

    @SneakyThrows
    @Transactional
    public SyncStatus migrateSource(Source source, Instant sinceWhen) {
        requireReaderSync(source);
        return completeMigration(Source.READER, migrateFromReader(Optional.of(sinceWhen)));
    }

    private void requireReaderSync(Source source) {
        if (source == Source.POCKET) {
            throw new ResponseStatusException(HttpStatus.GONE, "Pocket synchronization is no longer available");
        }
        if (source != Source.READER) {
            throw new IllegalArgumentException("Unknown source " + source);
        }
        if (!readerSyncConfiguration.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Readwise synchronization is not configured");
        }
    }

    private SyncStatus completeMigration(Source source, List<Item> importedItems) {
        log.info("Completing migration - got {} items from '{}'....", importedItems.size(), source);

        updateDB(source, importedItems);

        SyncStatus syncStatus = new SyncStatus(
                Instant.now(),
                importedItems.size(),
                true
        );

        eventsPublisher.sendUserSynchronizedItems(syncStatus);

        return syncStatus;
    }

    private List<Item> migrateFromReader(Optional<Instant> sinceWhen) throws Exception {
        log.info("Using reader for migration - when {}", sinceWhen);

        String accessToken = readerSyncConfiguration.accessToken();

        List<Item> items = new LinkedList<>();
        if (sinceWhen.isPresent()) {
            items = readerApiService.importAllSinceWhen(accessToken, sinceWhen.get());
        } else {
            items = readerApiService.importAll(accessToken);
        }

        log.debug("Reader items before filtering: {}", items.size());
        items = items.stream()
                .filter(it -> allowForReaderCategories()
                        .contains(it.category().toLowerCase()))
                .collect(Collectors.toList());
        log.debug("Reader items after filtering: {}", items.size());

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
        return lastMigration(source)
                .map(MigrationStatus::date);
    }

    public Optional<MigrationStatus> lastMigration(Source source) {
        return migrationStatusRepository
                .findFirstBySourceOrderByDateDesc(source);
    }
}
