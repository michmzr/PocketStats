package eu.cybershu.pocketstats.reader.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.cybershu.pocketstats.pocket.api.ApiXHeaders;
import eu.cybershu.pocketstats.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Service for interacting with the Readwise Reader API: https://readwise.io/reader_api
 */
@Slf4j
@Service
public class ReaderApiService {
    private final String readewiseReaderListUrl = "https://readwise.io/api/v3/list/?";

    private final HttpClient client;

    private final ObjectMapper mapper;

    public ReaderApiService() {
        this.client = HttpClient
                .newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(60))
                .build();
        this.mapper = new ObjectMapper();
        this.mapper
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
                .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * Key	Type	Description	Required
     * id	string	The document's unique id. Using this parameter it will return just one document, if found.	no
     * updatedAfter	string (formatted as ISO 8601 date)	Fetch only documents updated after this date	no
     * location	string	The document's location, could be one of: new, later, shortlist, archive, feed	no
     * category	string	The document's category, could be one of: article, email, rss, highlight, note, pdf, epub, tweet, video	no
     * pageCursor	string	A string returned by a previous request to this endpoint. Use it to get the next page of documents if there are too many for one request.	no
     */
    public List<ReaderItem> fetchList(String accessToken, ReadwiseFetchParams params) throws IOException, InterruptedException {
        log.info("Fetching Readwise list with params: {}", params);

        List<ReaderItem> items = new LinkedList<>();
        ReadwiseFetchPaginationParams pageParams = ReadwiseFetchPaginationParams
                .builder()
                .category(params.category())
                .location(params.location())
                .pageCursor(null)
                .build();

        do {
            ReaderListResponse response = fetchPage(accessToken, pageParams);
            items.addAll(response.results());

            pageParams = ReadwiseFetchPaginationParams
                    .builder()
                    .category(params.category())
                    .location(params.location())
                    .pageCursor(response.nextPageCursor())
                    .build();
        } while (pageParams.pageCursor() != null);

        return items;
    }

    private ReaderListResponse fetchPage(String accessToken, ReadwiseFetchPaginationParams params) throws IOException, InterruptedException {
        log.info("Fetching Readwise list with params: {}", params);

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
            case 401:
                break;
            case 400:
            case 403:
            case 500:
            case 504:
            default:
                throw new IllegalStateException("Not expected http response code " + response.statusCode());
        }

        throw new IllegalStateException("Not expected http response code " + response.statusCode());
    }

    private void logResponse(HttpResponse<String> response) {
        log.debug("status: {}", response.statusCode());

        // print response body
        if (response.statusCode() != HttpStatus.OK.value()) {
            log.debug("response: {}", response.body());
        }
    }
}
