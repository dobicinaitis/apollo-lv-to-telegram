package dev.dobicinaitis.feedreader.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlUtilsTest {

    private static final String PRE_REDIRECT_URL = "https://google.com";
    private static final String POST_REDIRECT_URL = "https://www.google.com/";

    @Test
    void shouldReturnTrueForValidURL() {
        assertTrue(UrlUtils.isUrlValid("https://www.example.com"));
    }

    @Test
    void shouldReturnFalseForInvalidURL() {
        assertFalse(UrlUtils.isUrlValid("invalid url"), "Invalid URL should return false.");
        assertFalse(UrlUtils.isUrlValid(null), "Null URL should return false.");
    }

    @Test
    void shouldReturnRedirectUrl() {
        assertEquals(POST_REDIRECT_URL, UrlUtils.getRedirectUrl(PRE_REDIRECT_URL), "Redirect URL should be returned.");
    }

    @Test
    void shouldReturnOriginalUrlIfNoRedirectOccurs() {
        assertEquals(POST_REDIRECT_URL, UrlUtils.getRedirectUrl(POST_REDIRECT_URL), "Original URL should be returned.");
    }

    @Test
    void shouldReturnOriginalInputOnInvalidUrl() {
        assertNull(UrlUtils.getRedirectUrl(null));
        assertEquals("invalid url", UrlUtils.getRedirectUrl("invalid url"), "Original URL should be returned.");
    }

}