package eu.cybershu.pocketstats.stats.library;

import eu.cybershu.pocketstats.api.ApiResponse;
import eu.cybershu.pocketstats.db.Source;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class LibraryAnalyticsController {
    private final LibraryAnalyticsService service;

    public LibraryAnalyticsController(LibraryAnalyticsService service) {
        this.service = service;
    }

    @GetMapping(value = "/stats/library", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<LibraryAnalytics> library(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) Source source) {
        if (days != 7 && days != 30 && days != 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be 7, 30 or 90");
        }
        return new ApiResponse<>(0, "ok", service.getAnalytics(days, source));
    }
}
