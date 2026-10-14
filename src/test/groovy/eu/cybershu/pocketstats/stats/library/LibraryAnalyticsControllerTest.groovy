package eu.cybershu.pocketstats.stats.library

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import eu.cybershu.pocketstats.db.Source
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification
import spock.lang.Unroll
import java.time.Instant
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

class LibraryAnalyticsControllerTest extends Specification {
    def service = Mock(LibraryAnalyticsService)
    def mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    def mvc() {
        MockMvcBuilders.standaloneSetup(new LibraryAnalyticsController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper)).build()
    }

    def 'endpoint works without authentication or configuration and preserves JSON contract defaults'() {
        given:
        def now = Instant.parse('2026-10-14T12:00:00Z')
        when:
        def response = mvc().perform(get('/stats/library')).andReturn().response
        def body = mapper.readTree(response.contentAsString)
        then:
        1 * service.getAnalytics(30, null) >> new LibraryAnalyticsCalculator().calculate([].stream(), now, 30, null)
        response.status == 200
        body.status.intValue() == 0
        body.message.textValue() == 'ok'
        body.data.asOf.textValue() == now.toString()
        body.data.source.isNull()
        body.data.backlog.ageBuckets.size() == 4
        body.data.archiveDelay.medianDays.isNull()
        body.data.comparison.added.percent.isNull()
        body.data.categories.isArray()
        body.data.domains.isArray()
        body.data.unknownDomainCount.intValue() == 0
    }

    @Unroll
    def 'supported query days=#days source=#source selects the requested calculation'() {
        when:
        def response = mvc().perform(get('/stats/library').param('days', days).param('source', source)).andReturn().response
        then:
        1 * service.getAnalytics(Integer.parseInt(days), Source.valueOf(source)) >>
                new LibraryAnalyticsCalculator().calculate([].stream(), Instant.EPOCH, Integer.parseInt(days), Source.valueOf(source))
        response.status == 200
        where:
        days | source
        '7' | 'READER'
        '90' | 'POCKET'
    }

    @Unroll
    def 'invalid query #query returns 400 before reading database'() {
        when:
        def response = mvc().perform(get('/stats/library' + query)).andReturn().response
        then:
        response.status == 400
        0 * service._
        where:
        query << ['?days=0', '?days=31', '?days=-1', '?days=abc', '?source=UNKNOWN', '?source=ALL']
    }
}
