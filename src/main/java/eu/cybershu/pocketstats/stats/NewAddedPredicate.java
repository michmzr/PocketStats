package eu.cybershu.pocketstats.stats;

import eu.cybershu.pocketstats.model.ListItem;
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
        if(item.getTimeAdded() == null)
            return false;

        return item.getTimeAdded().isAfter(sinceWhen);
    }

    @Override
    public int hashCode() {
        return getName().hashCode() + getClass().hashCode();
    }
}
