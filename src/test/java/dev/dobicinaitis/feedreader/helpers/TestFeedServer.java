package dev.dobicinaitis.feedreader.helpers;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.Getter;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.micronaut.core.io.socket.SocketUtils.findAvailableTcpPort;

/**
 * This class is used to mock a feed server for use in tests.
 */
public class TestFeedServer {

    private static final String BASE_URL_TEMPLATE = "http://localhost:%s";
    private static final String FEED_ENDPOINT = "/rss";
    // test file location: src/test/resources/__files/
    private static final String RSS_FILE = "test-rss.xml";
    public static final String IMAGE_FILE_RC_200 = "image.gif";
    public static final String IMAGE_FILE_RC_503 = "unavailable.jpg"; // does not exist
    public static final String PAYWALLED_ARTICLE_ENDPOINT = "paywalled-article";
    public static final String FREE_ARTICLE_ENDPOINT = "free-article";

    private final WireMockServer server;

    @Getter
    private final String baseUrl;

    @Getter
    private final String feedUrl;

    public TestFeedServer() {
        final int port = findAvailableTcpPort();
        this.server = new WireMockServer(port);
        this.baseUrl = String.format(BASE_URL_TEMPLATE, port);
        this.feedUrl = this.baseUrl + FEED_ENDPOINT;
        server.start();
        configureFor(port);
        stubFor(get(urlEqualTo(FEED_ENDPOINT)).willReturn(aResponse()
                .withStatus(200)
                .withBodyFile(RSS_FILE)
                .withHeader("Content-Type", "application/xml")
        ));
        stubFor(get(urlEqualTo("/" + IMAGE_FILE_RC_200)).willReturn(aResponse()
                .withStatus(200)
                .withBodyFile(IMAGE_FILE_RC_200)
                .withHeader("Content-Type", "image/gif")
        ));
        stubFor(get(urlEqualTo("/" + IMAGE_FILE_RC_503)).willReturn(aResponse()
                .withStatus(503)
                .withBody("Service Unavailable")
        ));
        stubFor(get(urlEqualTo("/" + PAYWALLED_ARTICLE_ENDPOINT)).willReturn(aResponse()
                .withStatus(200)
                .withBodyFile("article-with-paywall-label.html")
                .withHeader("Content-Type", "text/html")
        ));
        stubFor(get(urlEqualTo("/" + FREE_ARTICLE_ENDPOINT)).willReturn(aResponse()
                .withStatus(200)
                .withBodyFile("article-without-paywall-label.html")
                .withHeader("Content-Type", "text/html")
        ));
    }

    public void stop() {
        server.stop();
    }
}
