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

    private Boolean isUserAuthorisating;

    public PocketAuthorizationService() {
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20)).build();
        this.mapper = new ObjectMapper();
        this.isUserAuthorisating = false;
    }

    /**
     * @return authorisation code
     * @link <a href="https://getpocket.com/developer/docs/authentication">pocket api auth</a>
     */
    public String obtainAuthCode() throws IOException, InterruptedException {
        Map<Object, Object> data = new HashMap<>();
        data.put("consumer_key", pocketConsumerKey);
        data.put("redirect_uri", getPocketRedirectUrl());

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

    public void registerAuthSession() {
        isUserAuthorisating = true;
    }

    public void deregisterAuthSession() {
        isUserAuthorisating = false;
    }

    public boolean isAuthSessionActive() {
        return isUserAuthorisating;
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

    public String generateLoginUrl(String code) {
        return String.format(this.pocketAuthorizeUrl + "?request_token=%s&redirect_uri=%s",
                code, getPocketRedirectUrl());
    }

    private String getPocketRedirectUrl() {
        return pocketRedirectUrl;
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
