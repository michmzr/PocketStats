package eu.cybershu.pocketstats.pocket;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
@ToString(exclude = {"code"})
public class UserCurrentlyAuthorising {
    private final String sessionId;
    private String code;
    private String link;

    public static UserCurrentlyAuthorising of(String sessionId, String code, String loginLink) {
        return new UserCurrentlyAuthorising(sessionId, code, loginLink);
    }
}
