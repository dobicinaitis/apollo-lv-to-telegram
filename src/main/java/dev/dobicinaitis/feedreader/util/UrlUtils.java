package dev.dobicinaitis.feedreader.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
public class UrlUtils {

    private UrlUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Returns true if the given URL is valid, false otherwise.
     *
     * @param urlString URL to check
     * @return true if the given URL is valid, false otherwise
     */
    public static boolean isUrlValid(String urlString) {
        try {
            new URL(urlString);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * Returns the redirect URL if the given URL is being redirected, or the original URL if it's not.
     *
     * @param url URL to check
     * @return the redirect URL if the given URL is a redirect, or the original URL if it's not
     */
    public static String getRedirectUrl(String url) {
        HttpURLConnection connection = null;
        String realUrl = url;
        int maxRedirects = 10;

        try {
            while (maxRedirects > 0) {
                connection = (HttpURLConnection) new URL(realUrl).openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("HEAD");

                int responseCode = connection.getResponseCode();

                if (responseCode >= 300 && responseCode < 400) {
                    // if it's a redirect, get the new location and repeat the process
                    String newLocation = connection.getHeaderField("Location");
                    if (newLocation == null) {
                        log.warn("Redirect location not found in headers, will return the original URL");
                        return realUrl;
                    }
                    realUrl = newLocation;
                } else {
                    // if it's not a redirect or no more redirects, return the current URL
                    break;
                }

                maxRedirects--;
            }

            if (maxRedirects == 0) {
                log.warn("Too many redirects. Potential loop.");
            }

        } catch (IOException e) {
            log.error("An error occurred while looking up the redirect URL, will return the original URL.");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return realUrl;
    }

}
