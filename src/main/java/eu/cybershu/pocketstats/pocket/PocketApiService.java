package eu.cybershu.pocketstats.pocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cybershu.pocketstats.db.*;
import eu.cybershu.pocketstats.events.EventsPublisher;
import eu.cybershu.pocketstats.pocket.api.ApiXHeaders;
import eu.cybershu.pocketstats.pocket.api.ListItem;
import eu.cybershu.pocketstats.pocket.api.PocketGetResponse;
import eu.cybershu.pocketstats.sync.SyncStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class PocketApiService {
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final PocketItemRepository pocketItemRepository;
    private final MigrationStatusRepository migrationStatusRepository;
    private final PocketAuthorizationService authorizationService;
    private final PocketItemMapper pocketItemMapper;

    private final EventsPublisher eventsPublisher;

    @Value("${auth.pocket.consumer-key}")
    private String pocketConsumerKey;
    @Value("${auth.pocket.url.get}")
    private String pocketGetUrl;

    public PocketApiService(PocketItemRepository pocketItemRepository,
                            MigrationStatusRepository migrationStatusRepository,
                            PocketAuthorizationService authorizationService, EventsPublisher eventsPublisher) {
        this.migrationStatusRepository = migrationStatusRepository;
        this.authorizationService = authorizationService;
        this.eventsPublisher = eventsPublisher;
        this.client = HttpClient
                .newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.mapper = new ObjectMapper();
        this.pocketItemMapper = PocketItemMapper.INSTANCE;
        this.pocketItemRepository = pocketItemRepository;
    }

    public SyncStatus importFromSinceLastUpdate() throws IOException, InterruptedException {
        MigrationStatus status = lastMigration();

        log.debug("import from last - status={}", status);

        int items;
        if (status == null) {
            log.warn("No import was done. Importing all items");
            items =  importAll();
        } else {
            items = importAllToDbSince(status.date());
        }

        SyncStatus syncStatus = new SyncStatus(
                Instant.now(),
                items
        );
        eventsPublisher.sendUserSynchronizedItems(syncStatus);

        return syncStatus;
    }

    public MigrationStatus lastMigration() {
        return migrationStatusRepository.findTopByOrderByDateDesc();
    }

    public Integer importAllToDbSince(Instant sinceWhen) throws IOException, InterruptedException {
        var pocketResponse = sinceWhen(sinceWhen);

        var models = pocketResponse
                .items()
                .values()
                .stream()
                .map(pocketItemMapper::apiToEntity)
                .toList();

        List<PocketItem> pocketItems = pocketItemRepository.saveAll(models);

        MigrationStatus migrationStatus = new MigrationStatus();
        migrationStatus.id(UUID.randomUUID()
                               .toString());
        migrationStatus.date(Instant.now());
        migrationStatus.migratedItems(pocketItems.size());
        migrationStatusRepository.save(migrationStatus);

        return pocketItems.size();
    }

    public int importAll() throws IOException, InterruptedException {
        log.info("Importing all items from GetPocket");

        int offset = 0;
        final var count = 300;
        int gotItems = 0;

        List<PocketItem> importedItems = new LinkedList<>();
        while (true) {
            log.debug("offset:{}, count:{}", offset, count);

            var pocketResponse = callGetApi(
                    Map.of("count", count,
                            "state", "all",
                            "offset", offset,
                            "sort", "oldest",
                            "detailType", "complete"));

            Map<String, ListItem> items = pocketResponse.items();
            log.info("Got {} items", items.size());
            log.debug("response: {}", pocketResponse);

            if (items.isEmpty())
                break;

            var models = pocketResponse.items()
                                       .values()
                                       .stream()
                                       .map(pocketItemMapper::apiToEntity)
                                       .toList();
            importedItems.addAll(models);

            gotItems += items.size();

            pocketItemRepository.saveAll(importedItems);

            offset += count;
        }

        MigrationStatus migrationStatus = new MigrationStatus();
        migrationStatus.id(UUID.randomUUID()
                               .toString());
        migrationStatus.date(Instant.now());
        migrationStatus.migratedItems(gotItems);
        migrationStatusRepository.save(migrationStatus);

        return gotItems;
    }

    public PocketGetResponse sinceWhen(Instant sinceWhen) throws IOException, InterruptedException {
        log.info("Reading items since {}", sinceWhen);

        return callGetApi(Map.of("since", sinceWhen.getEpochSecond(), "detailType", "complete"));
    }


    private PocketGetResponse callGetApi(Map<String, Object> extraFields) throws IOException, InterruptedException {
        Map<Object, Object> payloadData = new HashMap<>();
        payloadData.put("consumer_key", pocketConsumerKey);
        payloadData.put("access_token", getCreds().accessCode());
        payloadData.putAll(extraFields);

        String payload = mapper.writeValueAsString(payloadData);
        HttpRequest request = HttpRequest.newBuilder()
                                         .POST(HttpRequest.BodyPublishers.ofString(payload))
                                         .uri(URI.create(pocketGetUrl))
                                         .header("Content-Type", "application/json")
                                         .header("X-accept", "application/json")
                                         .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        logResponse(response);

        switch (response.statusCode()) {
            case 200:
                return getPocketGetResponse(response);
            case 401:
                handle401(response);
                break;
            case 400:
            case 403:
            case 500:
            case 504:
                handleOtherHttpErrorCode(response);
            default:
                throw new IllegalStateException("Not expected http response code " + response.statusCode());
        }

        throw new IllegalStateException("Not expected http response code " + response.statusCode());
    }

    private PocketGetResponse getPocketGetResponse(HttpResponse<String> response) throws JsonProcessingException {
        return mapper.readValue(response.body(), PocketGetResponse.class);
    }

    private void handleOtherHttpErrorCode(HttpResponse<String> response) {
        log.error("Handling {} error", response.statusCode());
        ApiXHeaders apiXHeaders = ApiXHeaders.of(response);

        throw new IllegalStateException(String.format(
                "Http request failed with status: %s."
                        + "Api pocket responded with 401, " + "error code: %d, "
                        + "error description: %s",
                apiXHeaders.status(), apiXHeaders.errorCode(), apiXHeaders.error()));
    }

    private void handle401(HttpResponse<String> response) {
        log.info("Handling 401 error");
        ApiXHeaders apiXHeaders = ApiXHeaders.of(response);

        throw new IllegalStateException(
                "Generate new token!.Authorization failed with status: '%s'. Api pocket responded with 401, error code: %d, %s"
                        .formatted(apiXHeaders.status(),
                                apiXHeaders.errorCode(), apiXHeaders.error()));
    }

    private void logResponse(HttpResponse<String> response) {
        log.debug("status: {}", response.statusCode());

        // print response body
        if (response.statusCode() != HttpStatus.OK.value()) {
            log.debug("response: {}", response.body());

            ApiXHeaders apiXHeaders = ApiXHeaders.of(response);
            log.debug("api X headers: {}", apiXHeaders);
        }
    }

    public PocketUserCredentials getCreds() {
        return authorizationService.getCredentials()
                                   .orElseThrow(() ->
                                           new IllegalStateException("Not found saved credentials. Authorize to service first"));
    }
}
