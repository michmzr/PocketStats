package eu.cybershu.pocketstats.command;

import org.springframework.shell.Availability;
public abstract class SecuredCommand {
    public Availability isUserSignedIn() {
        return Availability.unavailable("you are not signedIn. Please sign in to be able to use this command!");
    }
}
