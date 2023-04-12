package eu.cybershu.pocketstats.auth;

import java.io.Serializable;

public record AuthorizationStatus(
        boolean status) implements Serializable {
}
