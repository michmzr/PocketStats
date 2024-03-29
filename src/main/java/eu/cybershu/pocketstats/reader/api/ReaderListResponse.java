package eu.cybershu.pocketstats.reader.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * {
 *     "count": 2304,
 *     "nextPageCursor": "01gm6kjzabcd609yepjrmcgz8a",
 *     "results": [
 *         {
 *             "id": "01gwfvp9pyaabcdgmx14f6ha0",
 *             "url": "https://readiwise.io/feed/read/01gwfvp9pyaabcdgmx14f6ha0",
 *             "source_url": "https://www.driverlesscrocodile.com/values/ends-and-meanings-3-alasdair-macintyre-virtue-mortality-and-story-in-heroic-societies/",
 *             "title": "Ends and Meanings (3): Alasdair MacIntyre virtue, mortality and story in heroic societies",
 *             "author": "Stuart Patience",
 *             "source": "Reader RSS",
 *             "category": "rss",
 *             "location": "feed",
 *             "tags": {},
 *             "site_name": "Driverless Crocodile",
 *             "word_count": 819,
 *             "created_at": "2023-03-26T21:02:51.618751+00:00",
 *             "updated_at": "2023-03-26T21:02:55.453827+00:00",
 *             "notes": "",
 *             "published_date": "2023-03-22",
 *             "summary": "Without … a place in the social order, ...",
 *             "image_url": "https://i0.wp.com/www.driverlesscrocodile.com/wp-content/uploads/2019/10/cropped-driverlesscrocodile-icon-e1571123201159-4.jpg?fit=32%2C32&ssl=1",
 *             "parent_id": null,
 *             "reading_progress": 0.15,
 *         },
 *         {
 *             "id": "01gkqtdz9xabcd5gt96khreyb",
 *             "url": "https://readiwise.io/new/read/01gkqtdz9xabcd5gt96khreyb",
 *             "source_url": "https://www.vanityfair.com/hollywood/2017/08/the-story-of-the-ducktales-theme-music",
 *             "title": "The Story of the DuckTales Theme, History’s Catchiest Single Minute of Music",
 *             "author": "Darryn King",
 *             "source": "Reader add from import URL",
 *             "category": "article",
 *             "location": "new",
 *             "tags": {},
 *             "site_name": "Vanity Fair",
 *             "word_count": 2678,
 *             "created_at": "2022-12-08T02:53:29.639650+00:00",
 *             "updated_at": "2022-12-13T20:37:42.544298+00:00",
 *             "published_date": "2017-08-09",
 *             "notes": "A sample note",
 *             "summary": "A woo-hoo heard around the world.",
 *             "image_url": "https://media.vanityfair.com/photos/598b1452f7f0a433bd4d149c/16:9/w_1280,c_limit/t-ducktales-woohoo-song.png",
 *             "parent_id": null,
 *             "reading_progress": 0.5,
 *         },
 *         {
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
 *     ]
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReaderListResponse(
        @JsonProperty("count") int count,
        @JsonProperty("nextPageCursor") String nextPageCursor,
        @JsonProperty("results") List<ReaderItem> results
) {}
