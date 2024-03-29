package eu.cybershu.pocketstats.reader.api

import spock.lang.Specification

import java.time.Instant

class ReaderApiServiceTest extends Specification {
    private String accessToken

    private ReaderApiService readerApiService = new ReaderApiService()

    void setup() {
        accessToken = System.getenv("READER_ACCESS_TOKEN")
    }

    def "test connection"() {
        given:
        Instant readFrom = Instant.now().minusSeconds(60*60*24*10)

        when:
        def response = readerApiService.fetchList(accessToken,
               ReadwiseFetchParams
                       .builder()
                       .updatedAfter(readFrom)
                       .build()
        )

        then:
        response.size() > 0
    }
}
