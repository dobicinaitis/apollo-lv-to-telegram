package dev.dobicinaitis.feedreader.services;

import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.RssReader;
import com.apptasticsoftware.rssreader.util.ItemComparator;
import dev.dobicinaitis.feedreader.exceptions.FeedReaderRuntimeException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class FeedReaderService {

    private static final int CONNECTION_TIMEOUT_IN_SECONDS = 10;
    private static final int FEED_READ_RETRY_COUNT = 3;

    private final String url;
    private final HttpClient httpClient;

    public FeedReaderService(String url) {
        this.url = url;
        this.httpClient = createHttpClient(CONNECTION_TIMEOUT_IN_SECONDS);
    }


    /**
     * Returns RSS feed items sorted by their publication date, with the oldest items listed first.
     *
     * @return list of RSS feed items
     */
    public List<Item> getItems() {
        return loadItems(FEED_READ_RETRY_COUNT).stream()
                .sorted(ItemComparator.oldestItemFirst())
                .toList();
    }

    /**
     * Loads items from the RSS feed URL.
     *
     * @param retriesLeft number of attempts to load the RSS feed
     * @return list of RSS feed items
     */
    protected List<Item> loadItems(int retriesLeft) {
        final RssReader rssReader = new RssReader(httpClient);
        while (true) {
            try {
                return rssReader.read(url).toList();
            } catch (IOException e) {
                log.error("Could not load the RSS feed, reason {}", e.getMessage());
                retriesLeft--;
                if (retriesLeft == 0) {
                    throw new FeedReaderRuntimeException(e);
                }
            }
            log.info("Retrying to load the RSS feed, retries left: {}", retriesLeft);
        }
    }

    /**
     * Creates a new HTTP client with a custom connection timeout.
     *
     * @return HTTP client
     */
    protected static HttpClient createHttpClient(int timeoutInSeconds) {
        HttpClient client;
        try {
            var sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, null, null);
            client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(timeoutInSeconds))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeoutInSeconds))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
        }
        return client;
    }
}
