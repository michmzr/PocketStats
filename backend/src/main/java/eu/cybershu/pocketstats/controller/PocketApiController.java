package eu.cybershu.pocketstats.controller;

import eu.cybershu.pocketstats.api.ApiResponse;
import eu.cybershu.pocketstats.pocket.PocketAuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

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

    @GetMapping(value = "/authorized",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<AuthorizationStatus> authorized() {
        AuthorizationStatus status = new AuthorizationStatus(
                authorizationService.getCredentials().isPresent());

        return new ApiResponse<AuthorizationStatus>(0, "ok", status);
    }

    @GetMapping(value = "/login_url",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<AuthorizationLink> authorizationLink() throws IOException, InterruptedException {
        final String code = authorizationService.obtainAuthCode();

        authorizationService.registerAuthSession();

        var response = new ApiResponse<>(0, "ok",
                new AuthorizationLink(authorizationService.generateLoginUrl(code), code)
        );

        return response;

    }
}
