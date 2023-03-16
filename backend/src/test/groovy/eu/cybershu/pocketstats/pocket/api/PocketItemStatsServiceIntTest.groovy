package eu.cybershu.pocketstats.pocket.api

import eu.cybershu.pocketstats.db.PocketItemRepository
import eu.cybershu.pocketstats.stats.DayStatsRecords
import eu.cybershu.pocketstats.stats.DayStatsType
import eu.cybershu.pocketstats.utils.TimeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Shared

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

import static eu.cybershu.pocketstats.PocketItemBuilder.archived
import static eu.cybershu.pocketstats.PocketItemBuilder.todo

@SpringBootTest
@AutoConfigureDataMongo
class PocketItemStatsServiceIntTest extends BaseTest {
    @Autowired
    private PocketItemRepository repository

    @Autowired
    private PocketItemStatsService statsService

    @Shared
    private Instant now = Instant.now()

    void cleanup() {
        repository.deleteAll()
    }

    def "given items in time period expect valid number of valid ARCHIVED(read) items"() {
        given:
            def items = []

            //-7 days: 3 archived
            Instant minus7days = now.minus(7, ChronoUnit.DAYS)
            items.addAll([
                    todo(minus7days),
                    todo(minus7days),
                    todo(minus7days),
                    archived(minus7days, minus7days),
                    archived(minus7days, minus7days),
                    archived(minus7days, minus7days)
            ])

            //-5 days: 2 archived
            Instant minus5days = now.minus(5, ChronoUnit.DAYS)
            items.addAll([
                    todo(minus5days),
                    todo(minus5days),
                    archived(minus5days, minus5days),
                    archived(minus5days, minus5days)
            ])

            //-2 days: 1 archived
            Instant minus2days = now.minus(2, ChronoUnit.DAYS)
            items.addAll([
                    todo(minus2days),
                    archived(minus2days, minus2days),
            ])

            //today: 1 archived
            items.addAll([
                    todo(TimeUtils.instantTodayEnd()),
                    archived(now, TimeUtils.instantTodayEnd()),
            ])

            assert repository.saveAll(items).size() == items.size()

            def start = LocalDate.now().minusDays(7)
            def end = LocalDate.now()
        when:
            def result = statsService.getDayStatsRecords(
                    start, end, DayStatsType.ARCHIVED
            )
        then:
            result.type() == DayStatsType.ARCHIVED
        and: "assert numbers"
            assertNumberInDay(result, start, 3)
            assertNumberInDay(result, end.minusDays(5), 2)
            assertNumberInDay(result, end.minusDays(2), 1)
            assertNumberInDay(result, end, 1)
    }

    def "given items in time period expect valid number of valid TODO items"() {
        given:
            def items = []

            //-7 days: 3 added
            Instant minus7days = now.minus(7, ChronoUnit.DAYS)
            items.addAll([
                    todo(minus7days),
                    todo(minus7days),
                    todo(minus7days),
                    archived(minus7days, minus7days),
                    archived(minus7days, minus7days),
                    archived(minus7days, minus7days)
            ])

            //-5 days: 4 added
            Instant minus5days = now.minus(5, ChronoUnit.DAYS)
            items.addAll([
                    todo(minus5days),
                    todo(minus5days),
                    todo(minus5days),
                    todo(minus5days),
                    archived(minus5days, minus5days),
                    archived(minus5days, minus5days)
            ])

            //-2 days: 1 added
            Instant minus2days = now.minus(2, ChronoUnit.DAYS)
            items.addAll([
                    todo(minus2days),
                    archived(minus2days, minus2days),
            ])

            //today: 3 added
            items.addAll([
                    todo(TimeUtils.instantTodayEnd()),
                    todo(now),
                    todo(TimeUtils.instantTodayEnd()),
                    archived(now, now),
            ])

            assert repository.saveAll(items).size() == items.size()

            def start = LocalDate.now().minusDays(7)
            def end = LocalDate.now()
        when:
            def result = statsService.getDayStatsRecords(
                    start, end, DayStatsType.TODO
            )
        then:
            result.type() == DayStatsType.TODO
        and: "assert numbers"
            assertNumberInDay(result, start, 3)
            assertNumberInDay(result, end.minusDays(5), 4)
            assertNumberInDay(result, end.minusDays(2), 1)
            assertNumberInDay(result, end, 3)
    }

    private void assertNumberInDay(DayStatsRecords dayStatsRecords, LocalDate date, Integer expected) {
        def found = dayStatsRecords.stats().findAll { it -> it.day() == date }
        assert found.size() == 1, "Found more than 1 record for day ${date}. It can be only one!"
        assert found.get(0).number() == expected, "Expected ${expected} got ${found.size()} items for day ${date}"
    }

}
