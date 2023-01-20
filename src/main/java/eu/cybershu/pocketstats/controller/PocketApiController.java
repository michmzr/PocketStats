package eu.cybershu.pocketstats.controller;

import eu.cybershu.pocketstats.pocket.PocketAuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/pocket/auth/")
public class PocketApiController {
    @Autowired
    private PocketAuthorizationService authorizationService;

    @GetMapping("token")
    public String onRedirect() {
        log.info("Redirection successfull");

        authorizationService.deregisterAuthSession();

        return "authorized app. You can go back to console.";
    }
}
