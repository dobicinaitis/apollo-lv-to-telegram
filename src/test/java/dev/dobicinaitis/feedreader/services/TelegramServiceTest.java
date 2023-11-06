package dev.dobicinaitis.feedreader.services;

import dev.dobicinaitis.feedreader.helpers.TestFeedServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class TelegramServiceTest {

    private static final TestFeedServer feedServer = new TestFeedServer();

    @AfterAll
    static void afterAll() {
        feedServer.stop();
    }

    @Test
    void shouldEscapeSpecialCharacters() {
        assertEquals("\\\\", TelegramService.escapeSpecialCharacters("\\"), "Backslash should be escaped.");
        assertEquals("\\`", TelegramService.escapeSpecialCharacters("`"), "Backtick should be escaped.");
        assertEquals("\\*", TelegramService.escapeSpecialCharacters("*"), "Asterisk should be escaped.");
        assertEquals("\\_", TelegramService.escapeSpecialCharacters("_"), "Underscore should be escaped.");
        assertEquals("\\{", TelegramService.escapeSpecialCharacters("{"), "Opening curly brace should be escaped.");
        assertEquals("\\}", TelegramService.escapeSpecialCharacters("}"), "Closing curly brace should be escaped.");
        assertEquals("\\[", TelegramService.escapeSpecialCharacters("["), "Opening square bracket should be escaped.");
        assertEquals("\\]", TelegramService.escapeSpecialCharacters("]"), "Closing square bracket should be escaped.");
        assertEquals("\\<", TelegramService.escapeSpecialCharacters("<"), "Opening angle bracket should be escaped.");
        assertEquals("\\>", TelegramService.escapeSpecialCharacters(">"), "Closing angle bracket should be escaped.");
        assertEquals("\\(", TelegramService.escapeSpecialCharacters("("), "Opening parenthesis should be escaped.");
        assertEquals("\\)", TelegramService.escapeSpecialCharacters(")"), "Closing parenthesis should be escaped.");
        assertEquals("\\#", TelegramService.escapeSpecialCharacters("#"), "Hash should be escaped.");
        assertEquals("\\+", TelegramService.escapeSpecialCharacters("+"), "Plus should be escaped.");
        assertEquals("\\-", TelegramService.escapeSpecialCharacters("-"), "Minus should be escaped.");
        assertEquals("\\.", TelegramService.escapeSpecialCharacters("."), "Dot should be escaped.");
        assertEquals("\\!", TelegramService.escapeSpecialCharacters("!"), "Exclamation mark should be escaped.");
        assertEquals("\\|", TelegramService.escapeSpecialCharacters("|"), "Pipe should be escaped.");
    }

    @Test
    void shouldPrepareImagesUsingRandomFilenames() throws IOException {
        // given
        final String imageFilename = TestFeedServer.IMAGE_FILE_RC_200;
        final String imageExtension = imageFilename.substring(imageFilename.lastIndexOf(".") + 1); // .gif
        final String imageUrl = feedServer.getBaseUrl() + "/" + imageFilename;
        // when
        final InputFile preparedImage1 = TelegramService.prepareImage(imageUrl);
        final InputFile preparedImage2 = TelegramService.prepareImage(imageUrl);
        // then
        assertNotEquals(preparedImage1.getMediaName(), preparedImage2.getMediaName(), "Prepared image filenames should not be the same.");
        assertNotEquals(imageFilename, preparedImage1.getMediaName(), "Prepared image should not have the original filename.");
        assertNotEquals(imageFilename, preparedImage2.getMediaName(), "Prepared image should not have the original filename.");
        assertTrue(preparedImage1.getMediaName().endsWith(imageExtension), "Prepared image should have the original file extension.");
        assertTrue(preparedImage2.getMediaName().endsWith(imageExtension), "Prepared image should have the original file extension.");
    }

    @Test
    void shouldThrowAnIOExceptionWhenImageIsUnavailable() {
        // given
        final String imageUrl = feedServer.getBaseUrl() + "/" + TestFeedServer.IMAGE_FILE_RC_503;
        // when, then
        assertThrows(IOException.class, () -> TelegramService.prepareImage(imageUrl), "Should throw IOException.");
    }

}