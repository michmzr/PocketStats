package eu.cybershu.pocketstats.command;

import eu.cybershu.pocketstats.pocket.PocketApiService;
import eu.cybershu.pocketstats.pocket.PocketStats;
import eu.cybershu.pocketstats.pocket.api.PocketItemStatsService;
import eu.cybershu.pocketstats.shell.ShellHelper;
import eu.cybershu.pocketstats.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;

@Slf4j
@ShellComponent
public class PocketCommand extends SecuredCommand {

    @Lazy
    private final ShellHelper shellHelper;

    private final PocketApiService pocketApiService;

    private final PocketItemStatsService statsService;

    public PocketCommand(ShellHelper shellHelper, PocketApiService pocketApiService, PocketItemStatsService statsService) {
        this.shellHelper = shellHelper;
        this.pocketApiService = pocketApiService;
        this.statsService = statsService;
    }

    @ShellMethod("Print today stats")
    @ShellMethodAvailability("isUserAuthorized")
    public void statsToday(@ShellOption(value = {"--update", "-u"},
            help = "if true then app gets new items from GetPocket") Boolean updateDb) throws IOException, InterruptedException {
        log.info("Today stats - update: {}", updateDb);

        if (updateDb) {
            shellHelper.printInfo("Getting items from GetPocket");
            pocketApiService.importFromSinceLastUpdate();
        }

        PocketStats stats = statsService.getStats(
                TimeUtils.instantTodayBegin(),
                TimeUtils.instantTodayEnd()
        );

        shellHelper.print("Today's stats ->");
        shellHelper.print("added:" + stats.added());
        shellHelper.print("read:" + stats.read());
    }
}
