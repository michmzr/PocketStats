package eu.cybershu.pocketstats.pocket.api;

import java.util.List;

public record ItemsStatsAggregated(
        List<ItemsStatsPerPeriod> itemsStats) {
}
