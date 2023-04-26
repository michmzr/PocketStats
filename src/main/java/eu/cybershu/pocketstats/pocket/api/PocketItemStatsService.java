package eu.cybershu.pocketstats.pocket.api;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import eu.cybershu.pocketstats.events.UserSynchronizedItemsEvent;
import eu.cybershu.pocketstats.stats.DayStat;
import eu.cybershu.pocketstats.stats.DayStatsRecords;
import eu.cybershu.pocketstats.stats.DayStatsType;
import eu.cybershu.pocketstats.stats.TopTag;
import eu.cybershu.pocketstats.utils.TimePeriod;
import eu.cybershu.pocketstats.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonNull;
import org.bson.Document;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class PocketItemStatsService {
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    public PocketItemStatsService(
            MongoTemplate mongoTemplate
    ) {
        this.clock = Clock.systemUTC();
        this.mongoTemplate = mongoTemplate;
    }

    @Cacheable("stats-day-records")
    public DayStatsRecords getDayStatsRecords(LocalDate start, LocalDate end, DayStatsType type) {
        log.info("Calculating {} items status by day from {} to {}", type, start, end);

        final Date gteDate = Date.from(start.atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
        final Date ltDate = Date.from(end.atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault())
                .toInstant());

        String timeFieldName = getTimeFieldForAggregation(type);

        var collection = getPocketItemsCollection();
        AggregateIterable<Document> resultsIter = collection.aggregate(Arrays.asList(new Document("$match",
                        new Document("status", type.toItemStatus()
                                .name())
                                .append(timeFieldName,
                                        new Document("$gte", gteDate).append("$lte", ltDate))),
                new Document("$group",
                        new Document("_id",
                                new Document("$dateToString",
                                        new Document("format", "%d-%m-%Y")
                                                .append("date", "$" + timeFieldName)))
                                .append("items",
                                        new Document("$sum", 1L)))));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-y");
        List<DayStat> days = new LinkedList<>();
        for (Document docs : resultsIter) {
            String day = docs.getString("_id");
            Long items = docs.getLong("items");
            days.add(new DayStat(
                    LocalDate.parse(day, formatter), items
            ));
        }

        days.sort(Comparator.comparing(DayStat::day));

        return new DayStatsRecords(days, type);
    }

    private String getTimeFieldForAggregation(DayStatsType type) {
        return switch (type) {
            case ARCHIVED -> "timeRead";
            case TODO -> "timeAdded";
            case DELETED -> throw new UnsupportedOperationException("Aggregation by DELETE status is not supported.");
        };
    }

    @Cacheable("stats-top-tags")
    public List<TopTag> getTopTags(Integer number) {
        log.info("Calculating top {} tags", number);

        var collection = getPocketItemsCollection();
        AggregateIterable<Document> resultsIter = collection.aggregate(Arrays.asList(new Document("$unwind",
                        new Document("path", "$tags")),
                new Document("$group",
                        new Document("_id", "$tags")
                                .append("count",
                                        new Document("$sum", 1L))),
                new Document("$sort",
                        new Document("count", -1L)),
                new Document("$limit", number)
        ));

        List<TopTag> topTags = new ArrayList<>(number);

        for (Document docs : resultsIter) {
            String name = docs.getString("_id");
            long count = docs.getLong("count");

            topTags.add(
                    new TopTag(name, count)
            );
        }

        return topTags;
    }

    @Cacheable("stats-periods-aggregated")
    public ItemsStatsAggregated itemsStatsAggregated() {
        log.info("Aggregating stats per period....");

        //current week
        TimePeriod currWeek = TimePeriod.currentWeek(clock());
        CompletableFuture<ItemsStatsPerPeriod> currWeekFuture = CompletableFuture.supplyAsync(() ->
                new ItemsStatsPerPeriod("current-week",
                        "Current week", itemsStatsPeriod(currWeek), currWeek));

        //last week
        TimePeriod lastWeek = TimePeriod.previousWeek(clock());
        CompletableFuture<ItemsStatsPerPeriod> lastWeekFuture = CompletableFuture.supplyAsync(() ->
                new ItemsStatsPerPeriod("last-week",
                        "Last week", itemsStatsPeriod(lastWeek), lastWeek));

        //current month
        TimePeriod currentMonth = TimePeriod.currentMonth(clock());
        CompletableFuture<ItemsStatsPerPeriod> currentMonthFuture = CompletableFuture.supplyAsync(() ->
                new ItemsStatsPerPeriod("current-month",
                        "Current month", itemsStatsPeriod(currentMonth), currentMonth));

        //last  month
        TimePeriod lastMonth = TimePeriod.lastMonth(clock());
        CompletableFuture<ItemsStatsPerPeriod> lastMonthFuture = CompletableFuture.supplyAsync(() ->
                new ItemsStatsPerPeriod("last-month",
                        "last month", itemsStatsPeriod(lastMonth), lastMonth));

        //current year
        TimePeriod currentYear = TimePeriod.currentYear(clock());
        CompletableFuture<ItemsStatsPerPeriod> currentYearFuture = CompletableFuture.supplyAsync(() ->
                new ItemsStatsPerPeriod("current-year",
                        "Current year", itemsStatsPeriod(currentYear), currentYear));

        //last year
        TimePeriod lastYear = TimePeriod.lastYear(clock());
        CompletableFuture<ItemsStatsPerPeriod> lastYearFuture = CompletableFuture.supplyAsync(() ->
                new ItemsStatsPerPeriod("last-year",
                        "Last year", itemsStatsPeriod(lastYear), lastYear));

        //total
        CompletableFuture<ItemsStatsPerPeriod> totalFuture = CompletableFuture.supplyAsync(() ->
                new ItemsStatsPerPeriod("total",
                        "Total", itemsStatsTotal(), null));

        CompletableFuture.allOf(currWeekFuture, lastWeekFuture, currentMonthFuture,
                lastMonthFuture, currentYearFuture, lastYearFuture, totalFuture).join();

        final ItemsStatsAggregated itemsStatsAggregated = new ItemsStatsAggregated();
        try {
            itemsStatsAggregated.addStat(currWeekFuture.get());
            itemsStatsAggregated.addStat(lastWeekFuture.get());
            itemsStatsAggregated.addStat(currentMonthFuture.get());
            itemsStatsAggregated.addStat(lastMonthFuture.get());
            itemsStatsAggregated.addStat(currentYearFuture.get());
            itemsStatsAggregated.addStat(lastYearFuture.get());
            itemsStatsAggregated.addStat(totalFuture.get());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting parallelized results", e);
        }

        return itemsStatsAggregated;
    }

    public PeriodItemsStats itemsStatsPeriod(TimePeriod timePeriod) {
        log.info("Calculating items stats for period: {}", timePeriod);
        Instant begin = TimeUtils.toStartDayInstant(timePeriod.begin());
        Instant end = TimeUtils.toEndOfDayInstant(timePeriod.end());

        var collection = getPocketItemsCollection();
        AggregateIterable<Document> result = collection.aggregate(Arrays.asList(new Document("$match",
                        new Document("$or", Arrays.asList(new Document("timeAdded",
                                        new Document("$gte",
                                                begin)
                                                .append("$lte",
                                                        end)),
                                new Document("timeRead",
                                        new Document("$gte",
                                                begin)
                                                .append("$lte",
                                                        end))))),
                new Document("$group",
                        new Document("_id",
                                new BsonNull())
                                .append("added",
                                        new Document("$sum",
                                                new Document("$cond", Arrays.asList(new Document("$and", Arrays.asList(new Document("$gte", Arrays.asList("$timeAdded",
                                                                begin)),
                                                        new Document("$lte", Arrays.asList("$timeAdded",
                                                                end)))), 1L, 0L))))
                                .append("archived",
                                        new Document("$sum",
                                                new Document("$cond", Arrays.asList(new Document("$and", Arrays.asList(new Document("$gte", Arrays.asList("$timeRead",
                                                                begin)),
                                                        new Document("$lte", Arrays.asList("$timeRead",
                                                                end)))), 1L, 0L))))),
                new Document("$project",
                        new Document("_id", 0L)
                                .append("added", 1L)
                                .append("archived", 1L))));

        Document docs = result.first();
        Objects.requireNonNull(docs);

        return new PeriodItemsStats(
                docs.getLong("added"), docs.getLong("archived")
        );
    }

    public PeriodItemsStats itemsStatsTotal() {
        log.info("Counting items per status for whole dataset...");

        var collection = getPocketItemsCollection();
        AggregateIterable<Document> result = collection.aggregate(List.of(new Document("$group",
                new Document("_id", "$status")
                        .append("count",
                                new Document("$sum", 1L)))));

        Map<ItemStatus, Long> itemStats = new HashMap<>();
        for (Document docs : result) {
            String name = docs.getString("_id");
            long count = docs.getLong("count");
            itemStats.put(ItemStatus.valueOf(name), count);
        }

        return new PeriodItemsStats(
                itemStats.get(ItemStatus.TO_READ) + itemStats.get(ItemStatus.ARCHIVED),
                itemStats.get(ItemStatus.ARCHIVED));
    }

    private MongoCollection<Document> getPocketItemsCollection() {
        return mongoTemplate.getCollection("pocketItem");
    }

    @Cacheable("stats-lang-stats")
    public Map<String, Long> getLangStats() {
        log.info("Calculating lang stats");

        var collection = getPocketItemsCollection();
        AggregateIterable<Document> resultsIter = collection.aggregate(Arrays.asList(new Document("$group",
                        new Document("_id", "$lang")
                                .append("count",
                                        new Document("$sum", 1L))),
                new Document("$sort",
                        new Document("count", -1L))
        ));

        Map<String, Long> langStats = new HashMap<>();
        for (Document docs : resultsIter) {
            String name = docs.getString("_id");
            long count = docs.getLong("count");

            if (!StringUtils.hasLength(name))
                name = "unknown";

            langStats.put(
                    name, count
            );
        }

        return langStats;
    }

    @EventListener
    @CacheEvict(value = {"stats-day-records", "stats-top-tags", "stats-lang-stats", "stats-periods-aggregated"}, allEntries = true)
    public void handleUserRemovedEvent(UserSynchronizedItemsEvent event) {
        log.info("User synchronized items with GetPocket: {}", event.getSyncStatus());
    }

    private Clock clock() {
        return this.clock;
    }
}
