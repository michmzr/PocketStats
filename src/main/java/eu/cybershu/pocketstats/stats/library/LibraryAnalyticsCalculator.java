package eu.cybershu.pocketstats.stats.library;

import eu.cybershu.pocketstats.db.Item;
import eu.cybershu.pocketstats.db.ItemStatus;
import eu.cybershu.pocketstats.db.Source;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;
import static eu.cybershu.pocketstats.stats.library.LibraryAnalytics.*;

@Component
public class LibraryAnalyticsCalculator {
    public LibraryAnalytics calculate(Stream<Item> items, Instant asOf, int days, Source source) {
        Instant currentStart = asOf.minus(days, ChronoUnit.DAYS);
        Instant previousStart = currentStart.minus(days, ChronoUnit.DAYS);
        long[] ages = new long[4];
        long[] totals = new long[4]; // backlog, unknown age, excluded delay, unknown domain
        long[] periods = new long[4]; // current added/read, previous added/read
        List<Double> delays = new ArrayList<>();
        Map<String, long[]> categories = new HashMap<>();
        Map<String, long[]> domains = new HashMap<>();
        items.forEach(item -> {
            if (source != null && source != item.source()) return;
            boolean backlog = item.status() == ItemStatus.TO_READ;
            if (!backlog && item.status() != ItemStatus.ARCHIVED) return;
            String category = item.category() == null || item.category().isBlank()
                    ? "unknown" : item.category().trim().toLowerCase(Locale.ROOT);
            if (category.equals("highlight") || category.equals("note")) return;
            increment(categories, category, backlog);
            String domain = domain(item.url());
            if (domain == null) totals[3]++;
            else increment(domains, domain, backlog);
            Instant added = item.timeAdded();
            Instant read = item.timeRead();
            if (backlog) {
                totals[0]++;
                if (added == null || added.isAfter(asOf)) totals[1]++;
                else {
                    long age = Duration.between(added, asOf).toDays();
                    ages[age <= 30 ? 0 : age <= 90 ? 1 : age <= 180 ? 2 : 3]++;
                }
            } else {
                if (added == null || read == null || read.isBefore(added) || read.isAfter(asOf)) totals[2]++;
                else {
                    Duration delay = Duration.between(added, read);
                    delays.add(delay.getSeconds() / 86400.0 + delay.getNano() / 86400000000000.0);
                }
            }
            countPeriod(added, currentStart, previousStart, asOf, periods, 0);
            if (!backlog) countPeriod(read, currentStart, previousStart, asOf, periods, 1);
        });
        Collections.sort(delays);
        Double median = delays.isEmpty() ? null : delays.size() % 2 == 1
                ? delays.get(delays.size() / 2)
                : (delays.get(delays.size() / 2 - 1) + delays.get(delays.size() / 2)) / 2;
        return new LibraryAnalytics(asOf, source,
                new Backlog(totals[0], List.of(new AgeBucket("0-30", "0–30 days", ages[0]),
                        new AgeBucket("31-90", "31–90 days", ages[1]),
                        new AgeBucket("91-180", "91–180 days", ages[2]),
                        new AgeBucket("181+", "181+ days", ages[3])), totals[1]),
                new ArchiveDelay(median, delays.size(), totals[2]),
                new Comparison(days, new Period(currentStart, asOf, periods[0], periods[1]),
                        new Period(previousStart, currentStart, periods[2], periods[3]),
                        change(periods[0], periods[2]), change(periods[1], periods[3])),
                ranking(categories, Integer.MAX_VALUE), ranking(domains, 10), totals[3]);
    }

    private static void increment(Map<String, long[]> ranking, String name, boolean backlog) {
        long[] counts = ranking.computeIfAbsent(name, ignored -> new long[3]);
        counts[0]++;
        counts[backlog ? 1 : 2]++;
    }

    private static List<Ranking> ranking(Map<String, long[]> counts, int limit) {
        return counts.entrySet().stream()
                .map(entry -> new Ranking(entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]))
                .sorted(Comparator.comparingLong(Ranking::count).reversed().thenComparing(Ranking::name))
                .limit(limit).toList();
    }

    private static void countPeriod(Instant value, Instant currentStart, Instant previousStart,
                                    Instant end, long[] counts, int offset) {
        if (value == null || !value.isBefore(end) || value.isBefore(previousStart)) return;
        counts[value.isBefore(currentStart) ? offset + 2 : offset]++;
    }

    private static Change change(long current, long previous) {
        long delta = current - previous;
        return new Change(delta, previous == 0 ? null : delta * 100.0 / previous);
    }

    private static String domain(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            URI uri = URI.create(url.trim());
            String host;
            if ("mailto".equalsIgnoreCase(uri.getScheme())) {
                if (uri.getRawFragment() != null) return null;
                String recipient = uri.getRawSchemeSpecificPart().split("\\?", 2)[0].split(",", 2)[0];
                recipient = URLDecoder.decode(recipient.replace("+", "%2B"), StandardCharsets.UTF_8);
                int at = recipient.lastIndexOf('@');
                if (at <= 0 || recipient.indexOf('@') != at) return null;
                String senderHost = recipient.substring(at + 1);
                URI sender = URI.create("https://" + senderHost);
                host = sender.getHost();
                if (host == null || !host.equalsIgnoreCase(senderHost)) return null;
            } else if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
                host = uri.getHost();
            } else return null;
            if (host == null || host.isBlank()) return null;
            host = host.toLowerCase(Locale.ROOT);
            host = host.startsWith("www.") ? host.substring(4) : host;
            return host.isBlank() ? null : host;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
