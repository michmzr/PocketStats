package eu.cybershu.pocketstats.events;

import eu.cybershu.pocketstats.sync.SyncStatus;
import org.springframework.context.ApplicationEvent;

public class UserSynchronizedItemsEvent extends ApplicationEvent {
    private final SyncStatus syncStatus;
    public UserSynchronizedItemsEvent(SyncStatus source) {
        super(source);
        this.syncStatus = source;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }
}
