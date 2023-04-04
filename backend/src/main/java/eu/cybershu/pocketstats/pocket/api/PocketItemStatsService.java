package eu.cybershu.pocketstats.pocket.api;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import eu.cybershu.pocketstats.stats.DayStat;
import eu.cybershu.pocketstats.stats.DayStatsRecords;
import eu.cybershu.pocketstats.stats.DayStatsType;
import eu.cybershu.pocketstats.stats.TopTag;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class PocketItemStatsService {
    private final MongoTemplate mongoTemplate;

    public PocketItemStatsService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

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

    private MongoCollection<Document> getPocketItemsCollection() {
        return mongoTemplate.getCollection("pocketItem");
    }

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
}
