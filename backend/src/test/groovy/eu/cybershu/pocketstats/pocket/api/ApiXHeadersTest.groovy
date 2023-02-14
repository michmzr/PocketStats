package eu.cybershu.pocketstats.pocket.api


import spock.lang.Specification

import java.net.http.HttpHeaders
import java.net.http.HttpResponse

class ApiXHeadersTest extends Specification {
    def "given response header expect binded values to POJO"() {
        given:
            def response = Stub(HttpResponse)
            def props = [
                    "Status"                : ["400 Bad Request"],
                    "X-Error"               : ["Missing API Key"],
                    "X-Error-Code"          : ["0"],

                    "X-Limit-User-Limit"    : ["100"],
                    "X-Limit-User-Remaining": ["10"],
                    "X-Limit-User-Reset"    : ["25"],

                    "X-Limit-Key-Remaining" : ["10"],
                    "X-Limit-Key-Limit"     : ["10000"],
                    "X-Limit-Key-Reset"     : ["666"]
            ]

            response.headers() >> HttpHeaders.of(props,
                    (s, s2) -> true)
        when:
            def xHeaders = ApiXHeaders.of(response)
        then:
            verifyAll {
                xHeaders.errorCode == 0
                xHeaders.limitUserLimit == 100
                xHeaders.limitUserRemaining == 10
                xHeaders.limitUserReset == 25


                xHeaders.limitKeyLimit == 10000
                xHeaders.limitKeyRemaining == 10
                xHeaders.limitKeyReset == 666

            }
    }

    def "given null or not existing headers expect nulls in fields"() {
        def response = Stub(HttpResponse)
        def props = [
                "X-Error-Code"      : [],
                "X-Limit-User-Reset": [""],
                "X-Limit-Key-Reset" : []
        ]

        response.headers() >> HttpHeaders.of(props,
                (s, s2) -> true)
        when:
            def xHeaders = ApiXHeaders.of(response)
        then:
            noExceptionThrown()

            verifyAll {
                xHeaders.errorCode == null
                xHeaders.limitUserLimit == null
                xHeaders.limitUserRemaining == null
                xHeaders.limitUserReset == null

                xHeaders.limitKeyLimit == null
                xHeaders.limitKeyRemaining == null
                xHeaders.limitKeyReset == null
            }
    }
}
