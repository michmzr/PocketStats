package eu.cybershu.pocketstats.pocket.api;

import eu.cybershu.pocketstats.utils.TimePeriod;

public record ItemsStatsPerPeriod(
        String nameShort,
        String nameDesc,
        PeriodItemsStats stats,
        TimePeriod period
) {
}
