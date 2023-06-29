package eu.cybershu.pocketstats.pocket.api;

import javax.validation.constraints.NotNull;

public record ActivityHeatmapItem(
        /* 0-23 */
        int hour,
        /* 1-monday, 7- sunday */
        int weekday,
        @NotNull long count) {
}
