package eu.cybershu.pocketstats.pocket.api

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import spock.lang.Shared

class PocketGetResponseTest extends BaseTest {
    @Shared
    ObjectMapper objectMapper = new ObjectMapper()

    def "test empty items in response"() {
        given:
            def payloadObject = [
                    "status": 2, "complete": 1, "list": [], "error": null, "search_meta": ["search_type": "normal"], "since": 1673449243
            ]
            def json = JsonOutput.toJson(payloadObject)
        when:
            PocketGetResponse response = objectMapper.readValue(json, PocketGetResponse)
        then:
            noExceptionThrown()

            verifyAll {
                response.status() == 2
                response.complete()
                response.items().isEmpty()
                !response.error()
                response.searchMeta().searchType() == "normal"
                response.since()
            }
    }

    def "given response with item expect valid pojo"() {
        given:
            def payloadObject = [
                    "status": 1, "complete": 1,
                    "list"  : [
                            "37783": [
                                    "item_id"       : "37783",
                                    "resolved_id"   : "37783",
                                    "given_url"     : "http://www.bresink.com/osx/TinkerTool.html",
                                    "given_title"   : "Link",
                                    "favorite"      : "1",
                                    "status"        : "0",
                                    "time_added"    : "1672449191",
                                    "time_updated"  : "1672570016",
                                    "time_read"     : "1672570016",
                                    "time_favorited": "1672571016",
                                    "sort_id"       : 4,
                                    "resolved_title": "TinkerTool",
                                    "resolved_url"  : "http://www.bresink.com/osx/TinkerTool.html",
                                    "excerpt"       : "TinkerTool is an application that gives you access to additional preference settings Apple has built into macOS. This allows to activate hidden features in the operating system and in some of the applications delivered with the system.",
                                    "is_article"    : "1",
                                    "is_index"      : "0",
                                    "has_video"     : "0",
                                    "has_image"     : "1",
                                    "word_count"    : "194",
                                    "lang"          : "en",
                                    "tags"          : [
                                            "macos": [
                                                    "item_id": "37783",
                                                    "tag"    : "macos"
                                            ]
                                    ]
                            ]

                    ],
                    "error" : null, "search_meta": ["search_type": "normal"], "since": 1673449242
            ]

            def json = JsonOutput.toJson(payloadObject)
        when:
            PocketGetResponse response = objectMapper.readValue(json, PocketGetResponse)
        then:
            noExceptionThrown()

            verifyAll {
                response.status() == 1
                response.complete()
                response.items().size() == 1
                response.items().containsKey("37783")
                !response.error()
                response.searchMeta().searchType() == "normal"
                response.since()
            }
        and: "verify object"
            ListItem item = response.items()["37783"]

            verifyAll {
                item.id() == "37783"

                item.timeAdded()
                item.timeRead()

                item.timeFavorited()
                item.favorite()

                item.tags().containsKey("macos")
                item.tags()["macos"].itemId() == "37783"
                item.tags()["macos"].tag() == "macos"
            }
    }
}
