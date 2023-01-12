package eu.cybershu.pocketstats.command;

import eu.cybershu.pocketstats.pocket.PocketApiService;
import eu.cybershu.pocketstats.shell.ShellHelper;
import eu.cybershu.pocketstats.stats.PocketStatPredicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import java.io.IOException;
import java.util.Map;

@ShellComponent
public class PocketCommand extends SecuredCommand {

    @Lazy
    @Autowired
    private ShellHelper shellHelper;

    @Autowired
    private PocketApiService pocketApiService;

    @ShellMethod("Get current month records")
    @ShellMethodAvailability("isUserAuthorized")
    public void currentMonth() throws IOException, InterruptedException {
        Map<PocketStatPredicate, Integer> stats = pocketApiService.getCurrentMonth();

        printStats(stats);
    }

    @ShellMethod("Items left to read")
    @ShellMethodAvailability("isUserAuthorized")
    public void toRead() throws IOException, InterruptedException {
        int counter = pocketApiService.itemsToRead();
        shellHelper.print("Items to read:" + counter);
    }

    private void printStats(Map<PocketStatPredicate, Integer> stats) {
        stats.forEach((pred, counter)-> shellHelper.print(pred.getName() + ":" + counter));
    }
}
