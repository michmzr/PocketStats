package eu.cybershu.pocketstats


import eu.cybershu.pocketstats.db.Item
import eu.cybershu.pocketstats.db.ItemStatus

import java.time.Instant

class ItemBuilder {
    static Item todo(Instant timeAdded = Instant.now(),
                     String title = "Johhny Doe eats banana", String url = "http://localhost/url/") {
        def item = new Item()
        item.status(ItemStatus.TO_READ)
        item.timeAdded(timeAdded)

        item.id(UUID.randomUUID().toString())
        item.url(url)
        item.title(title)
        item.excerpt(title.md5())

        item
    }

    static Item archived(Instant timeAdded = Instant.now(),
                         Instant timeRead = Instant.now(),
                         String title = "Johhny Doe eated banana", String url = "http://localhost/url/") {
        def item = new Item()
        item.status(ItemStatus.ARCHIVED)
        item.timeAdded(timeAdded)
        item.timeRead(timeRead)
        item.timeUpdated(timeRead)
        item.id(UUID.randomUUID().toString())
        item.url(url)
        item.title(title)
        item.excerpt(title.md5())

        item
    }

    static Item withLang(Item item, String lang) {
        item.lang(lang)
        item
    }
}
