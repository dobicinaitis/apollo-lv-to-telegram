package dev.dobicinaitis.feedreader.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TelegramServiceTest {

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

}