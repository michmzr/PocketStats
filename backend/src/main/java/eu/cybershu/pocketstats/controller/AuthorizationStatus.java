package eu.cybershu.pocketstats.controller;

import java.io.Serializable;

public record AuthorizationStatus(
        boolean status) implements Serializable {
}
