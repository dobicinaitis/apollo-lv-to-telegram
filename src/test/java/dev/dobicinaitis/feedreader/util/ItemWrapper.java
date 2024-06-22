package dev.dobicinaitis.feedreader.util;

import com.apptasticsoftware.rssreader.DateTime;
import com.apptasticsoftware.rssreader.Enclosure;
import com.apptasticsoftware.rssreader.Item;
import lombok.Builder;

import java.util.List;

/**
 * Wrapper class for creating test RSS Item.
 */
@Builder
public class ItemWrapper {

    private String title;
    private String description;
    private String link;
    private String enclosureUrl;
    private String enclosureType;
    private List<String> categories;

    public Item toRssItem() {
        final Item item = new Item(new DateTime());
        item.setTitle(title);
        item.setDescription(description);
        item.setLink(link);

        final Enclosure enclosure = new Enclosure();
        enclosure.setUrl(enclosureUrl);
        enclosure.setType(enclosureType);
        enclosure.setLength(0L);
        item.setEnclosure(enclosure);

        if (categories != null) {
            categories.forEach(item::addCategory);
        }

        return item;
    }
}
