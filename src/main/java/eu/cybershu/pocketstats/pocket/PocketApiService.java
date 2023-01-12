package eu.cybershu.pocketstats.pocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cybershu.pocketstats.db.*;
import eu.cybershu.pocketstats.pocket.api.ApiXHeaders;
import eu.cybershu.pocketstats.pocket.api.ListItem;
import eu.cybershu.pocketstats.pocket.api.PocketGetResponse;
import eu.cybershu.pocketstats.stats.PocketStatPredicate;
import eu.cybershu.pocketstats.stats.ToReadPredicate;
import eu.cybershu.pocketstats.utils.TimeUtils;
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
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
public class PocketApiService {
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final List<PocketStatPredicate> statPredicates;
    private final PocketItemRepository pocketItemRepository;
    private final MigrationStatusRepository migrationStatusRepository;
    private final PocketAuthorizationService authorizationService;
    private final PocketItemMapper pocketItemMapper;
    @Value("${auth.pocket.consumer-key}")
    private String pocketConsumerKey;
    @Value("${auth.pocket.url.get}")
    private String pocketGetUrl;

    public PocketApiService(List<PocketStatPredicate> statPredicates, PocketItemRepository pocketItemRepository, MigrationStatusRepository migrationStatusRepository, PocketAuthorizationService authorizationService) {
        this.migrationStatusRepository = migrationStatusRepository;
        this.authorizationService = authorizationService;
        this.client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build();
        this.mapper = new ObjectMapper();
        this.pocketItemMapper = PocketItemMapper.INSTANCE;
        this.statPredicates = statPredicates;
        this.pocketItemRepository = pocketItemRepository;
    }

    private static ZoneId getZoneId() {
        return ZoneId.systemDefault();
    }


    //Items to read
    public int itemsToRead() throws IOException, InterruptedException {
        Map<Object, Object> data = new HashMap<>();
        data.put("consumer_key", pocketConsumerKey);
        data.put("access_token", getCreds().accessCode());
        data.put("state", "unread");
        data.put("detailType", "complete");

        String payload = mapper.writeValueAsString(data);
        HttpRequest request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(payload)).uri(URI.create(pocketGetUrl)).header("Content-Type", "application/json").header("X-accept", "application/json").build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // print status code
        log.debug("status: {}", response.statusCode());

        // print response body
        log.debug("response: {}", response.body());

        if (response.statusCode() == 200) {
            var pocketResponse = getPocketGetResponse(response);

            ToReadPredicate predicate = new ToReadPredicate();
            var items = pocketResponse.items();
            int counter = 0;
            for (Map.Entry<String, ListItem> entry : items.entrySet()) {
                ListItem item = entry.getValue();
                counter += predicate.test(item, null) ? 1 : 0;
            }

            return counter;
        } else {
            throw new IllegalArgumentException("Not acquired access token.");
        }
    }

    public Integer importAllToDbSince(Instant sinceWhen) throws IOException, InterruptedException {
        var pocketResponse = sinceWhen(sinceWhen);

        var models = pocketResponse.items().values().stream().map(pocketItemMapper::apiToEntity).toList();

        List<PocketItem> pocketItems = pocketItemRepository.saveAll(models);

        MigrationStatus migrationStatus = new MigrationStatus();
        migrationStatus.id(UUID.randomUUID().toString());
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

            var pocketResponse = callGetApi(Map.of("count", count, "state", "all", "offset", offset, "sort", "oldest", "detailType", "complete"));

            Map<String, ListItem> items = pocketResponse.items();
            log.info("Got {} items - since {}", items.size(), pocketResponse.since());
            log.debug("response: {}", pocketResponse);

            if (items.isEmpty()) break;

            var models = pocketResponse.items().values().stream().map(pocketItemMapper::apiToEntity).toList();
            importedItems.addAll(models);

            gotItems += items.size();

            pocketItemRepository.saveAll(importedItems);

            offset += count;
        }

        MigrationStatus migrationStatus = new MigrationStatus();
        migrationStatus.id(UUID.randomUUID().toString());
        migrationStatus.date(Instant.now());
        migrationStatus.migratedItems(gotItems);
        migrationStatusRepository.save(migrationStatus);

        return gotItems;
    }

    public Map<PocketStatPredicate, Integer> getCurrentMonth() throws IOException, InterruptedException {
        ZoneId zoneId = getZoneId();
        Instant sinceWhen = TimeUtils.getStartOfCurrentMonth().atZone(zoneId).toInstant();
        return calcStatsSinceWhen(sinceWhen);
    }

    public Map<PocketStatPredicate, Integer> calcStatsSinceWhen(Instant sinceWhen) throws IOException, InterruptedException {
        var pocketResponse = sinceWhen(sinceWhen);

        Map<PocketStatPredicate, Integer> stats = new HashMap<>();
        statPredicates.forEach(predicate -> {
            stats.put(predicate, 0);
        });

        var items = pocketResponse.items();
        items.forEach((itemId, item) -> {
            try {
                statPredicates.forEach(predicate -> stats.compute(predicate, (k, oldValue) -> predicate.test(item, sinceWhen) ? oldValue + 1 : oldValue));
            } catch (Exception e) {
                log.error("exception catched:", e);
            }
        });

        return stats;
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
        HttpRequest request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(payload)).uri(URI.create(pocketGetUrl)).header("Content-Type", "application/json").header("X-accept", "application/json").build();

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

        throw new IllegalStateException(String.format("Http request failed with status: %s." + "Api pocket responded with 401, " + "error code: %d, " + "error description: %s", apiXHeaders.status(), apiXHeaders.errorCode(), apiXHeaders.error()));
    }

    private void handle401(HttpResponse<String> response) {
        log.info("Handling 401 error");
        ApiXHeaders apiXHeaders = ApiXHeaders.of(response);

        throw new IllegalStateException("Generate new token!. Authorization failed with status: '%s'. Api pocket " + "responded " + "with " + "401, " + "error code: %d, " + "error description: %s".formatted(apiXHeaders.status(), apiXHeaders.errorCode(), apiXHeaders.error()));
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
        return authorizationService.getCredentials().orElseThrow(() -> new IllegalStateException("Not found saved credentials. Authorize to service first"));
    }
}
