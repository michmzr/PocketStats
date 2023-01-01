package eu.cybershu.pocketstats.user;

public interface UserService {
    Boolean exists(String username);
    PocketUser register(PocketUser pocketUser);
}
