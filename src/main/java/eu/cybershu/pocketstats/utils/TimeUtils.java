package eu.cybershu.pocketstats.utils;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

public class TimeUtils {

    public static LocalDateTime getFirstDayOfLastYear() {
        var now = LocalDateTime.now();
        LocalDate lastyear =  Year.of(now.getYear()-1).atMonth(Month.JANUARY).atDay(1);
        return LocalDateTime.of(lastyear, LocalTime.of(0,0,1));
    }

    public static LocalDateTime getStartOfCurrentMonth() {
        LocalDateTime time = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth());
        return time.with(LocalTime.of(0,0,1));
    }

    private static ZoneId getZoneId() {
        return ZoneId.systemDefault();
    }
}
