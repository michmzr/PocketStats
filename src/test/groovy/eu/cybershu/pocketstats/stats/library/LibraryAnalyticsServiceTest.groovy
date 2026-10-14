package eu.cybershu.pocketstats.stats.library

import eu.cybershu.pocketstats.db.*
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import spock.lang.Specification
import java.time.*
import java.util.concurrent.atomic.AtomicBoolean

class LibraryAnalyticsServiceTest extends Specification {
    def mongo = Mock(MongoTemplate)
    def now = Instant.parse('2026-10-14T12:00:00Z')
    def clock = Clock.fixed(now, ZoneOffset.UTC)

    def 'read only service filters and projects Mongo records and always closes the stream'() {
        given:
        def closed = new AtomicBoolean()
        def records = [new Item(status: ItemStatus.TO_READ, source: Source.POCKET)].stream().onClose { closed.set(true) }
        def service = new LibraryAnalyticsService(mongo, new LibraryAnalyticsCalculator(), clock)
        when:
        def result = service.getAnalytics(30, Source.POCKET)
        then:
        1 * mongo.stream({ Query q ->
            q.queryObject.source == Source.POCKET &&
                    q.queryObject.status['$in'] == [ItemStatus.TO_READ, ItemStatus.ARCHIVED] &&
                    q.fieldsObject == [status: 1, source: 1, category: 1, url: 1, timeAdded: 1, timeRead: 1, _id: 0]
        }, Item) >> records
        0 * mongo._
        closed.get()
        result.asOf() == now
        result.backlog().total() == 1
    }

    def 'stream also closes when processing fails'() {
        given:
        def closed = new AtomicBoolean()
        def records = [null].stream().onClose { closed.set(true) }
        def service = new LibraryAnalyticsService(mongo, new LibraryAnalyticsCalculator(), clock)
        when:
        service.getAnalytics(7, null)
        then:
        1 * mongo.stream(_, Item) >> records
        0 * mongo._
        thrown(NullPointerException)
        closed.get()
    }
}
