package eu.cybershu.pocketstats.stats;

import eu.cybershu.pocketstats.api.ApiResponse;
import eu.cybershu.pocketstats.pocket.api.PocketItemStatsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/stats")
public class StatsController {
    private final PocketItemStatsService statsService;

    public StatsController(PocketItemStatsService statsService) {
        this.statsService = statsService;
    }

    @PostMapping(value = "/archived",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<DayStatsRecords> archived(
            @RequestBody StatsRequest statsRequest
    ) {
        log.info("Stats for {} - {}", statsRequest.start(), statsRequest.end());

        DayStatsRecords records = statsService.getDayStatsRecords(statsRequest.start(), statsRequest.end(), DayStatsType.ARCHIVED);

        return new ApiResponse<>(0, null, records);
    }

}
