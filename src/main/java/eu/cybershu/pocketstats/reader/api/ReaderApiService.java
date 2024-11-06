package eu.cybershu.pocketstats.reader.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cybershu.pocketstats.api.TooManyRequestsException;
import eu.cybershu.pocketstats.db.Item;
import eu.cybershu.pocketstats.reader.ReaderItemToDbItemMapper;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Thread.sleep;

/**
 * Service for interacting with the Readwise Reader API: https://readwise.io/reader_api
 */
@Slf4j
@Service
public class ReaderApiService {
    private static final Integer READER_MAX_RETRIES = 3;
    private final String readewiseReaderListUrl = "https://readwise.io/api/v3/list/?";

    private final ReaderItemToDbItemMapper itemMapper;
    private final HttpClient client;
    private final ObjectMapper mapper;

    private Integer retryAfter;

    public ReaderApiService() {
        this.itemMapper = ReaderItemToDbItemMapper.INSTANCE;
        this.client = HttpClient
                .newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(60))
                .build();
        this.retryAfter = 0;
        this.mapper = new ObjectMapper().findAndRegisterModules();
//        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
//        .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
//
    }

    private synchronized void updateAfter(Integer updateAfter) {
        retryAfter = updateAfter;
    }

    public List<Item> importAll(String accessToken, ReadwiseFetchParams params) throws IOException, InterruptedException {
        log.info("Fetching Readwise list with params: {}", params);

        List<ReaderItem> items = new LinkedList<>();
        ReadwiseFetchPaginationParams pageParams = ReadwiseFetchPaginationParams
                .builder()
                .category(params.category())
                .location(params.location())
                .pageCursor(null)
                .build();

        return getItems(accessToken, params, pageParams, items);
    }

    /**
     * Key	Type	Description	Required
     * id	string	The document's unique id. Using this parameter it will return just one document, if found.	no
     * updatedAfter	string (formatted as ISO 8601 date)	Fetch only documents updated after this date	no
     * location	string	The document's location, could be one of: new, later, shortlist, archive, feed	no
     * category	string	The document's category, could be one of: article, email, rss, highlight, note, pdf, epub, tweet, video	no
     * pageCursor	string	A string returned by a previous request to this endpoint. Use it to get the next page of documents if there are too many for one request.	no
     */
    @RateLimiter(name = "readwise-api")
    public List<Item> fetchList(String accessToken, ReadwiseFetchParams params)
            throws IOException, InterruptedException {
        log.info("Fetching Readwise list with params: {}", params);

        List<ReaderItem> items = new LinkedList<>();
        ReadwiseFetchPaginationParams pageParams = ReadwiseFetchPaginationParams
                .builder()
                .updatedAfter(params.updatedAfter())
                .category(params.category())
                .location(params.location())
                .pageCursor(null)
                .build();

        return getItems(accessToken, params, pageParams, items);
    }

    private List<Item> getItems(String accessToken, ReadwiseFetchParams params, ReadwiseFetchPaginationParams pageParams, List<ReaderItem> items) throws IOException, InterruptedException {
        do {
            ReaderListResponse response = null;
            Integer retried = 0;

            do {
                try {
                    response = fetchPage(accessToken, pageParams);

                    break;
                } catch (TooManyRequestsException e) {
                    log.debug("Catched too may request exception: {}, retry {}", e.retryAfter() + 1, ++retried);

                    sleep(e.retryAfter() * 1000 + 1000);

                    if (retried > READER_MAX_RETRIES)
                        throw new RuntimeException(e);
                }
            } while (retried <= READER_MAX_RETRIES);

            log.debug("Got items: {}", response.results().size());
            items.addAll(response.results());

            pageParams = ReadwiseFetchPaginationParams
                    .builder()
                    .category(params.category())
                    .location(params.location())
                    .updatedAfter(params.updatedAfter())
                    .pageCursor(response.nextPageCursor())
                    .build();
        } while (pageParams.pageCursor() != null);

        return items
                .stream()
                .map(this.itemMapper::apiToEntity)
                .toList();
    }

    @RateLimiter(name = "readwise-api")
    private ReaderListResponse fetchPage(String accessToken, ReadwiseFetchPaginationParams params)
            throws IOException, InterruptedException, TooManyRequestsException {
        log.info("Fetching Readwise page with params: {}", params);

        String url = readewiseReaderListUrl + params.toQueryParams();
        log.debug("url: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .header("Authorization", "Token " + accessToken)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        logResponse(response);

        switch (response.statusCode()) {
            case 200:
                String body = response.body();

                return mapper.readValue(body, ReaderListResponse.class);
            case 429:
                String retryAfter = response
                        .headers()
                        .map()
                        .get("Retry-After")
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Not got `Retry-After` header from Reader API."));

                log.info("Got RetryAfter={}", retryAfter);

                updateAfter(Integer.valueOf(retryAfter));
                throw new TooManyRequestsException(Integer.valueOf(retryAfter));
            case 400:
            case 401:
            case 403:
            case 500:
            case 504:
            default:
                throw new IllegalStateException(
                        MessageFormat.format("Not expected http response code {0}", response.statusCode()));
        }
    }

    private void logResponse(HttpResponse<String> response) {
        log.debug("status: {}", response.statusCode());

        // print response body
        if (response.statusCode() != HttpStatus.OK.value()) {
            log.debug("response: {}", response.body());
        }
    }
}
