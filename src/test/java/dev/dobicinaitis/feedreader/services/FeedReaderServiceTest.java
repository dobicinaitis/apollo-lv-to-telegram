package dev.dobicinaitis.feedreader.services;

import com.apptasticsoftware.rssreader.RssReader;
import dev.dobicinaitis.feedreader.exceptions.FeedReaderRuntimeException;
import dev.dobicinaitis.feedreader.helpers.TestFeedServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.*;

class FeedReaderServiceTest {

    private static final String NON_ROUTABLE_IP = "10.255.255.1";
    private static final TestFeedServer feedServer = new TestFeedServer();

    @AfterAll
    static void afterAll() {
        feedServer.stop();
    }

    @Test
    void shouldReturnArticles() {
        // given
        final String feedUrl = feedServer.getFeedUrl();
        // when
        final FeedReaderService feedReader = new FeedReaderService(feedUrl);
        // then
        assertEquals(2, feedReader.getItems().size(), "Should return 2 articles");
        assertTrue(feedReader.getItems().get(0).getTitle().get().startsWith("VIDEO ⟩"), "Should return correct title");
        assertEquals("https://t.ly/nSXIV", feedReader.getItems().get(0).getLink().get(), "Should return correct link");
        assertEquals("1970-01-01T00:00+03:00", feedReader.getItems().get(0).getPubDateZonedDateTime().orElseThrow().toString(),
                "Should return correct publication date");
    }

    @Test
    void shouldThrowFeedReaderRuntimeExceptionWhenFeedIsUnavailable() {
        final String feedUrl = "http://localhost/invalid";
        final FeedReaderService feedReader = new FeedReaderService(feedUrl);
        assertThrows(FeedReaderRuntimeException.class, feedReader::getItems, "Should throw FeedReaderRuntimeException");
    }

    @Test
    @Timeout(3)
    void shouldRespectHttpRequestTimeoutSettings() {
        // given
        final int timeoutInSeconds = 1;
        final String feedUrl = "https://" + NON_ROUTABLE_IP;
        final HttpClient httpClient = FeedReaderService.createHttpClient(timeoutInSeconds);
        final RssReader rssReader = new RssReader(httpClient);
        // when, then
        assertThrows(IOException.class, () -> rssReader.read(feedUrl), "Should throw IOException");
        // the exception should be thrown in ~1s, therefore if it takes longer then 3s, the test will fail
    }
}

