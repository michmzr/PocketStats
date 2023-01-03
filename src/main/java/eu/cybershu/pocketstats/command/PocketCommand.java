package eu.cybershu.pocketstats.command;

import eu.cybershu.pocketstats.PocketApiService;
import eu.cybershu.pocketstats.shell.ShellHelper;
import eu.cybershu.pocketstats.stats.PocketStatPredicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@ShellComponent
public class PocketCommand {

    @Lazy
    @Autowired
    private ShellHelper shellHelper;

    @Autowired
    private PocketApiService pocketApiService;

    @ShellMethod("Authorize")
    public void authorize() throws IOException, InterruptedException {
        final String code = pocketApiService.obtainAuthCode();

        shellHelper.printInfo("Your code: " + code);
        pocketApiService.registerAuthSession(code);

        shellHelper.printInfo("Click on link to proceed:\n" +  pocketApiService.generateLoginUrl(code));

        while (pocketApiService.isAuthSessionActive(code)) { //todo better way
            Thread.sleep(80);
        };

        shellHelper.printInfo("App authorized to your API.");

        final String accessToken =  pocketApiService.authorize(code);

        shellHelper.printInfo("Access token: " + accessToken);
    }

    @ShellMethod("Get current month records")
    public void currentMonth(@ShellOption({"-a", "--access-token"}) String accessToken) throws IOException,
            InterruptedException {
        Map<PocketStatPredicate, Integer> stats = pocketApiService.getCurrentMonth(accessToken);

        printStats(stats);
    }

    @ShellMethod("Get previous year stats")
    public void previousYear(@ShellOption({"-a", "--access-token"}) String accessToken) throws IOException,
            InterruptedException {
        Map<PocketStatPredicate, Integer> stats = pocketApiService.getLastYearItems(accessToken);

        printStats(stats);
    }

    @ShellMethod("Import last years to DB")
    public void importLastYears(@ShellOption({"-a", "--access-token"}) String accessToken,
                                @ShellOption("-y") Integer years) throws IOException,
            InterruptedException {
        Instant sinceWhen = LocalDateTime.now().minusYears(years).atZone(ZoneId.systemDefault()).toInstant();

        shellHelper.print("Imported: " + pocketApiService.importAllToDbSince(accessToken, sinceWhen));
    }

    @ShellMethod("Items left to read")
    public void toRead(@ShellOption({"-a", "--access-token"}) String accessToken) throws IOException,
            InterruptedException {
        int counter = pocketApiService.itemsToRead(accessToken);
        shellHelper.print("Items to read:" + counter);
    }

    private void printStats(Map<PocketStatPredicate, Integer> stats) {
        stats.forEach((pred, counter)-> shellHelper.print(pred.getName() + ":" + counter));
    }
}
