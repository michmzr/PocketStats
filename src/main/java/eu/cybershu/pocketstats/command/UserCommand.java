package eu.cybershu.pocketstats.command;

import eu.cybershu.pocketstats.shell.InputReader;
import eu.cybershu.pocketstats.shell.ShellHelper;
import eu.cybershu.pocketstats.user.PocketUser;
import eu.cybershu.pocketstats.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.StringUtils;

@ShellComponent
public class UserCommand {
    @Autowired
    private ShellHelper shellHelper;

    @Autowired
    private  UserService userService;

    @Autowired
    private InputReader inputReader;

    @ShellMethod("Create new user with supplied username")
    public void createUser(@ShellOption({"-U", "--username"}) String username) {
        if (userService.exists(username)) {
            shellHelper.printError(String.format("User with username='%s' already exists --> ABORTING", username));
            return;
        }

        var  pocketUserBldr = PocketUser.builder();

        // 1. read user's fullName --------------------------------------------
        do {
            String fullName = inputReader.prompt("Full name");
            if (StringUtils.hasText(fullName)) {
                pocketUserBldr.fullName(fullName);
                break;
            }else {
                shellHelper.printWarning("User's full name CAN NOT be empty string? Please enter valid value!");
            }
        } while (true);

        // 2. read user's password --------------------------------------------
        do {
            String password = inputReader.prompt("Password", "secret", false);
            if (StringUtils.hasText(password)) {
                pocketUserBldr.password(password);
                break;
            } else {
                shellHelper.printWarning("Password'CAN NOT be empty string? Please enter valid value!");
            }
        } while (true);

        PocketUser createdPocketUser = userService.register(pocketUserBldr.build());
        shellHelper.printSuccess("Created user with id=" + createdPocketUser.getUuid());
    }
}
