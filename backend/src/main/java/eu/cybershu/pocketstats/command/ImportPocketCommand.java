package eu.cybershu.pocketstats.command;

import eu.cybershu.pocketstats.db.PocketItemRepository;
import eu.cybershu.pocketstats.pocket.PocketApiService;
import eu.cybershu.pocketstats.shell.ShellHelper;
import eu.cybershu.pocketstats.validation.CheckDateFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@ShellComponent
public class ImportPocketCommand extends SecuredCommand {
    private static final String EXPECTED_DATE_FORMAT = "dd-MM-yyyy";

    @Lazy
    private final ShellHelper shellHelper;

    private final PocketApiService pocketApiService;

    private final PocketItemRepository repository;

    public ImportPocketCommand(ShellHelper shellHelper, PocketApiService pocketApiService,
                               PocketItemRepository repository) {
        this.shellHelper = shellHelper;
        this.pocketApiService = pocketApiService;
        this.repository = repository;
    }

    @ShellMethod("Import items since last migration")
    @ShellMethodAvailability("isUserAuthorized")
    public void importFromLast() throws IOException, InterruptedException {
        shellHelper.print("Imported: " + pocketApiService.importFromSinceLastUpdate());
    }

    @ShellMethod("Import all items from API to DB")
    @ShellMethodAvailability("isUserAuthorized")
    public void importAll() throws IOException, InterruptedException {
        shellHelper.print("Imported: " + pocketApiService.importAll());
    }

    @ShellMethod("Import items since date to DB")
    @ShellMethodAvailability("isUserAuthorized")
    public void importSince(@ShellOption(value = {"-s", "--since"}, help = "Use date in DD-MM-YYYY format")
                            @CheckDateFormat(pattern = EXPECTED_DATE_FORMAT) String date) throws IOException, InterruptedException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(EXPECTED_DATE_FORMAT);
        LocalDateTime dateTime = LocalDate.parse(date, formatter).atTime(0, 0, 1);

        var sinceWhen = dateTime.withHour(0).withMinute(0).withSecond(1).atZone(ZoneId.systemDefault()).toInstant();

        shellHelper.print("Imported: " + pocketApiService.importAllToDbSince(sinceWhen));
    }

    @ShellMethod("Cleanup pocket items from database.")
    @ShellMethodAvailability("isUserAuthorized")
    public void cleanDb() {
        repository.deleteAll();

        shellHelper.printInfo("Collection of pocket items got cleaned.");
    }
}
