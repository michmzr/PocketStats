package eu.cybershu.pocketstats.stats;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

public record StatsRequest(
        @NotNull
        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate start,
        @NotNull
        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate end,

        @NotNull
        StatsWithStatusType type
) {
}
