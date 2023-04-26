package eu.cybershu.pocketstats.pocket.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemsStatsAggregated {
    private final List<ItemsStatsPerPeriod> itemsStats = new ArrayList<>();

    public void addStat(ItemsStatsPerPeriod statsPerPeriod) {
        itemsStats.add(statsPerPeriod);
    }

    public List<ItemsStatsPerPeriod> getItemsStats() {
        return itemsStats;
    }

    public Optional<ItemsStatsPerPeriod> findByName(String name) {
        return itemsStats.stream().filter(it -> it.nameShort().equals(name)).findFirst();
    }
}
