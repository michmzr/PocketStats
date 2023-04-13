package eu.cybershu.pocketstats.validation

import spock.lang.Specification
import spock.lang.Unroll

class CheckDateValidatorTest extends Specification {
    private static final String DD_MM_YYYY = "dd-MM-yyyy"

    @Unroll
    def "test isValid - date: #date, pattern #pattern || expected #isValid"(String date, String pattern, Boolean isValid) {
        given:
            CheckDateValidator validator = new CheckDateValidator()
            validator.setPattern(pattern)
        expect:
            validator.isValid(date, null) == isValid
        where:
            date         | pattern   || isValid
            "12-01-1990" | DD_MM_YYYY | true
            "12-12-1990" | DD_MM_YYYY | true
            "12_12_1990" | DD_MM_YYYY | false
            "32-12-1990" | DD_MM_YYYY | false
            "-1-12-1990" | DD_MM_YYYY | false
            "31-14-1990" | DD_MM_YYYY | false
            "31--1-1990" | DD_MM_YYYY | false
            "31-12-00"   | DD_MM_YYYY | false
            "31-12-99"   | DD_MM_YYYY | false
            "31-12-999"  | DD_MM_YYYY | false
            "31-12-9"    | DD_MM_YYYY | false

    }
}
