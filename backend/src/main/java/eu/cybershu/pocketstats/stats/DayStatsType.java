package eu.cybershu.pocketstats.stats;

import eu.cybershu.pocketstats.pocket.api.ItemStatus;

public enum DayStatsType {
    ARCHIVED,
    DELETED,
    TODO;

    public ItemStatus toItemStatus() {
        return switch (this) {
            case ARCHIVED -> ItemStatus.ARCHIVED;
            case DELETED -> ItemStatus.DELETED;
            case TODO -> ItemStatus.TO_READ;
        };
    }
}
