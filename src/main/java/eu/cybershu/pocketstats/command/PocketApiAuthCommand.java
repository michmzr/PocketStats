package eu.cybershu.pocketstats.command;

import eu.cybershu.pocketstats.pocket.PocketAuthorizationService;
import eu.cybershu.pocketstats.pocket.PocketUserCredentials;
import eu.cybershu.pocketstats.shell.ShellHelper;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.IOException;
import java.util.Optional;

@ShellComponent
public class PocketApiAuthCommand {
    private final ShellHelper shellHelper;
    private final PocketAuthorizationService authorizationService;

    public PocketApiAuthCommand(

            ShellHelper shellHelper, PocketAuthorizationService authorizationService) {
        this.shellHelper = shellHelper;
        this.authorizationService = authorizationService;
    }

    @ShellMethod("Authorize in PocketAPI")
    public void authorize() throws IOException, InterruptedException {
        final String code = authorizationService.obtainAuthCode();
        shellHelper.printInfo("Your code: " + code);

        authorizationService.registerAuthSession();

        shellHelper.printInfo("Click on link to proceed:\n" + authorizationService.generateLoginUrl(code));

        while (authorizationService.isAuthSessionActive()) {
            Thread.sleep(80);
        }
        ;

        shellHelper.printInfo("App authorized to your API.");

        final String accessToken = authorizationService.authorize(code);
        shellHelper.printInfo("Access token: " + accessToken);

        authorizationService.save(new PocketUserCredentials(code, accessToken));
        shellHelper.printInfo("Credentials saved to file.");
    }

    @ShellMethod("Reset access token")
    public void resetAccessToken() throws IOException, InterruptedException {
        Optional<PocketUserCredentials> credsOpt = authorizationService.getCredentials();

        if (credsOpt.isPresent()) {
            final String accessToken = authorizationService.authorize(credsOpt.get().code());
            authorizationService.save(new PocketUserCredentials(credsOpt.get().code(), accessToken));
            shellHelper.printInfo("Access token: " + accessToken);
        } else {
            authorize();
        }
    }
}
