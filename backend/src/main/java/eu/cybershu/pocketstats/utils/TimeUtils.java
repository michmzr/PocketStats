package eu.cybershu.pocketstats.utils;

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class TimeUtils {
    public static Instant instantTodayBegin() {
        return OffsetDateTime
                .now(ZoneOffset.UTC)
                .with(LocalTime.of(0, 0, 1)).toInstant();
    }

    public static Instant instantTodayEnd() {
        return OffsetDateTime
                .now(ZoneOffset.UTC)
                .with(LocalTime.of(23, 59, 59)).toInstant();
    }
}
