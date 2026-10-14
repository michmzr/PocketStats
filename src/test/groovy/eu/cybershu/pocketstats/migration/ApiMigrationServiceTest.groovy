package eu.cybershu.pocketstats.migration

import eu.cybershu.pocketstats.db.*
import eu.cybershu.pocketstats.events.EventsPublisher
import eu.cybershu.pocketstats.reader.ReaderSyncConfiguration
import eu.cybershu.pocketstats.reader.api.ReaderApiService
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Unroll
import java.time.Instant

class ApiMigrationServiceTest extends Specification {
    def statuses = Mock(MigrationStatusRepository)
    def reader = Mock(ReaderApiService)
    def items = Mock(ItemRepository)
    def events = Mock(EventsPublisher)
    def service = new ApiMigrationService(statuses, reader, items, events,
            new ReaderSyncConfiguration("test-token"))

    @Unroll
    def "Pocket import #operation is rejected without any external or persistence operations"() {
        when:
        if (operation == 'last') service.importAllFromSinceLastUpdate(Source.POCKET)
        else service.migrateSource(Source.POCKET, Instant.EPOCH)

        then:
        def error = thrown(ResponseStatusException)
        error.statusCode.value() == 410
        0 * reader._
        0 * items._
        0 * statuses._
        0 * events._

        where:
        operation << ['last', 'since']
    }

    @Unroll
    def "unconfigured Readwise #token is rejected before reading or writing data"() {
        given:
        def unconfigured = new ApiMigrationService(statuses, reader, items, events,
                new ReaderSyncConfiguration(token))

        when:
        unconfigured.importAllFromSinceLastUpdate()

        then:
        def error = thrown(ResponseStatusException)
        error.statusCode.value() == 503
        0 * reader._
        0 * items._
        0 * statuses._
        0 * events._

        where:
        token << [null, '', '  \t']
    }

    def "default synchronization imports only Readwise since its last sync"() {
        given:
        def since = Instant.parse('2024-09-30T01:00:00Z')
        def last = new MigrationStatus(source: Source.READER, date: since)
        def article = new Item(id: 'reader-456', source: Source.READER, category: 'article')

        when:
        def result = service.importAllFromSinceLastUpdate()

        then:
        1 * statuses.findFirstBySourceOrderByDateDesc(Source.READER) >> Optional.of(last)
        1 * reader.importAllSinceWhen('test-token', since) >> [article]
        1 * items.saveAll([article])
        1 * statuses.save({ it.source() == Source.READER && it.migratedItems() == 1 })
        1 * events.sendUserSynchronizedItems({ it.records() == 1 })
        0 * statuses._
        0 * items._
        0 * reader._
        result.records() == 1
        result.success()
    }

    def "first Readwise sync imports all items and filters unsupported categories"() {
        given:
        def article = new Item(id: 'reader-1', source: Source.READER, category: 'article')
        def excluded = new Item(id: 'reader-2', source: Source.READER, category: 'unsupported')

        when:
        def result = service.importAllFromSinceLastUpdate(Source.READER)

        then:
        1 * statuses.findFirstBySourceOrderByDateDesc(Source.READER) >> Optional.empty()
        1 * reader.importAll('test-token') >> [article, excluded]
        1 * items.saveAll([article])
        1 * statuses.save({ it.source() == Source.READER && it.migratedItems() == 1 })
        1 * events.sendUserSynchronizedItems(_)
        0 * statuses._
        0 * items._
        result.records() == 1
        result.success()
    }

    def "empty Readwise import records a sync without modifying items"() {
        when:
        def result = service.migrateSource(Source.READER, Instant.EPOCH)

        then:
        1 * reader.importAllSinceWhen('test-token', Instant.EPOCH) >> []
        0 * items._
        1 * statuses.save({ it.source() == Source.READER && it.migratedItems() == 0 })
        1 * events.sendUserSynchronizedItems({ it.records() == 0 })
        result.records() == 0
        result.success()
    }

    def "failed Readwise import leaves existing items and migration history unchanged"() {
        when:
        service.importAllFromSinceLastUpdate()

        then:
        1 * statuses.findFirstBySourceOrderByDateDesc(Source.READER) >> Optional.empty()
        1 * reader.importAll('test-token') >> { throw new IOException('API error') }
        thrown(IOException)
        0 * items._
        0 * statuses.save(_)
        0 * events._
    }

    @Unroll
    def "historical status for #source remains readable"() {
        given:
        def historical = new MigrationStatus(source: source, date: Instant.EPOCH, migratedItems: 12)

        when:
        def result = service.lastMigration(source)

        then:
        1 * statuses.findFirstBySourceOrderByDateDesc(source) >> Optional.of(historical)
        result.get().is(historical)
        0 * items._
        0 * reader._
        0 * statuses.save(_)

        where:
        source << [Source.POCKET, Source.READER]
    }
}
