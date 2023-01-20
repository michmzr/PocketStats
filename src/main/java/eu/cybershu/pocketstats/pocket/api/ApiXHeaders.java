package eu.cybershu.pocketstats.pocket.api;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.net.http.HttpResponse;
import java.util.function.Function;

import static eu.cybershu.pocketstats.pocket.api.ApiXHeaders.HeaderNames.*;

/**
 * <h2>User Limit</h2>
 * Each user is limited to 320 calls per hour.
 * his should be very sufficient for most users as the average user only makes changes to their list periodically. To ensure the user stays within this limit, make use of the send method for batching requests.
 *
 * <h2>Consumer Key Limit</h2>
 * Each application is limited to 10,000 calls per hour. If your application grows to be popular and requires to be whitelisted, please contact us at api@getpocket.com. If you are adding Pocket support to an existing application with a large user base and think you might hit this limit right off the bat, please contact us at api@getpocket.com.
 *
 * @link <a href="https://getpocket.com/developer/docs/rate-limits">pocket rate limits</a>
 */
@Builder
@Value
public class ApiXHeaders {
    String status;

    /**
     * description of the problem
     */
    String error;
    Integer errorCode;
    /**
     * Current rate limit enforced per user
     **/
    Integer limitUserLimit;
    /**
     * Number of calls remaining before hitting user's rate limit
     **/
    Integer limitUserRemaining;
    /**
     * Seconds until user's rate limit resets
     */
    Integer limitUserReset;

    /**
     * Current rate limit enforced per consumer key
     */
    Integer limitKeyLimit;

    /**
     * Number of calls remaining before hitting consumer key's rate limit
     */
    Integer limitKeyRemaining;

    /**
     * Seconds until consumer key rate limit resets
     */
    Integer limitKeyReset;

    public static ApiXHeaders of(HttpResponse<String> response) {
        final Function<HeaderNames, Integer> getHeaderValue = (HeaderNames name) -> response.headers().firstValue(name.headerName).map(Integer::valueOf).orElse(null);

        final Function<HeaderNames, String> getHeaderStringValue = (HeaderNames name) -> response.headers().firstValue(name.headerName).orElse(null);

        var builder = ApiXHeaders.builder();

        builder.status(getHeaderStringValue.apply(STATUS));
        builder.error(getHeaderStringValue.apply(X_ERROR));
        builder.errorCode(getHeaderValue.apply(X_ERROR_CODE));
        builder.limitUserLimit(getHeaderValue.apply(X_LIMIT_USER_LIMIT));
        builder.limitUserRemaining(getHeaderValue.apply(X_LIMIT_USER_REMAINING));
        builder.limitUserReset(getHeaderValue.apply(X_LIMIT_USER_RESET));
        builder.limitKeyLimit(getHeaderValue.apply(X_LIMIT_KEY_LIMIT));
        builder.limitKeyRemaining(getHeaderValue.apply(X_LIMIT_KEY_REMAINING));
        builder.limitKeyReset(getHeaderValue.apply(X_LIMIT_KEY_RESET));

        return builder.build();
    }

    @Getter
    public enum HeaderNames {
        STATUS("Status"), X_ERROR("X-Error"), X_ERROR_CODE("X-Error-Code"), X_LIMIT_USER_LIMIT("X-Limit-User-Limit"), X_LIMIT_USER_REMAINING("X-Limit-User-Remaining"), X_LIMIT_USER_RESET("X-Limit-User-Reset"), X_LIMIT_KEY_LIMIT("X-Limit-Key-Limit"), X_LIMIT_KEY_REMAINING("X-Limit-Key-Remaining"), X_LIMIT_KEY_RESET("X-Limit-Key-Reset");

        private final String headerName;

        HeaderNames(String s) {
            headerName = s;
        }
    }


}
