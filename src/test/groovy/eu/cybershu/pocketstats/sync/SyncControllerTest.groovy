package eu.cybershu.pocketstats.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import eu.cybershu.pocketstats.db.*
import eu.cybershu.pocketstats.events.EventsPublisher
import eu.cybershu.pocketstats.migration.ApiMigrationService
import eu.cybershu.pocketstats.reader.ReaderSyncConfiguration
import eu.cybershu.pocketstats.reader.api.ReaderApiService
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification
import spock.lang.Unroll
import java.time.Instant
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*

class SyncControllerTest extends Specification {
    def statuses = Mock(MigrationStatusRepository)
    def reader = Mock(ReaderApiService)
    def items = Mock(ItemRepository)
    def events = Mock(EventsPublisher)

    def mvc(String token) {
        def config = new ReaderSyncConfiguration(token)
        def service = new ApiMigrationService(statuses, reader, items, events, config)
        MockMvcBuilders.standaloneSetup(new SyncController(service, config))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        new ObjectMapper().registerModule(new JavaTimeModule())))
                .build()
    }

    @Unroll
    def "availability exposes only a boolean for configured=#available"() {
        when:
        def response = mvc(token).perform(get('/sync/availability')).andReturn().response
        def body = new ObjectMapper().readTree(response.contentAsString)

        then:
        response.status == 200
        body.path('data').path('available').booleanValue() == available
        body.path('data').size() == 1
        !response.contentAsString.contains('test-secret')
        0 * reader._
        0 * items._
        0 * statuses._

        where:
        token         | available
        ''            | false
        'test-secret' | true
    }

    def "Pocket sync returns 410 without touching historical data"() {
        when:
        def response = mvc('test-token').perform(post('/sync/last/POCKET')).andReturn().response

        then:
        response.status == 410
        0 * reader._
        0 * items._
        0 * statuses._
    }

    @Unroll
    def "unconfigured sync at #path returns 503 before importing or writing"() {
        when:
        def response = mvc('').perform(post(path)).andReturn().response

        then:
        response.status == 503
        0 * reader._
        0 * items._
        0 * statuses._

        where:
        path << ['/sync/last', '/sync/last/READER']
    }

    @Unroll
    def "Readwise sync at #path preserves response format"() {
        when:
        def response = mvc('test-token').perform(post(path)).andReturn().response
        def body = new ObjectMapper().readTree(response.contentAsString)

        then:
        1 * statuses.findFirstBySourceOrderByDateDesc(Source.READER) >> Optional.empty()
        1 * reader.importAll('test-token') >> []
        1 * statuses.save({ it.source() == Source.READER })
        1 * events.sendUserSynchronizedItems(_)
        0 * items._
        response.status == 200
        body.path('status').intValue() == 0
        body.path('data').path('success').booleanValue()
        body.path('data').path('records').intValue() == 0
        !body.path('data').path('date').isNull()

        where:
        path << ['/sync/last', '/sync/last/READER']
    }

    def "historical Pocket sync status remains readable without Readwise"() {
        when:
        def response = mvc('').perform(get('/sync/last/POCKET')).andReturn().response
        def body = new ObjectMapper().readTree(response.contentAsString)

        then:
        1 * statuses.findFirstBySourceOrderByDateDesc(Source.POCKET) >> Optional.of(
                new MigrationStatus(source: Source.POCKET, date: Instant.EPOCH, migratedItems: 12))
        response.status == 200
        body.path('data').path('records').intValue() == 12
        0 * items._
        0 * reader._
        0 * statuses.save(_)
    }

    @Unroll
    def "removed Pocket auth endpoint #path returns 404"() {
        expect:
        mvc('').perform(get(path)).andReturn().response.status == 404

        where:
        path << ['/pocket/auth/login', '/pocket/auth/token', '/pocket/auth/authorized']
    }
}
