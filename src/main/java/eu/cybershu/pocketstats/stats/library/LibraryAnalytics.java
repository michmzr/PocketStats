package eu.cybershu.pocketstats.stats.library;

import eu.cybershu.pocketstats.db.Source;
import java.time.Instant;
import java.util.List;

public record LibraryAnalytics(Instant asOf, Source source, Backlog backlog, ArchiveDelay archiveDelay,
                               Comparison comparison, List<Ranking> categories, List<Ranking> domains,
                               long unknownDomainCount) {
    public record Backlog(long total, List<AgeBucket> ageBuckets, long unknownAge) {}
    public record AgeBucket(String key, String label, long count) {}
    public record ArchiveDelay(Double medianDays, long eligibleRecords, long excludedRecords) {}
    public record Period(Instant start, Instant end, long added, long read) {}
    public record Change(long delta, Double percent) {}
    public record Comparison(int days, Period current, Period previous, Change added, Change read) {}
    public record Ranking(String name, long count, long backlog, long archived) {}
}
