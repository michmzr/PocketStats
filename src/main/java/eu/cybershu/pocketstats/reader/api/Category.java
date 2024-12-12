package eu.cybershu.pocketstats.reader.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Category {
    ARTICLE("article"),
    EMAIL("email"),
    RSS("rss"),
    HIGHLIGHT("highlight"),
    NOTE("note"),
    PDF("pdf"),
    EPUB("epub"),
    TWEET("tweet"),
    VIDEO("video");

    private final String value;

    Category(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Category forValue(String value) {
        for (Category category : Category.values()) {
            if (category.getValue().equals(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown category: " + value);
    }
}
