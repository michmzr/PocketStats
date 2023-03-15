package eu.cybershu.pocketstats.stats;

import java.time.LocalDate;

public record DayStat(
        LocalDate day,
        Long number
) {
}
