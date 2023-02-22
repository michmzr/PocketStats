package eu.cybershu.pocketstats.controller;

import eu.cybershu.pocketstats.pocket.PocketAuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/pocket/auth/")
public class PocketApiController {
    private final PocketAuthorizationService authorizationService;

    public PocketApiController(PocketAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("token")
    public String onRedirect() {
        log.info("Redirection successful.");

        authorizationService.deregisterAuthSession();

        return "authorized app. You can go back to console.";
    }

//    @GetMapping("/auth/authorized")
//    public Boolean authorized() {
//        return authorizationService.
//    }
}
