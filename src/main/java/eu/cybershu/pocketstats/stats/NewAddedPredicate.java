package eu.cybershu.pocketstats.stats;

import eu.cybershu.pocketstats.pocket.api.ListItem;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class NewAddedPredicate implements PocketStatPredicate{
    @Override
    public String getName() {
        return "Added articles";
    }

    @Override
    public Boolean test(ListItem item, Instant sinceWhen) {
        if (item.timeAdded() == null) return false;

        return item.timeAdded().isAfter(sinceWhen);
    }

    @Override
    public int hashCode() {
        return getName().hashCode() + getClass().hashCode();
    }
}
