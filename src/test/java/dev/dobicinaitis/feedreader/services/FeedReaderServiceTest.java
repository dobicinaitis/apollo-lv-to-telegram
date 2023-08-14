package dev.dobicinaitis.feedreader.services;

import dev.dobicinaitis.feedreader.exceptions.FeedReaderRuntimeException;
import dev.dobicinaitis.feedreader.helpers.TestFeedServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeedReaderServiceTest {

    private static final TestFeedServer feedServer = new TestFeedServer();

    @AfterAll
    static void afterAll() {
        feedServer.stop();
    }

    @Test
    void shouldReturnArticles() {
        // given
        final String feedUrl = feedServer.getUrl();
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
}

