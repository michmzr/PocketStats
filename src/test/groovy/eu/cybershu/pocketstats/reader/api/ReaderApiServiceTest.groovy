package eu.cybershu.pocketstats.reader.api

import eu.cybershu.pocketstats.pocket.api.BaseTest
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.context.annotation.EnableAspectJAutoProxy
import spock.lang.Specification

import java.time.*

@EnableAspectJAutoProxy
class ReaderApiServiceTest extends BaseTest {
    private String accessToken

    private ReaderApiService readerApiService

    void setup() {
        accessToken = System.getenv("READER_ACCESS_TOKEN")

        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(19)
                .limitRefreshPeriod(Duration.ofSeconds(61))
                .timeoutDuration(Duration.ofSeconds(61))
                .build();
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        rateLimiterRegistry.addConfiguration("readwise-api", config);

        readerApiService = new ReaderApiService(rateLimiterRegistry)
    }

    def "test connection"() {
        given:
        Instant readFrom = instantFrom("2024-10-28", "01:00:00")

        when:
        def response = readerApiService.fetch(accessToken,
               ReadwiseFetchParams
                       .builder()
                       .updatedAfter(readFrom)
                       .build()
        )

        then:
        response.size() > 0
    }

    Instant instantFrom(String strDate, String strTime) {
        var ldt = LocalDateTime.of(LocalDate.parse(strDate), LocalTime.parse(strTime))
        return ldt.toInstant(ZoneId.systemDefault().rules.getOffset(ldt))
    }
}
