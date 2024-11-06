package eu.cybershu.pocketstats.pocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cybershu.pocketstats.db.Item;
import eu.cybershu.pocketstats.db.PocketItemToDbItemMapper;
import eu.cybershu.pocketstats.pocket.api.ApiXHeaders;
import eu.cybershu.pocketstats.pocket.api.ListItem;
import eu.cybershu.pocketstats.pocket.api.PocketGetResponse;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PocketApiService {
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final PocketAuthorizationService authorizationService;
    private final PocketItemToDbItemMapper itemMapper;

    @Value("${auth.pocket.consumer-key}")
    private String pocketConsumerKey;
    @Value("${auth.pocket.url.get}")
    private String pocketGetUrl;

    public PocketApiService(PocketAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
        this.client = HttpClient
                .newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.mapper = new ObjectMapper();
        this.itemMapper = PocketItemToDbItemMapper.INSTANCE;
    }

    public List<Item> importAllSinceWhen(Instant sinceWhen) throws IOException, InterruptedException {
        var pocketResponse = sinceWhen(sinceWhen);

        return pocketResponse
                .items()
                .values()
                .stream()
                .map(itemMapper::apiToEntity)
                .toList();
    }

    public List<Item> importAll() throws IOException, InterruptedException {
        log.info("Importing all items from GetPocket");

        int offset = 0;
        final var count = 300;
        int gotItems = 0;

        List<Item> importedItems = new LinkedList<>();
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
                    .map(itemMapper::apiToEntity)
                                       .toList();
            importedItems.addAll(models);

            offset += count;
        }

        return importedItems;
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
