package eu.cybershu.pocketstats.controller;

import eu.cybershu.pocketstats.PocketApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pocket/auth/")
public class PocketApiController {
    @Autowired
    private PocketApiService pocketApiService;

    @GetMapping("token")
    public String onRedirect(@RequestParam("sessionId") String sessionId) {
        pocketApiService.deregisterAuthSession(sessionId);

        return "authorized app. You can go back to console.";
    }
}
