package eu.cybershu.pocketstats.stats;

import eu.cybershu.pocketstats.model.api.ListItem;

import java.time.Instant;

public interface PocketStatPredicate {
    public String getName();

    public Boolean test(ListItem item, Instant sinceWhen);
}