package eu.cybershu.pocketstats.migration

import eu.cybershu.pocketstats.db.*
import eu.cybershu.pocketstats.events.EventsPublisher
import eu.cybershu.pocketstats.pocket.PocketApiService
import eu.cybershu.pocketstats.reader.api.ReaderApiService
import eu.cybershu.pocketstats.utils.TimeUtils
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

class ApiMigrationServiceTest extends Specification {
    private MigrationStatusRepository migrationStatusRepository
    private PocketApiService pocketApiService
    private ReaderApiService readerApiService
    private ItemRepository itemRepository
    private EventsPublisher eventsPublisher

    private ApiMigrationService migrationService

    void setup() {
        migrationStatusRepository = Mock()
        pocketApiService = Mock()
        readerApiService = Mock()
        itemRepository = Mock()
        eventsPublisher = Mock()

        migrationService = new ApiMigrationService(
                migrationStatusRepository, pocketApiService, readerApiService, itemRepository, eventsPublisher)
    }

    def "given POCKET and set time Expect pocket api will be running and db actions updated"() {
        given:
        Instant sinceWhen = TimeUtils.instantFrom("2024-09-28", "01:00:00")
        Source source = Source.POCKET
        def expectedItem = new Item()
        def savedStatus = new MigrationStatus(
                source: source,
                migratedItems: 1,
                date: Instant.now()
        )

        when:
        def result = migrationService.migrateSource(source, sinceWhen)

        then:
        1 * pocketApiService.importAllSinceWhen(sinceWhen) >> [expectedItem]
        1 * itemRepository.saveAll([expectedItem])
        1 * eventsPublisher.sendUserSynchronizedItems(_)
        1 * migrationStatusRepository.save(_ as MigrationStatus) >> savedStatus

        result.records == 1
        result.success
    }

    def "given READER and set time Expect reader api will be running and db actions updated"() {
        setup:
        Instant sinceWhen = TimeUtils.instantFrom("2024-09-28", "01:00:00")

        Source source = Source.READER
        def expectedItem = new Item()
        def savedStatus = new MigrationStatus(
                source: source,
                migratedItems: 1,
                date: Instant.now()
        )

        when:
        def result = migrationService.migrateSource(source, sinceWhen)

        then:
        readerApiService.importAllSinceWhen(_, _) >> {[expectedItem]}
        1 * itemRepository.saveAll([expectedItem])
        1 * eventsPublisher.sendUserSynchronizedItems(_)
        1 * migrationStatusRepository.save(_ as MigrationStatus) >> savedStatus

        result.records == 1
        result.success
    }

    def "given no POCKET items imported Expect empty migration status"() {
        given:
        Instant sinceWhen = TimeUtils.instantFrom("2024-09-28", "01:00:00")
        Source source = Source.POCKET
        def savedStatus = new MigrationStatus(
                source: source,
                migratedItems: 0,
                date: Instant.now()
        )

        when:
        def result = migrationService.migrateSource(source, sinceWhen)

        then:
        1 * pocketApiService.importAllSinceWhen(sinceWhen) >> {[]}
        0 * itemRepository.saveAll([])
        1 * eventsPublisher.sendUserSynchronizedItems({ it.records == 0 })
        1 * migrationStatusRepository.save(_ as MigrationStatus) >> savedStatus

        result.records == 0
        result.success
    }

    @Unroll
    def "lastMigration should return status for source #source"() {
        given:
        def expectedStatus = new MigrationStatus(
                date: Instant.now(),
                migratedItems: 10,
                source: source
        )

        when:
        def result = migrationService.lastMigration(source)

        then:
        1 * migrationStatusRepository.findBySourceOrderByDateDesc(source) >> Optional.of(expectedStatus)
        result.isPresent()
        with(result.get()) {
            it.date == expectedStatus.date
            it.migratedItems == expectedStatus.migratedItems
            it.source == expectedStatus.source
        }

        where:
        source << [Source.POCKET, Source.READER]
    }

    def "importAllFromSinceLastUpdate should use last POCKET migration date"() {
        given:
        def pkLastMigration = new MigrationStatus(
                date: TimeUtils.instantFrom("2024-09-28", "02:00:00"),
                migratedItems: 10,
                source: Source.POCKET
        )

        def readerLastMigration = new MigrationStatus(
                date: TimeUtils.instantFrom("2024-09-30", "01:00:00"),
                migratedItems: 2,
                source: Source.READER
        )

        def pocketItem = new Item(id: 123, title: "Pocket Item")
        def readerItem = new Item(id: 456, title: "Reader Item")

        def pkStatus = new MigrationStatus(
                source: Source.POCKET,
                migratedItems: 1,
                date: Instant.now()
        )

        when:
        def result = migrationService.importAllFromSinceLastUpdate()

        then:
        1 * migrationStatusRepository.findBySourceOrderByDateDesc(Source.POCKET) >> Optional.of(pkLastMigration)
        1 * migrationStatusRepository.findBySourceOrderByDateDesc(Source.READER) >> Optional.of(readerLastMigration)

        1 * pocketApiService.importAllSinceWhen(pkLastMigration.date()) >> [pocketItem]
        1 * readerApiService.importAllSinceWhen(_, readerLastMigration.date()) >> [readerItem]

        1 * itemRepository.saveAll({it.size() == 1})
        1 * migrationStatusRepository.save({ it.source == Source.POCKET && it.migratedItems==1 }) >> pkStatus


        1 * itemRepository.saveAll({it.size() == 1})
        1 * migrationStatusRepository.save({ it.source == Source.READER && it.migratedItems==1 })

        2 * eventsPublisher.sendUserSynchronizedItems(_)

        result.records == 2
        result.success
    }

    def "should handle API exceptions gracefully"() {
        given:
        Instant sinceWhen = TimeUtils.instantFrom("2024-09-28", "01:00:00")
        Source source = Source.POCKET

        when:
        migrationService.migrateSource(source, sinceWhen)

        then:
        1 * pocketApiService.importAllSinceWhen(sinceWhen) >> { throw new IOException("API Error") }
        0 * itemRepository.saveAll(_)
        0 * eventsPublisher.sendUserSynchronizedItems(_)
        0 * migrationStatusRepository.save(_)

        thrown(IOException)
    }
}
