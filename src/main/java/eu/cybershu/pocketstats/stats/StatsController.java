package eu.cybershu.pocketstats.stats;

import eu.cybershu.pocketstats.api.ApiResponse;
import eu.cybershu.pocketstats.pocket.api.ActivityHeatmapStats;
import eu.cybershu.pocketstats.pocket.api.ItemsStatsAggregated;
import eu.cybershu.pocketstats.pocket.api.ItemsStatsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/stats")
public class StatsController {
    private final ItemsStatsService statsService;

    public StatsController(ItemsStatsService statsService) {
        this.statsService = statsService;
    }

    @PostMapping(value = "/byDay",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<DayStatsRecords> statsByDay(
            @RequestBody StatsRequest statsRequest
    ) {
        log.info("Stats for {}", statsRequest);

        DayStatsRecords records = statsService.getDayStatsRecords(
                statsRequest.start(), statsRequest.end(), statsRequest.type());

        return new ApiResponse<>(0, null, records);
    }

    @GetMapping(value = "/topTags", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<TopTags> topTags(@RequestParam(required = false) Integer count) {
        count = count == null ? 10 : count;
        log.info("Top {} tags", count);

        List<TopTag> topTags = statsService.getTopTags(count);
        return new ApiResponse<>(0, null, new TopTags(topTags, count));
    }

    @GetMapping(value = "/langs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<LangStats> langsStats() {
        var langStats = statsService.getLangStats();
        return new ApiResponse<>(0, null, new LangStats(langStats));
    }

    @GetMapping(value = "/byPeriods", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ItemsStatsAggregated> itemStatsAggregatedByPeriod() {
        return new ApiResponse<>(0, null, statsService.itemsStatsAggregated());
    }

    @GetMapping(value = "/heatmap",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ActivityHeatmapStats> heatmapStats(
            @RequestParam StatsWithStatusType type
    ) {
        log.info("Heatmap with status {}", type);

        var heatmapRecords = statsService.heatmapOfStatus(type);

        return new ApiResponse<>(0, null, heatmapRecords);
    }
}
