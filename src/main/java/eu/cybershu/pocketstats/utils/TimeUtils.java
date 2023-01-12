package eu.cybershu.pocketstats.utils;

import java.time.*;

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

    public static LocalDateTime dayBegin(LocalDateTime localDateTime) {
        return localDateTime.withHour(0).withMinute(0).withSecond(1);
    }

    public static LocalDateTime dayEnd(LocalDateTime localDateTime) {
        return localDateTime.withHour(23).withMinute(59).withSecond(59);
    }

    public static Instant localDateTimeToInstant(LocalDateTime ldt) {
        return ldt.atZone(getZoneId()).toInstant();
    }

    private static ZoneId getZoneId() {
        return ZoneId.systemDefault();
    }
}
