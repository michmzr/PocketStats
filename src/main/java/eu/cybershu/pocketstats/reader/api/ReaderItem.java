package eu.cybershu.pocketstats.reader.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 {
 *             "id": "01gkqt8nbms4t698abcdvcswvf",
 *             "url": "https://readwise.io/new/read/01gkqt8nbms4t698abcdvcswvf",
 *             "source_url": "https://www.vanityfair.com/news/2022/10/covid-origins-investigation-wuhan-lab",
 *             "title": "COVID-19 Origins: Investigating a “Complex and Grave Situation” Inside a Wuhan Lab",
 *             "author": "Condé Nast",
 *             "source": "Reader add from import URL",
 *             "category": "article",
 *             "location": "new",
 *             "tags": {},
 *             "site_name": "Vanity Fair",
 *             "word_count": 9601,
 *             "created_at": "2022-12-08T02:50:35.662027+00:00",
 *             "updated_at": "2023-03-22T13:29:41.827456+00:00",
 *             "published_date": "2022-10-28",
 *             "notes": "",
 *             "summary": "The Wuhan Institute of Virology, the cutting-edge ...",
 *             "image_url": "https://media.vanityfair.com/photos/63599642578d980751943b65/16:9/w_1280,c_limit/vf-1022-covid-trackers-site-story.jpg",
 *             "parent_id": null,
 *             "reading_progress": 0,
 *         }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReaderItem(
        @JsonProperty String id,
        @JsonProperty String url,
        @JsonProperty("source_url") String sourceUrl,
        @JsonProperty String title,
        @JsonProperty String author,
        @JsonProperty String source,
        @JsonProperty Category category,
        @JsonProperty Location location,
        //@JsonProperty List<String> tags,
        @JsonProperty("site_name") String siteName,
        @JsonProperty("word_count") int wordCount,
        @JsonProperty
        @JsonFormat(shape = JsonFormat.Shape.NUMBER, without = JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS) Instant created_at,
        @JsonProperty
        @JsonFormat(shape = JsonFormat.Shape.NUMBER, without = JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS) Instant updated_at,
        @JsonProperty
        @JsonFormat(shape = JsonFormat.Shape.NUMBER, without = JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS) Instant published_date,
        @JsonProperty String notes,
        @JsonProperty String summary,
        @JsonProperty("image_url") String imageUrl,
        @JsonProperty double reading_progress
) {}
