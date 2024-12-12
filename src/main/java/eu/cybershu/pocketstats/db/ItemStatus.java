package eu.cybershu.pocketstats.db;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ItemStatus {
    TO_READ(0),
    ARCHIVED(1),
    DELETED(2);

    private final int status;

    ItemStatus(int status) {
        this.status = status;
    }

    @JsonValue
    public int getStatus() {
        return status;
    }
}
