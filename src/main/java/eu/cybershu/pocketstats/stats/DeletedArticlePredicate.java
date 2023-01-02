package eu.cybershu.pocketstats.stats;

import eu.cybershu.pocketstats.model.ItemStatus;
import eu.cybershu.pocketstats.model.ListItem;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DeletedArticlePredicate implements PocketStatPredicate{
    @Override
    public String getName() {
        return "Deleted items";
    }

    @Override
    public Boolean test(ListItem item, Instant sinceWhen) {
        return item.getStatus() == ItemStatus.DELETED;
    }

    @Override
    public int hashCode() {
        return getName().hashCode() + getClass().hashCode();
    }
}
