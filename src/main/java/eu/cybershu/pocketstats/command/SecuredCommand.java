package eu.cybershu.pocketstats.command;

import eu.cybershu.pocketstats.pocket.PocketAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;

public abstract class SecuredCommand {
    @Autowired
    PocketAuthorizationService authorizationService;

    public Availability isUserAuthorized() {
        if (authorizationService.getCredentials().isEmpty()) {
            return Availability.unavailable("you are authorized to GetPocket Api. Please use authorize command to be " + "able to " + "use " + "this command!");
        } else {
            return Availability.available();
        }
    }
}
