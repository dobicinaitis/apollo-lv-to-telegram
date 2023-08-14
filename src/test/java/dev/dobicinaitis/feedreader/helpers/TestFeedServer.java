package dev.dobicinaitis.feedreader.helpers;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.Getter;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.micronaut.core.io.socket.SocketUtils.findAvailableTcpPort;

/**
 * This class is used to mock a feed server for use in tests.
 */
public class TestFeedServer {

    private static final String RSS_FILE = "test-rss.xml"; // location: src/test/resources/__files/
    private static final String URL_TEMPLATE = "http://localhost:%s/rss";

    private final WireMockServer server;

    @Getter
    private final String url;

    public TestFeedServer() {
        final int port = findAvailableTcpPort();
        this.server = new WireMockServer(port);
        this.url = String.format(URL_TEMPLATE, port);
        server.start();
        configureFor(port);
        stubFor(get(urlEqualTo("/rss")).willReturn(aResponse()
                .withStatus(200)
                .withBodyFile(RSS_FILE)
                .withHeader("Content-Type", "application/xml")
        ));
    }

    public void stop() {
        server.stop();
    }
}
