package eu.cybershu.pocketstats.user;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserServiceImpl implements UserService{
    private final Map<String, PocketUser> users;

    public UserServiceImpl() {
        this.users = new ConcurrentHashMap<>();
    }

    @Override
    public Boolean exists(String username) {
        return username.equals("admin") || this.users.containsKey(username);
    }

    @Override
    public PocketUser register(PocketUser pocketUser) {
        pocketUser.setUuid(UUID.randomUUID().toString());
        this.users.put(pocketUser.getUsername(), pocketUser);
        return pocketUser;
    }
}
