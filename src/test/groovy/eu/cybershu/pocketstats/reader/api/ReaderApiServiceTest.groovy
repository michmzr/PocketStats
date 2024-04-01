package eu.cybershu.pocketstats.reader.api

import spock.lang.Specification

import java.time.*

class ReaderApiServiceTest extends Specification {
    private String accessToken

    private ReaderApiService readerApiService = new ReaderApiService()

    void setup() {
        accessToken = System.getenv("READER_ACCESS_TOKEN")
    }

    def "test connection"() {
        given:
        Instant readFrom = instantFrom("2023-03-28", "01:00:00")

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

    Instant instantFromLocalDateTime(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant()
    }

    Instant instantFrom(String strDate, String strTime) {
        var ldt = LocalDateTime.of(LocalDate.parse(strDate), LocalTime.parse(strTime))
        return ldt.toInstant(ZoneId.systemDefault().rules.getOffset(ldt))
    }
}
