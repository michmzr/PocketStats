package eu.cybershu.pocketstats.stats;

import java.util.Map;

public record LangStats(
        Map<String, Long> langCount
) {
}
