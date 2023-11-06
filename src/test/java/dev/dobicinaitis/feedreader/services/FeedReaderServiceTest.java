package dev.dobicinaitis.feedreader.services;

import dev.dobicinaitis.feedreader.exceptions.FeedReaderRuntimeException;
import dev.dobicinaitis.feedreader.helpers.TestFeedServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

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
        final int retryCount = 1;
        final String feedUrl = "https://" + NON_ROUTABLE_IP;
        final HttpClient httpClient = FeedReaderService.createHttpClient(timeoutInSeconds);
        final FeedReaderService feedReader = new FeedReaderService(feedUrl, httpClient);
        // when, then
        assertThrows(FeedReaderRuntimeException.class, () -> feedReader.loadItems(retryCount), "Should throw FeedReaderRuntimeException");
        // the timeout should occur after 1 second, so anything under 3 seconds is fine
    }

    @Test
    @Timeout(5)
    void shouldAttemptToLoadFeedMultipleTimes(){
        // given
        final int timeoutPerRequestInSeconds = 1;
        final int retryCount = 3;
        final String feedUrl = "https://" + NON_ROUTABLE_IP;
        final HttpClient httpClient = FeedReaderService.createHttpClient(timeoutPerRequestInSeconds);
        final FeedReaderService feedReader = new FeedReaderService(feedUrl, httpClient);
        // when, then
        assertThrows(FeedReaderRuntimeException.class, () -> feedReader.loadItems(retryCount), "Should throw FeedReaderRuntimeException");
        // the timeout should occur after 3 seconds, so anything under 5 seconds is fine
    }
}

