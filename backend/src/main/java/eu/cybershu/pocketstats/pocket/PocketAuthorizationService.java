package eu.cybershu.pocketstats.pocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
public class PocketAuthorizationService {
    public static final String CRED_FILE_PATH = "./pocket-creds.json";
    private final HttpClient client;
    private final ObjectMapper mapper;

    @Value("${auth.pocket.consumer-key}")
    private String pocketConsumerKey;

    @Value("${auth.pocket.redirect_uri}")
    private String pocketRedirectUrl;

    @Value("${auth.pocket.url.request}")
    private String pocketRequestUrl;

    @Value("${auth.pocket.url.authorize_app}")
    private String pocketAuthorizeUrl;

    @Value("${auth.pocket.url.access_token}")
    private String pocketAccessTokenRetrieveUrl;

    private UserCurrentlyAuthorising userCurrentlyAuthorising;

    public PocketAuthorizationService() {
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20)).build();
        this.mapper = new ObjectMapper();
    }

    public String generateSessionId() {
        return String.valueOf(new Random().nextInt());
    }

    /**
     * @return authorisation code
     * @link <a href="https://getpocket.com/developer/docs/authentication">pocket api auth</a>
     */
    public String obtainAuthCode(String sessionId) throws IOException, InterruptedException {
        Map<Object, Object> data = new HashMap<>();
        data.put("consumer_key", pocketConsumerKey);
        data.put("redirect_uri", getPocketRedirectUrl(sessionId));

        String payload = mapper.writeValueAsString(data);
        HttpRequest request = HttpRequest
                .newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .uri(URI.create(pocketRequestUrl))
                .header("Content-Type", "application/json; charset=UTF8")
                .header("X-accept", "application/json").build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // print status code
        log.debug("status: {}", response.statusCode());

        // print response body
        log.debug("response: {}", response.body());

        if (response.statusCode() == 200) {
            var responseCode = mapper.readTree(response.body());
            return responseCode.get("code").asText();
        } else {
            throw new IllegalArgumentException("Not acquired code");
        }
    }


    public boolean isAuthSessionActive() {
        return this.userCurrentlyAuthorising != null;
    }

    public void startAuthProcess(String sessionId, String code, String loginLink) {
        log.info("Starting auth process - sessionId: {}", sessionId);

        if (this.userCurrentlyAuthorising != null)
            clearAuthProcess();

        this.userCurrentlyAuthorising = UserCurrentlyAuthorising.of(
                sessionId, code, loginLink
        );
    }

    public void completeAuthProcess() throws IOException, InterruptedException {
        log.info("Completing auth process - {}...", this.userCurrentlyAuthorising);

        if (this.userCurrentlyAuthorising == null) {
            throw new IllegalStateException("Another auth process is active. Complete one or reboot application");
        }

        log.info("Acquiring access token");
        final String accessToken = authorize(this.userCurrentlyAuthorising.code());

        log.info("Acquired access token. Saving access token and code to file");
        save(new PocketUserCredentials(this.userCurrentlyAuthorising.code(), accessToken));

        clearAuthProcess();
    }

    public void clearAuthProcess() {
        this.userCurrentlyAuthorising = null;
    }

    public String authorize(String code) throws IOException, InterruptedException {
        Map<Object, Object> data = new HashMap<>();
        data.put("consumer_key", pocketConsumerKey);
        data.put("code", code);

        String payload = mapper.writeValueAsString(data);
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .uri(URI.create(pocketAccessTokenRetrieveUrl))
                .header("Content-Type", "application/json; charset=UTF8")
                .header("X-accept", "application/json").build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // print status code
        log.debug("status: {}", response.statusCode());

        // print response body
        log.debug("response: {}", response.body());

        if (response.statusCode() == 200) {
            var responseCode = mapper.readTree(response.body());
            return responseCode.get("access_token").asText();
        } else {
            throw new IllegalArgumentException("Not acquired access token.");
        }
    }

    public String generateLoginUrl(String code, String sessionId) {
        return String.format(this.pocketAuthorizeUrl + "?request_token=%s&redirect_uri=%s",
                code, getPocketRedirectUrl(sessionId));
    }

    private String getPocketRedirectUrl(String id) {
        return pocketRedirectUrl + "/" + id;
    }

    public void save(PocketUserCredentials pocketUserCredentials) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(CRED_FILE_PATH)) {
            fileOutputStream.write(mapper.writeValueAsBytes(pocketUserCredentials));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<PocketUserCredentials> getCredentials() {
        Path path = Paths.get(CRED_FILE_PATH);

        if (!Files.exists(path)) {
            return Optional.empty();
        }

        try (FileInputStream stream = new FileInputStream(CRED_FILE_PATH)) {
            return Optional.of(mapper.readValue(stream.readAllBytes(), PocketUserCredentials.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
