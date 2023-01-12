package eu.cybershu.pocketstats.stats;

import eu.cybershu.pocketstats.pocket.api.ItemStatus;
import eu.cybershu.pocketstats.pocket.api.ListItem;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ToReadPredicate implements PocketStatPredicate{
    @Override
    public String getName() {
        return "To read";
    }

    @Override
    public Boolean test(ListItem item, Instant sinceWhen) {
        return item.status().equals(ItemStatus.TO_READ);
    }
}
