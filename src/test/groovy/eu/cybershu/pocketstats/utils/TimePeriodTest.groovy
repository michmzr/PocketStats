package eu.cybershu.pocketstats.utils

import spock.lang.Shared
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TimePeriodSpec extends Specification {
    @Shared
    private ZoneId timeZone = ZoneId.of("UTC")
    @Shared
    private Clock clock = Clock.fixed(Instant.parse("2023-04-19T10:00:00Z"), timeZone)

    def "Constructor should swap begin and end if end is before begin"() {
        given:
        LocalDate begin = LocalDate.now()
        LocalDate end = LocalDate.now().minusDays(1)

        when:
        TimePeriod timePeriod = new TimePeriod(begin, end)

        then:
        timePeriod.begin() == end
        timePeriod.end() == begin
    }

    def "currentWeek should return correct time period for the current week"() {
        when:
        TimePeriod currentWeek = TimePeriod.currentWeek(clock)

        then:
        currentWeek.begin() == LocalDate.parse("2023-04-17")
        currentWeek.end() == LocalDate.parse("2023-04-23")
    }

    def "previousWeek should return correct time period for the previous week"() {
        when:
        TimePeriod previousWeek = TimePeriod.previousWeek(clock)

        then:
        previousWeek.begin() == LocalDate.parse("2023-04-10")
        previousWeek.end() == LocalDate.parse("2023-04-16")
    }

    def "currentMonth should return correct time period for the current month"() {
        given:

        when:
        TimePeriod currentMonth = TimePeriod.currentMonth(clock)

        then:
        currentMonth.begin() == LocalDate.parse("2023-04-01")
        currentMonth.end() == LocalDate.parse("2023-04-30")
    }

    def "lastMonth should return correct time period for the last month"() {
        when:
        TimePeriod lastMonth = TimePeriod.lastMonth(clock)

        then:
        lastMonth.begin() == LocalDate.parse("2023-03-01")
        lastMonth.end() == LocalDate.parse("2023-03-31")
    }

    def "currentYear should return correct time period for the current year"() {
        when:
        TimePeriod currentYear = TimePeriod.currentYear(clock)

        then:
        currentYear.begin() == LocalDate.parse("2023-01-01")
        currentYear.end() == LocalDate.parse("2023-12-31")
    }

    def "lastYear should return correct time period for the last year"() {
        when:
        TimePeriod lastYear = TimePeriod.lastYear(clock)

        then:
        lastYear.begin() == LocalDate.parse("2022-01-01")
        lastYear.end() == LocalDate.parse("2022-12-31")
    }
}