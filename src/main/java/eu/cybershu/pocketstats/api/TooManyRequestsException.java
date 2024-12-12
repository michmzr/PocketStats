package eu.cybershu.pocketstats.api;

import lombok.Getter;

@Getter
public class TooManyRequestsException extends Exception {
    private final Integer retryAfter;

    public TooManyRequestsException(Integer retryAfterSeconds) {
        this.retryAfter = retryAfterSeconds;
    }

    public TooManyRequestsException(String message, Integer retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

}
