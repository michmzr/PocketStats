package eu.cybershu.pocketstats.stats;

import java.util.List;

public record DayStatsRecords(
        List<DayStat> stats, StatsWithStatusType type
) {
}
