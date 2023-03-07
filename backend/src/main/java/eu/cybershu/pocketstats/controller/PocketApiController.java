package eu.cybershu.pocketstats.controller;

import eu.cybershu.pocketstats.api.ApiResponse;
import eu.cybershu.pocketstats.pocket.PocketAuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("token/{id}")
    public String onRedirect(@PathVariable("id") String id) throws IOException, InterruptedException {
        log.info("Redirection successful - session id = {}", id);

        authorizationService.completeAuthProcess();

        return "App is authorized. You can now start using beauty of data.";
    }

    @GetMapping(value = "/authorized",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<AuthorizationStatus> authorized() {
        AuthorizationStatus status = new AuthorizationStatus(
                authorizationService.getCredentials().isPresent());

        return new ApiResponse<>(0, "ok", status);
    }

    @GetMapping(value = "/login",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<AuthorizationLink> loginToPocket() throws IOException, InterruptedException {
        final String sessionId = authorizationService.generateSessionId();
        final String code = authorizationService.obtainAuthCode(sessionId);

        final AuthorizationLink authorizationLink = new AuthorizationLink(authorizationService.generateLoginUrl(code,
                sessionId), code);
        authorizationService.startAuthProcess(
                sessionId, code, authorizationLink.link()
        );

        return new ApiResponse<>(0, "ok", authorizationLink);
    }
}
