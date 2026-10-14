package eu.cybershu.pocketstats.stats.library

import eu.cybershu.pocketstats.db.*
import spock.lang.Specification
import java.time.Instant
import java.time.temporal.ChronoUnit

class LibraryAnalyticsCalculatorTest extends Specification {
    final Instant now = Instant.parse('2026-10-14T12:00:00Z')
    def calculator = new LibraryAnalyticsCalculator()
    Item item(Map values = [:]) {
        new Item([status: ItemStatus.TO_READ, source: Source.READER, category: 'article',
                  url: 'https://example.org/a', timeAdded: now.minus(2, ChronoUnit.DAYS)] + values)
    }
    def report(List<Item> items, Source source = null, int days = 30) {
        calculator.calculate(items.stream(), now, days, source)
    }

    def 'empty library has complete zero buckets and no undefined median or percentage'() {
        when:
        def r = report([])
        then:
        r.asOf() == now
        r.source() == null
        r.backlog().total() == 0
        r.backlog().ageBuckets()*.count() == [0L, 0L, 0L, 0L]
        r.archiveDelay().medianDays() == null
        r.comparison().added().percent() == null
        r.categories().empty
        r.domains().empty
    }

    def 'backlog ages use full day boundaries and retain unknown ages'() {
        given:
        def items = [0, 30, 31, 90, 91, 180, 181].collect { item(timeAdded: now.minus(it, ChronoUnit.DAYS)) }
        items += [item(timeAdded: null), item(timeAdded: now.plusSeconds(1))]
        when:
        def r = report(items)
        then:
        r.backlog().total() == 9
        r.backlog().ageBuckets()*.count() == [2L, 2L, 2L, 1L]
        r.backlog().unknownAge() == 2
    }

    def 'all metrics exclude deleted notes highlights and unselected sources'() {
        when:
        def r = report([item(), item(status: ItemStatus.ARCHIVED, timeRead: now.minusSeconds(1)),
                        item(status: ItemStatus.DELETED), item(category: 'HIGHLIGHT'),
                        item(category: 'Note'), item(source: Source.POCKET)], Source.READER)
        then:
        r.backlog().total() == 1
        r.archiveDelay().eligibleRecords() == 1
        r.categories()*.count() == [2L]
        r.domains()*.count() == [2L]
        r.comparison().current().added() == 2
        report([item(source: Source.POCKET)], Source.POCKET).backlog().total() == 1
    }

    def 'archive median supports fractional days and excludes missing negative and future dates'() {
        given:
        def archived = { Map values -> item([status: ItemStatus.ARCHIVED] + values) }
        def valid = [1.0d, 2.0d, 4.0d, 8.0d].collect { d -> archived(timeAdded: now.minusSeconds((long)(d * 86400)), timeRead: now) }
        def invalid = [archived(timeRead: null), archived(timeAdded: null, timeRead: now),
                       archived(timeAdded: now, timeRead: now.minusSeconds(1)), archived(timeRead: now.plusSeconds(1))]
        when:
        def r = report(valid + invalid)
        then:
        r.archiveDelay().medianDays() == 3.0d
        r.archiveDelay().eligibleRecords() == 4
        r.archiveDelay().excludedRecords() == 4
        report(valid.take(3)).archiveDelay().medianDays() == 2.0d
        report([archived(timeAdded: now.minusSeconds(43200), timeRead: now)]).archiveDelay().medianDays() == 0.5d
    }

    def 'comparison windows are half open and zero baselines do not divide by zero'() {
        given:
        def currentStart = now.minus(30, ChronoUnit.DAYS)
        def previousStart = now.minus(60, ChronoUnit.DAYS)
        when:
        def r = report([item(timeAdded: previousStart), item(timeAdded: currentStart.minusSeconds(1)),
                        item(timeAdded: currentStart), item(timeAdded: now.minusSeconds(1)), item(timeAdded: now),
                        item(status: ItemStatus.ARCHIVED, timeAdded: null, timeRead: currentStart)])
        then:
        r.comparison().current().start() == currentStart
        r.comparison().previous().start() == previousStart
        r.comparison().current().added() == 2
        r.comparison().previous().added() == 2
        r.comparison().added().delta() == 0
        r.comparison().added().percent() == 0.0d
        r.comparison().current().read() == 1
        r.comparison().read().percent() == null
        report([item(), item()], null, 7).comparison().current().start() == now.minus(7, ChronoUnit.DAYS)
        report([item(), item(), item(timeAdded: previousStart)]).comparison().added().percent() == 100.0d
    }

    def 'domain ranking normalizes web and sender hosts while counting invalid addresses separately'() {
        given:
        def urls = ['https://WWW.Example.ORG:8443/a', 'http://example.org/b',
                    'mailto:person@EXAMPLE.ORG,second@other.org?subject=test', 'mailto:person%40news.example.org',
                    'ftp://example.org/file', '/relative', 'https://', null, 'mailto:invalid']
        when:
        def r = report(urls.collect { item(url: it, category: null) })
        then:
        r.domains()*.name() == ['example.org', 'news.example.org']
        r.domains()*.count() == [3L, 1L]
        r.unknownDomainCount() == 5
        r.categories()*.name() == ['unknown']
        r.categories()[0].backlog() == 9
    }

    def 'ranking returns ten domains with alphabetical ties and all categories'() {
        when:
        def r = report((0..11).collect { item(url: "https://site${String.format('%02d', it)}.org", category: "category${it}") })
        then:
        r.domains().size() == 10
        r.domains().first().name() == 'site00.org'
        r.domains().last().name() == 'site09.org'
        r.categories().size() == 12
    }

    def 'invalid mailto recipient suffixes cannot become valid web hosts'() {
        when:
        def r = report(['mailto:person@example.org/path', 'mailto:person@example.org:443',
                        'mailto:person@example.org#fragment', 'mailto:person@www.'].collect { item(url: it) })
        then:
        r.domains().empty
        r.unknownDomainCount() == 4
    }
}
