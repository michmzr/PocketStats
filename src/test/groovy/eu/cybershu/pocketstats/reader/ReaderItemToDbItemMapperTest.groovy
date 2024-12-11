package eu.cybershu.pocketstats.reader

import com.fasterxml.jackson.databind.ObjectMapper
import eu.cybershu.pocketstats.db.Item
import eu.cybershu.pocketstats.db.ItemStatus
import eu.cybershu.pocketstats.db.Source
import eu.cybershu.pocketstats.reader.api.Category
import eu.cybershu.pocketstats.reader.api.Location
import eu.cybershu.pocketstats.reader.api.ReaderItem
import eu.cybershu.pocketstats.reader.api.Tag
import spock.lang.Shared
import spock.lang.Specification

import java.time.Instant

class ReaderItemToDbItemMapperTest extends Specification {
    @Shared
    ObjectMapper objectMapper = new ObjectMapper()

    @Shared
    ReaderItemToDbItemMapper mapper = new ReaderItemToDbItemMapperImpl()

    void "test map ReaderItem to Item with new location"() {
        given:
        ReaderItem readerItem = new ReaderItem(
                "01jdhdfxjjan6qv28h5xwc4kjq",
                "https://read.readwise.io/read/01jdhdfxjjan6qv28h5xwc4kjq",
                "Apteczka Biohakera. Co i Dlaczego mam w Domowej Apteczce?",
                "Piona✋!",
                "Readwise web highlighter",
                Category.ARTICLE,
                Location.NEW,
                ["health & wellness": new Tag("Health & Wellness", "generated", 1732530146556)],
                "Biohacking - www.biohaker.pl",
                284,
                Instant.parse("2024-11-25T10:22:21.525500Z"),
                Instant.parse("2024-11-25T10:22:28.439490Z"),
                Instant.parse("2024-11-25T10:22:58.20439490Z"),
                "1688169600000",
                "The author shares their personal home medicine cabinet and its contents, which have been tested over the years. They emphasize the importance of being prepared with effective over-the-counter remedies for potential infections. The article also includes a reminder that the information is not medical advice and encourages readers to consult a trusted healthcare professional.",
                "Any summary",
                null,
                0.9
        )

        when:
        Item item = mapper.apiToEntity(readerItem)

        then:
        item.id == "01jdhdfxjjan6qv28h5xwc4kjq"
        item.url == "https://read.readwise.io/read/01jdhdfxjjan6qv28h5xwc4kjq"
        item.title == "Apteczka Biohakera. Co i Dlaczego mam w Domowej Apteczce?"
        item.status == ItemStatus.TO_READ
        item.timeAdded == Instant.parse("2024-11-25T10:22:21.525500Z")
        item.timeUpdated == Instant.parse("2024-11-25T10:22:28.439490Z")
        item.timeRead == null
        !item.excerpt.isBlank()
        item.wordCount == 284
        item.category == "article"
        item.source == Source.READER
    }

    void "test map ReaderItem to Item with archive location"() {
        given:
        ReaderItem readerItem = new ReaderItem(
                "01jee5h3kjxddevt2b1tspzdf0",
                "https://read.readwise.io/read/01jee5h3kjxddevt2b1tspzdf0",
                "abi/screenshot-to-code: Drop in a screenshot and convert it to clean code (HTML/Tailwind/React/Vue)",
                "https://github.com/abi/",
                "Readwise web highlighter",
                Category.ARTICLE,
                Location.ARCHIVE,
                ["technology": new Tag("Technology", "generated", 1733494874768)],
                "GitHub",
                591,
                Instant.parse("2024-12-06T14:21:10.728337Z"),
                Instant.parse("2024-12-06T14:32:50.375265Z"),
                Instant.parse("2024-12-06T14:32:50.092000Z"),
                "",
                "The \"screenshot-to-code\" tool converts images, mockups, and designs into clean code using AI models like Claude Sonnet 3.5 and GPT-4o. It supports multiple frameworks, including HTML, React, and Vue, and can even create prototypes from video recordings. Users need an OpenAI API key for the app to function, and detailed setup instructions are provided.",
                "any summary",
                "",
                0.2
        )

        when:
        Item item = mapper.apiToEntity(readerItem)

        then:
        item.id == "01jee5h3kjxddevt2b1tspzdf0"
        item.url == "https://read.readwise.io/read/01jee5h3kjxddevt2b1tspzdf0"
        item.title == "abi/screenshot-to-code: Drop in a screenshot and convert it to clean code (HTML/Tailwind/React/Vue)"
        item.status == ItemStatus.ARCHIVED
        item.timeAdded == Instant.parse("2024-12-06T14:21:10.728337Z")
        item.timeUpdated == Instant.parse("2024-12-06T14:32:50.375265Z")
        item.timeRead == Instant.parse("2024-12-06T14:32:50.092000Z")
        !item.excerpt.isEmpty()
        item.wordCount == 591
        item.category == "article"
        item.source == Source.READER
    }
}