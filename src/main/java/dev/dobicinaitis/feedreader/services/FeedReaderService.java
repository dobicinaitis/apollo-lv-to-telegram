package dev.dobicinaitis.feedreader.services;

import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.RssReader;
import com.apptasticsoftware.rssreader.util.ItemComparator;
import dev.dobicinaitis.feedreader.exceptions.FeedReaderRuntimeException;
import dev.failsafe.Failsafe;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;

import static dev.dobicinaitis.feedreader.configuration.FailsafeConfiguration.RETRY_POLICY;

@Slf4j
@AllArgsConstructor
public class FeedReaderService {

    private static final int CONNECTION_TIMEOUT_IN_SECONDS = 10;

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
        return loadItems().stream()
                .sorted(ItemComparator.oldestItemFirst())
                .toList();
    }

    /**
     * Loads items from the RSS feed URL.
     *
     * @return list of RSS feed items
     */
    protected List<Item> loadItems() {
        try {
            return Failsafe.with(RETRY_POLICY)
                    .get(() -> new RssReader(httpClient).read(url).toList());
        } catch (Exception e) {
            log.error("Could not load the RSS feed, reason: {}", e.getMessage());
            throw new FeedReaderRuntimeException(e);
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
