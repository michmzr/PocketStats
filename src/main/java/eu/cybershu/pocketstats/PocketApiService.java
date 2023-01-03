package eu.cybershu.pocketstats;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.cybershu.pocketstats.db.PocketItemMapper;
import eu.cybershu.pocketstats.db.PocketItemRepository;
import eu.cybershu.pocketstats.model.api.ListItem;
import eu.cybershu.pocketstats.model.api.PocketGetResponse;
import eu.cybershu.pocketstats.stats.PocketStatPredicate;
import eu.cybershu.pocketstats.stats.ToReadPredicate;
import eu.cybershu.pocketstats.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PocketApiService {
    private final HttpClient client;
    private final ObjectMapper mapper;

    @Value("${auth.pocket.consumer-key}")
    private String pocketConsumerKey;

    @Value("${auth.pocket.redirect_uri}")
    private String pocketRedirectUrl;

    @Value("${auth.pocket.url.request}")
    private String pocketRequestUrl;

    @Value("${auth.pocket.url.authorize}")
    private String pocketAuthorizeUrl;

    @Value("${auth.pocket.url.get}")
    private String pocketGetUrl;

    @Autowired
    List<PocketStatPredicate> statPredicates;

    @Autowired
    PocketItemRepository pocketItemRepository;

    PocketItemMapper pocketItemMapper;

    private final Set<String> activeAuthSessions ;

    public PocketApiService() {
        this.client =  HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.mapper =  new ObjectMapper();
        this.activeAuthSessions = ConcurrentHashMap.newKeySet();

        pocketItemMapper = PocketItemMapper.INSTANCE;
    }



    /**
     * @return authorisation code
     * @link <a href="https://getpocket.com/developer/docs/authentication">pocket api auth</a>
     */
    public String obtainAuthCode() throws IOException, InterruptedException {
        Map<Object, Object> data = new HashMap<>();
        data.put("consumer_key", pocketConsumerKey);
        data.put("redirect_uri", pocketRedirectUrl);

        String payload = mapper.writeValueAsString(data);
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .uri(URI.create(pocketRequestUrl))
                .header("Content-Type", "application/json; charset=UTF8")
                .header("X-accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // print status code
        log.debug("status: {}", response.statusCode());

        // print response body
        log.debug("response: {}", response.body());

        if(response.statusCode() == 200) {
            var responseCode = mapper.readTree(response.body());
            return responseCode.get("code").asText();
        } else {
            throw new IllegalArgumentException("Not acquired code");
        }
    }

    private static ZoneId getZoneId() {
        return ZoneId.systemDefault();
    }

    public void registerAuthSession(String sessionId) {
        activeAuthSessions.add(sessionId);
    }

    public void deregisterAuthSession(String sessionId) {
        activeAuthSessions.remove(sessionId);
    }

    public boolean isAuthSessionActive(String sessionId) {
        return activeAuthSessions.contains(sessionId);
    }

    public String authorize(String code) throws IOException, InterruptedException {
        Map<Object, Object> data = new HashMap<>();
        data.put("consumer_key", pocketConsumerKey);
        data.put("code", code);

        String payload = mapper.writeValueAsString(data);
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .uri(URI.create(pocketGetUrl))
                .header("Content-Type", "application/json; charset=UTF8")
                .header("X-accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // print status code
        log.debug("status: {}", response.statusCode());

        // print response body
        log.debug("response: {}", response.body());

        if(response.statusCode() == 200) {
            var responseCode = mapper.readTree(response.body());
            return responseCode.get("access_token").asText();
        } else {
            throw new IllegalArgumentException("Not acquired access token.");
        }
    }

    public String generateLoginUrl(String code){
        return String.format(this.pocketAuthorizeUrl + "?request_token=%s&redirect_uri=%s", code,
                pocketRedirectUrl + "?sessionId="+code);
    }

    //Items to read
    public int itemsToRead(String accessToken) throws IOException, InterruptedException {
        Map<Object, Object> data = new HashMap<>();
        data.put("consumer_key", pocketConsumerKey);
        data.put("access_token",accessToken);
        data.put("state", "unread");
        data.put("detailType", "complete");

        String payload = mapper.writeValueAsString(data);
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .uri(URI.create(pocketGetUrl))
                .header("Content-Type", "application/json")
                .header("X-accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // print status code
        log.debug("status: {}", response.statusCode());

        // print response body
        log.debug("response: {}", response.body());

        if(response.statusCode() == 200) {
            var pocketResponse = mapper.readValue(response.body(), PocketGetResponse.class);

            ToReadPredicate predicate = new ToReadPredicate();
            var items = pocketResponse.getItems();
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

    public Integer importAllToDbSince(String accessToken, Instant sinceWhen) throws IOException, InterruptedException {
        var pocketResponse = sinceWhen(accessToken, sinceWhen);

        var models =  pocketResponse
                .getItems()
                .values()
                .stream()
                .map(it -> pocketItemMapper.apiModelToDb(it))
                .toList();


        return pocketItemRepository.saveAll(models).size();
    }

    public Map<PocketStatPredicate, Integer> getLastYearItems(String accessToken) throws IOException,
            InterruptedException {
        ZoneId zoneId = getZoneId();
        Instant sinceWhen = TimeUtils.getFirstDayOfLastYear().atZone(zoneId).toInstant();
        //todo end date - last day of year
        return calcStatsSinceWhen(accessToken, sinceWhen);
    }

    public Map<PocketStatPredicate, Integer> getCurrentMonth(String accessToken) throws IOException, InterruptedException {
        ZoneId zoneId = getZoneId();
        Instant sinceWhen = TimeUtils.getStartOfCurrentMonth().atZone(zoneId).toInstant();
        return calcStatsSinceWhen(accessToken, sinceWhen);
    }

    public Map<PocketStatPredicate, Integer> calcStatsSinceWhen(String accessToken, Instant sinceWhen) throws IOException,
            InterruptedException {
        var pocketResponse = sinceWhen(accessToken, sinceWhen);

        Map<PocketStatPredicate, Integer> stats = new HashMap<>();
        statPredicates.forEach(predicate -> {
            stats.put(predicate, 0);
        });

        var items = pocketResponse.getItems();
            items.forEach((itemId, item) -> {
                        try{
                            statPredicates.forEach(predicate ->
                                    stats.compute(predicate, (k, oldValue) ->
                                            predicate.test(item, sinceWhen) ? oldValue +1 : oldValue));
                        }catch (Exception e) {
                            log.error("exception catched:", e);
                        }
            }
        );

        return stats;
    }

    public PocketGetResponse sinceWhen(String accessToken, Instant sinceWhen) throws IOException,
            InterruptedException {
        Map<Object, Object> data = new HashMap<>();
        data.put("consumer_key", pocketConsumerKey);
        data.put("access_token",accessToken);
        data.put("since", sinceWhen.getEpochSecond());
        data.put("detailType", "complete");

        String payload = mapper.writeValueAsString(data);
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .uri(URI.create(pocketGetUrl))
                .header("Content-Type", "application/json")
                .header("X-accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // print status code
        log.debug("status: {}", response.statusCode());

        // print response body
        log.debug("response: {}", response.body());

        if(response.statusCode() == 200) {
            var pocketResponse = mapper.readValue(response.body(), PocketGetResponse.class);

            return pocketResponse;
        } else {
            throw new IllegalArgumentException("Not acquired access token.");
        }
    }


}
