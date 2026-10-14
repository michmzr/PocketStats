package eu.cybershu.pocketstats.stats.library;

import eu.cybershu.pocketstats.db.Item;
import eu.cybershu.pocketstats.db.ItemStatus;
import eu.cybershu.pocketstats.db.Source;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.stream.Stream;

@Service
public class LibraryAnalyticsService {
    private final MongoTemplate mongo;
    private final LibraryAnalyticsCalculator calculator;
    private final Clock clock;

    public LibraryAnalyticsService(MongoTemplate mongo, LibraryAnalyticsCalculator calculator, Clock clock) {
        this.mongo = mongo;
        this.calculator = calculator;
        this.clock = clock;
    }

    public LibraryAnalytics getAnalytics(int days, Source source) {
        Instant asOf = clock.instant();
        Query query = Query.query(Criteria.where("status").in(ItemStatus.TO_READ, ItemStatus.ARCHIVED));
        if (source != null) query.addCriteria(Criteria.where("source").is(source));
        query.fields().include("status", "source", "category", "url", "timeAdded", "timeRead").exclude("_id");
        try (Stream<Item> items = mongo.stream(query, Item.class)) {
            return calculator.calculate(items, asOf, days, source);
        }
    }
}
