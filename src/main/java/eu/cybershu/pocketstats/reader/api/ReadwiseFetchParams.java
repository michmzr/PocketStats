package eu.cybershu.pocketstats.reader.api;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 *   * Key	Type	Description	Required
 *      * id	string	The document's unique id. Using this parameter it will return just one document, if found.	no
 *      * updatedAfter	string (formatted as ISO 8601 date)	Fetch only documents updated after this date	no
 *      * location	string	The document's location, could be one of: new, later, shortlist, archive, feed	no
 *      * category	string	The document's category, could be one of: article, email, rss, highlight, note, pdf, epub, tweet, video	no
 *      * pageCursor	string	A string returned by a previous request to this endpoint. Use it to get the next page of documents if there are too many for one request.
 */
@Value
@Builder
public class ReadwiseFetchParams {
    Instant updatedAfter;
    Location location;
    Category category;

    public String toQueryParams() {
        StringBuilder builder = new StringBuilder();
        if (updatedAfter != null) {
            builder.append("updatedAfter=").append(updatedAfter).append("&");
        }
        if (location != null) {
            builder.append("location=").append(location).append("&");
        }
        if (category != null) {
            builder.append("category=").append(category).append("&");
        }
        return builder.toString();
    }
}
