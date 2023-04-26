package eu.cybershu.pocketstats.pocket.stats

import eu.cybershu.pocketstats.db.PocketItem
import eu.cybershu.pocketstats.db.PocketItemRepository
import eu.cybershu.pocketstats.pocket.api.BaseTest
import eu.cybershu.pocketstats.pocket.api.ItemsStatsAggregated
import eu.cybershu.pocketstats.pocket.api.PocketItemStatsService
import eu.cybershu.pocketstats.stats.DayStatsRecords
import eu.cybershu.pocketstats.stats.DayStatsType
import eu.cybershu.pocketstats.utils.TimePeriod
import eu.cybershu.pocketstats.utils.TimeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Shared

import java.time.*
import java.time.temporal.ChronoUnit

import static eu.cybershu.pocketstats.PocketItemBuilder.*
import static org.assertj.core.api.Assertions.assertThat

@SpringBootTest
@AutoConfigureDataMongo
class PocketItemStatsServiceIntTest extends BaseTest {
    @Autowired
    private PocketItemRepository repository

    @Autowired
    private PocketItemStatsService statsService

    @Shared
    private ZoneId timeZone = ZoneId.of("UTC")
    @Shared
    private Clock clock = Clock.fixed(Instant.parse("2023-04-26T10:00:00Z"), timeZone) //wednesday

    @Shared
    private Instant now = clock.instant()

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

    def "given items with different lang expect counted languages by name"() {
        given:
        def items = [
                withLang(todo(now, "item 1-pl"), "pl"),
                withLang(todo(now, "item 2-pl"), "pl"),
                withLang(todo(now, "item 3-en"), "en"),
                withLang(todo(now, "item 4-en"), "en"),
                withLang(todo(now, "item 5-en"), "en"),
                withLang(todo(now, "item 6-pt"), "pt")
        ]
        assert repository.saveAll(items).size() == items.size()
        when:
        def result = statsService.getLangStats()
        then:
        !result.isEmpty()
        and:
        result.pl == 2
        result.en == 3
        result.pt == 1
    }

    def "given records from current week expect count them in stats"() {
        given:
        def todoItems = [
                daysDiffAdded(-3), //last sunday (should not be included)!
                daysDiffAdded(-2), //monday
                daysDiffAdded(-1), //tuesday

                daysDiffAdded(0), //wednesday - today

                daysDiffAdded(1), //thursday
                daysDiffAdded(2), //friday
                daysDiffAdded(3), //saturday
                daysDiffAdded(4), //sunday
                daysDiffAdded(5), //next monday (should not be included)!
        ]

        def doneItems = [
                //added month ago, but read in this week
                daysDiffArchived(-2, instantDaysDiffers(-90)),
                daysDiffArchived(-1, instantDaysDiffers(-30)),
                daysDiffArchived(0, instantDaysDiffers(-7)),

                //added in this week and read in this week
                daysDiffArchived(-3, instantDaysDiffers(-3)), //last sunday (should not be included)!
                daysDiffArchived(-2, instantDaysDiffers(-2)), //monday
                daysDiffArchived(-1, instantDaysDiffers(-1)), //tuesday

                daysDiffArchived(0, now), //wednesday - today

                daysDiffArchived(1, instantDaysDiffers(1)), //thursday
                daysDiffArchived(2, instantDaysDiffers(2)), //friday
                daysDiffArchived(3, instantDaysDiffers(3)), //saturday
                daysDiffArchived(4, instantDaysDiffers(4)), //sunday
                daysDiffArchived(5, instantDaysDiffers(5)), //next monday (should not be included)!
        ]

        assert repository.saveAll(doneItems).size() == doneItems.size()
        assert repository.saveAll(todoItems).size() == todoItems.size()

        TimePeriod currWeek = TimePeriod.currentWeek(clock)
        when:
        def result = statsService.itemsStatsPeriod(currWeek)
        then:
        result.read() == 10
        result.added() == 7 + 7
    }

