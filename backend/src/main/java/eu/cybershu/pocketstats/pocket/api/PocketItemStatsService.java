package eu.cybershu.pocketstats.pocket.api;

import com.mongodb.client.AggregateIterable;
import eu.cybershu.pocketstats.stats.DayStat;
import eu.cybershu.pocketstats.stats.DayStatsRecords;
import eu.cybershu.pocketstats.stats.DayStatsType;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Service
public class PocketItemStatsService {
    private final MongoTemplate mongoTemplate;

    public PocketItemStatsService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public DayStatsRecords getDayStatsRecords(LocalDate start, LocalDate end, DayStatsType type) {
        log.info("Calculating {} items status by day from {} to {}", type, start, end);

        var collection = mongoTemplate.getCollection("pocketItem");

        Date gteDate = Date.from(start.atStartOfDay()
                                      .atZone(ZoneId.systemDefault())
                                      .toInstant());
        Date ltDate = Date.from(end.atTime(23, 59, 59)
                                   .atZone(ZoneId.systemDefault())
                                   .toInstant());

        String timeFieldName = getTimeFieldForAggregation(type);

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

        return new DayStatsRecords(days, type);
    }

    private String getTimeFieldForAggregation(DayStatsType type) {
        return switch (type) {
            case ARCHIVED -> "timeRead";
            case TODO -> "timeAdded";
            case DELETED -> throw new UnsupportedOperationException("Aggregation by DELETE status is not supported.");
        };
    }
}
