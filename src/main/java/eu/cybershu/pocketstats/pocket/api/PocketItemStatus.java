package eu.cybershu.pocketstats.pocket.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PocketItemStatus {
    TO_READ(0),
    ARCHIVED(1),
    DELETED(2);

    private final int status;

    PocketItemStatus(int status) {
        this.status = status;
    }

    @JsonValue
    public int getStatus() {
        return status;
    }

    @JsonCreator
    public static PocketItemStatus forValue(int status) {
        for (PocketItemStatus itemStatus : PocketItemStatus.values()) {
            if (itemStatus.getStatus() == status) {
                return itemStatus;
            }
        }

        throw new IllegalArgumentException("Unknown status: " + status);
    }
}
