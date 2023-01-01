package eu.cybershu.pocketstats.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class PocketUser {
    private String uuid;
    private  String username;
    private String pocketUserName;
    private String fullName;
    private String password;
}
