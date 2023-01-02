package eu.cybershu.pocketstats.command;

import eu.cybershu.pocketstats.PocketApiService;
import eu.cybershu.pocketstats.shell.ShellHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;

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
    public void currentMonthRecords(@ShellOption({"-a", "--access-token"}) String accessToken) throws IOException,
            InterruptedException {
        shellHelper.print(
                pocketApiService.getCurrentMonth(accessToken)
        );
    }
}
