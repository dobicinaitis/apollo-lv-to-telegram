package dev.dobicinaitis.feedreader.services;

import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.RssReader;
import com.apptasticsoftware.rssreader.util.ItemComparator;
import dev.dobicinaitis.feedreader.exceptions.FeedReaderRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class FeedReaderService {

    private final String url;


    /**
     * Returns RSS feed items sorted by their publication date, with the oldest items listed first.
     *
     * @return list of RSS feed items
     */
    public List<Item> getItems() {
        final RssReader rssReader = new RssReader();
        try {
            return rssReader.read(url)
                    .sorted(ItemComparator.oldestItemFirst())
                    .toList();
        } catch (IOException e) {
            log.error("Could not load items from the RSS feed.");
            throw new FeedReaderRuntimeException(e);
        }
    }
}
