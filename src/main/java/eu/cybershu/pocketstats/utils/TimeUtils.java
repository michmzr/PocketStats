package eu.cybershu.pocketstats.utils;

import java.time.*;

public class TimeUtils {
    public static Instant instantTodayBegin() {
        return OffsetDateTime
                .now(defaultTimeZone())
                .with(LocalTime.of(0, 0, 1)).toInstant();
    }

    public static Instant instantTodayEnd() {
        return OffsetDateTime
                .now(defaultTimeZone())
                .with(LocalTime.of(23, 59, 59)).toInstant();
    }

    public static Instant toStartOfDay(Instant instant) {
        if (instant == null) {
            throw new IllegalArgumentException("Instant cannot be null");
        }

        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, defaultTimeZone())
                .withHour(00)
                .withMinute(0)
                .withSecond(0)
                .withNano(1);

        return localDateTime.atZone(defaultTimeZone()).toInstant();
    }

    public static Instant toEndOfDay(Instant instant) {
        if (instant == null) {
            throw new IllegalArgumentException("Instant cannot be null");
        }

        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, defaultTimeZone())
                .withHour(23)
                .withMinute(59)
                .withSecond(59);

        return localDateTime.atZone(defaultTimeZone()).toInstant();
    }

    public static Instant toStartDayInstant(LocalDate date) {
        LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.of(0, 0, 1));
        return dateTime.atZone(defaultTimeZone()).toInstant();
    }

    private static ZoneOffset defaultTimeZone() {
        return ZoneOffset.UTC;
    }

    public static Instant toEndOfDayInstant(LocalDate date) {
        LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.of(23, 59, 59));
        return dateTime.atZone(defaultTimeZone()).toInstant();
    }

    private static Instant toInstant(LocalDate localDate) {
        return localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
    }

    private static LocalDate getLocalDate() {
        return LocalDate.now();
    }
}
