package eu.cybershu.pocketstats.pocket.api;

import com.google.common.base.Preconditions;
import eu.cybershu.pocketstats.db.PocketItemRepository;
import eu.cybershu.pocketstats.pocket.PocketStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class PocketItemStatsService {
    private final PocketItemRepository pocketItemRepository;

    public PocketItemStatsService(PocketItemRepository pocketItemRepository) {
        this.pocketItemRepository = pocketItemRepository;
    }

    private Long calcReadBetween(Instant a, Instant b) {
        log.info("Calc read between {} and {} ", a, b);
        return pocketItemRepository.countAllByTimeReadBetween(a, b);
    }

    private Long calcAddedBetween(Instant a, Instant b) {
        log.info("Calc added between {} and {} ", a, b);
        return pocketItemRepository.countAllByTimeAddedBetween(a, b);
    }

    public PocketStats getStats(Instant start, Instant end) {
        Preconditions.checkArgument(start.isBefore(end));

        return new PocketStats(
                calcAddedBetween(start, end),
                calcReadBetween(start, end)
        );
    }

}
