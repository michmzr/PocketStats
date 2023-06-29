package eu.cybershu.pocketstats.pocket.api;

import java.util.List;

public record ActivityHeatmapStats(
        List<ActivityHeatmapItem> items
) {
}
