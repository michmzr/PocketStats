package eu.cybershu.pocketstats

import eu.cybershu.pocketstats.db.PocketItem
import eu.cybershu.pocketstats.pocket.api.ItemStatus

import java.time.Instant;

class PocketItemBuilder {
    static PocketItem todo(Instant timeAdded = Instant.now(),
                           String title = "Johhny Doe eats banana", String url = "http://localhost/url/") {
        def item = new PocketItem()
        item.status(ItemStatus.TO_READ)
        item.timeAdded(timeAdded)

        item.id(UUID.randomUUID().toString())
        item.url(url)
        item.title(title)
        item.excerpt(title.md5())

        item
    }

    static PocketItem archived(Instant timeAdded = Instant.now(),
                               Instant timeRead = Instant.now(),
                               String title = "Johhny Doe eated banana", String url = "http://localhost/url/") {
        def item = new PocketItem()
        item.status(ItemStatus.ARCHIVED)
        item.timeAdded(timeAdded)
        item.timeRead(timeRead)
        item.id(UUID.randomUUID().toString())
        item.url(url)
        item.title(title)
        item.excerpt(title.md5())

        item
    }
}
