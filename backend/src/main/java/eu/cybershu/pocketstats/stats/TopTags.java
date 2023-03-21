package eu.cybershu.pocketstats.stats;

import java.util.List;

public record TopTags(
        List<TopTag> tags,
        int count
) {
}
