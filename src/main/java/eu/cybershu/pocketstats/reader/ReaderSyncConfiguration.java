package eu.cybershu.pocketstats.reader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReaderSyncConfiguration {
    private final String accessToken;

    public ReaderSyncConfiguration(@Value("${READER_ACCESS_TOKEN:}") String accessToken) {
        this.accessToken = accessToken == null ? "" : accessToken.strip();
    }

    public boolean isAvailable() {
        return !accessToken.isBlank();
    }

    public String accessToken() {
        return accessToken;
    }
}
