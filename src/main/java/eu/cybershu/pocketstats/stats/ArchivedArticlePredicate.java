package eu.cybershu.pocketstats.stats;

import eu.cybershu.pocketstats.pocket.api.ListItem;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ArchivedArticlePredicate implements PocketStatPredicate{
    @Override
    public String getName() {
        return "Archived articles";
    }

    @Override
    public Boolean test(ListItem item, Instant sinceWhen) {
        return item.timeRead() != null && item.timeRead().isAfter(sinceWhen);
    }

    @Override
    public int hashCode() {
        return getName().hashCode() + getClass().hashCode();
    }
}
