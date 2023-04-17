package eu.cybershu.pocketstats.events;

import eu.cybershu.pocketstats.sync.SyncStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class EventsPublisher {
    private final ApplicationEventPublisher publisher;

    public EventsPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void sendUserSynchronizedItems(SyncStatus syncStatus) {
        publisher.publishEvent(new UserSynchronizedItemsEvent(syncStatus));
    }
}
