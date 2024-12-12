package eu.cybershu.pocketstats.reader.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

//values: new, later, shortlist, archive, feed
public enum Location {
    NEW("new"),
    LATER("later"),
    SHORTLIST("shortlist"),
    ARCHIVE("archive"),
    FEED("feed");

    private final String value;

    Location(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Location forValue(String value) {
        for (Location location : Location.values()) {
            if (location.getValue().equals(value)) {
                return location;
            }
        }
        throw new IllegalArgumentException("Unknown location: " + value);
    }
}