    private def "given all periods expect numbers - fast checking"() {
        given:
        def todoItems = [
                todo(instantWithDiff(-10, ChronoUnit.YEARS)),  //-10 years

                todo(instantWithDiff(-1, ChronoUnit.YEARS)),  //last year

                todo(instantWithDiff(-2, ChronoUnit.MONTHS)),
                todo(instantWithDiff(-1, ChronoUnit.MONTHS)), //last month

                todo(instantWithDiff(-1, ChronoUnit.WEEKS)),
                todo(instantWithDiff(-1, ChronoUnit.DAYS)),

                todo(instantWithDiff(1, ChronoUnit.DAYS))
        ]

        def doneItems = [
                archived(instantWithDiff(-20, ChronoUnit.YEARS), instantWithDiff(-20, ChronoUnit.YEARS)),
                archived(instantWithDiff(-11, ChronoUnit.YEARS), instantWithDiff(-11, ChronoUnit.YEARS)),
                archived(instantWithDiff(-1, ChronoUnit.YEARS), instantWithDiff(-1, ChronoUnit.YEARS)),
                archived(instantWithDiff(-2, ChronoUnit.MONTHS), instantWithDiff(-2, ChronoUnit.MONTHS)),
                archived(instantWithDiff(-1, ChronoUnit.MONTHS), instantWithDiff(-1, ChronoUnit.MONTHS)),
                archived(instantWithDiff(-1, ChronoUnit.WEEKS), instantWithDiff(-1, ChronoUnit.WEEKS)),
                archived(instantWithDiff(-1, ChronoUnit.DAYS), instantWithDiff(-1, ChronoUnit.DAYS)),
                archived(instantWithDiff(1, ChronoUnit.DAYS), instantWithDiff(1, ChronoUnit.DAYS))
        ]

        assert repository.saveAll(doneItems).size() == doneItems.size()
        assert repository.saveAll(todoItems).size() == todoItems.size()

        when:
        def result = statsService.itemsStatsAggregated()
        then: "last year"
        assetPeriodStats(result, "last-year", 1, 2)
        and: "last month"
        assetPeriodStats(result, "last-month", 1, 2)
        and: "last week"
        assetPeriodStats(result, "last-week", 1, 2)

        then: "current year"
        assetPeriodStats(result, "current-year", 5, 5 + 5)
        and: "current month"
        assetPeriodStats(result, "current-month", 3, 3 + 3)
        and: "current week"
        assetPeriodStats(result, "current-week", 2, 2 + 2)
        and: "total"
        assetPeriodStats(result, "total", 8, 7 + 8)
    }

    private assetPeriodStats(ItemsStatsAggregated resul, String name, int expectedRead, int expectedAdded) {
        assertThat(resul.findByName(name)).isPresent()

        def stats = resul.findByName(name).get().stats()
        assertThat(stats.read()).isEqualTo(expectedRead)
        assertThat(stats.added()).isEqualTo(expectedAdded)
    }

    private PocketItem daysDiffAdded(int days) {
        def timeAdded = instantDaysDiffers(days)
        String title = "read " + timeAdded
        String url = "http://local/random"

        todo(timeAdded, title, url)
    }

    private PocketItem daysDiffArchived(int days) {
        daysDiffArchived(days, instantDaysDiffers(-30))
    }

    private PocketItem daysDiffArchived(int days, Instant dayAdded) {
        def timeAdded = instantDaysDiffers(days)
        String title = "read " + timeAdded
        String url = "http://local/random"

        archived(dayAdded, timeAdded, title, url)
    }

    private Instant instantDaysDiffers(int differs) {
        LocalDateTime localDateTimeNow = LocalDateTime.now(clock)

        if (differs > 0) {
            toInstant(localDateTimeNow.plusDays(differs))
        } else {
            toInstant(localDateTimeNow.minusDays(differs.abs()))
        }
    }

    private Instant instantWithDiff(int number, ChronoUnit chronoUnit) {
        LocalDateTime localDateTimeNow = LocalDateTime.now(clock)

        toInstant(localDateTimeNow.plus(number, chronoUnit))
    }

    private Instant toInstant(LocalDateTime lc) {
        lc.toInstant(timeZone.getOffset())
    }

    private void assertNumberInDay(DayStatsRecords dayStatsRecords, LocalDate date, Integer expected) {
        def found = dayStatsRecords.stats().findAll { it -> it.day() == date }
        assert found.size() == 1, "Found more than 1 record for day ${date}. It can be only one!"
        assert found.get(0).number() == expected, "Expected ${expected} got ${found.size()} items for day ${date}"
    }
}
