package eu.cybershu.pocketstats.utils;

import javax.validation.constraints.NotNull;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

public record TimePeriod(LocalDate begin, LocalDate end) {

    public TimePeriod(
            @NotNull LocalDate begin,
            @NotNull LocalDate end) {
        if (end.isBefore(begin)) {
            this.begin = end;
            this.end = begin;
        } else {
            this.begin = begin;
            this.end = end;
        }
    }

    public static TimePeriod currentWeek(Clock clock) {
        LocalDate now = getLocalDate(clock);
        LocalDate startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new TimePeriod(startOfWeek, endOfWeek);
    }

    public static TimePeriod previousWeek(Clock clock) {
        LocalDate now = getLocalDate(clock);
        LocalDate startOfPrevWeek = now.with(TemporalAdjusters.previous(DayOfWeek.MONDAY)).minusDays(7);
        LocalDate endOfPrevWeek = now.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        return new TimePeriod(startOfPrevWeek, endOfPrevWeek);
    }

    private static LocalDate getLocalDate(Clock clock) {
        if (clock == null) {
            return LocalDate.now(Clock.systemUTC());
        } else {
            return LocalDate.now();
        }
    }

    public static TimePeriod currentMonth(Clock clock) {
        LocalDate now = getLocalDate(clock);
        YearMonth yearMonth = YearMonth.from(now);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();
        return new TimePeriod(startOfMonth, endOfMonth);
    }

    public static TimePeriod lastMonth(Clock clock) {
        LocalDate now = getLocalDate(clock);
        YearMonth prevYearMonth = YearMonth.from(now).minusMonths(1);
        LocalDate startOfPrevMonth = prevYearMonth.atDay(1);
        LocalDate endOfPrevMonth = prevYearMonth.atEndOfMonth();
        return new TimePeriod(startOfPrevMonth, endOfPrevMonth);
    }

    public static TimePeriod currentYear(Clock clock) {
        LocalDate now = getLocalDate(clock);
        LocalDate startOfYear = now.withDayOfYear(1);
        LocalDate endOfYear = now.withDayOfYear(now.lengthOfYear());
        return new TimePeriod(startOfYear, endOfYear);
    }

    public static TimePeriod lastYear(Clock clock) {
        LocalDate now = getLocalDate(clock);
        LocalDate startOfLastYear = now.minusYears(1).withDayOfYear(1);
        LocalDate endOfLastYear = now.minusYears(1).withDayOfYear(now.minusYears(1).lengthOfYear());
        return new TimePeriod(startOfLastYear, endOfLastYear);
    }
}
