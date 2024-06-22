package dev.dobicinaitis.feedreader.services;

import com.apptasticsoftware.rssreader.Item;
import dev.dobicinaitis.feedreader.dto.Article;
import dev.dobicinaitis.feedreader.dto.SyncSettings;
import dev.dobicinaitis.feedreader.dto.SyncStatus;
import dev.dobicinaitis.feedreader.helpers.TestFeedServer;
import dev.dobicinaitis.feedreader.util.ItemWrapper;
import dev.dobicinaitis.feedreader.util.JsonUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.dobicinaitis.feedreader.dto.TitleEmoji.*;
import static org.junit.jupiter.api.Assertions.*;

class SyncServiceTest {

    private static final TestFeedServer feedServer = new TestFeedServer();
    private SyncService syncService;

    @BeforeEach
    void setUp() {
        final SyncSettings syncSettings = SyncSettings.builder()
                .rssUrl(feedServer.getFeedUrl())
                .telegramBotToken("bot-token")
                .telegramChannelId("channel-id")
                .build();
        syncService = new SyncService(syncSettings);
    }

    @AfterAll
    static void afterAll() {
        feedServer.stop();
    }

    @Test
    void shouldRemoveTagsFromTitle() {
        // given
        final String title = "VIDEO ⟩ Luffy Gear 10 Revealed! One Piece Chapter 9000 Breakdown";
        final String expected = "Luffy Gear 10 Revealed! One Piece Chapter 9000 Breakdown";
        // when
        final String sanitizeTitle = syncService.sanitizeTitle(title);
        // then
        assertEquals(expected, sanitizeTitle, "Tags should be removed from title.");
    }

    @Test
    void shouldPickAnEmojiBasedOnTitle() {
        assertEquals(ARTICLE, syncService.pickEmoji(null), "Article emoji should be picked when title is null.");
        assertEquals(ARTICLE, syncService.pickEmoji("Alien artifacts found on mars!"),
                "Article emoji should be picked when no title tags are not used.");
        assertEquals(ARTICLE, syncService.pickEmoji("SOMETHING_NEW_AND_UNKNOWN ⟩ Time travel experiment succeeds!"),
                "Article emoji should be picked when title tags are not recognized.");
        assertEquals(VIDEO, syncService.pickEmoji("VIDEO ⟩ Invisible cloak invented by scientists!"),
                "Video emoji should be picked.");
        assertEquals(PHOTO, syncService.pickEmoji("FOTO ⟩ Robots develop their own language!"),
                "Photo emoji should be picked.");
        assertEquals(PHOTO_AND_VIDEO, syncService.pickEmoji("FOTO UN VIDEO ⟩ Mysterious crop circles baffle experts!"),
                "Photo and video emoji should be picked.");
        assertEquals(NOTICE, syncService.pickEmoji("ŅEM VĒRĀ ⟩ Mysterious crop circles baffle experts!"),
                "Notice emoji should be picked.");
    }

    @Test
    void shouldConvertRssItemsToArticles() {
        // given
        final List<Item> items = List.of(
                ItemWrapper.builder()
                        .title("Scientists discover a parallel universe!")
                        .link("https://www.example.com")
                        .build()
                        .toRssItem(),
                ItemWrapper.builder()
                        .title("First ever alien selfie")
                        .enclosureUrl("https://www.example.com/image.jpg")
                        .enclosureType("image/jpeg")
                        .build()
                        .toRssItem(),
                ItemWrapper.builder().build().toRssItem()
        );
        // when
        final List<Article> articles = syncService.convertRssItemsToArticles(items);
        // then
        assertEquals(2, articles.size(), "Only 2 articles should be returned.");
        assertEquals("Scientists discover a parallel universe!", articles.get(0).getTitle(),
                "Title should mach the RSS feed item.");
        assertEquals("https://www.example.com", articles.get(0).getLink(),
                "Link should mach the RSS feed item.");
        assertEquals("https://www.example.com/image.jpg", articles.get(1).getImageUrl(),
                "Image URL should mach the RSS feed item.");
    }

    @Test
    void shouldCreateANewSyncObjectIfStatusFileDoesNotExist() {
        // given
        final File nonExistentStatusFile = new File("non-existent-status-file");
        syncService.setStatusFile(nonExistentStatusFile);
        // when
        final SyncStatus syncStatus = syncService.readSyncStatusFromFile();
        // then
        assertNull(syncStatus.getTitleOfLastPostedArticle(), "Title should be null.");
        assertNull(syncStatus.getPublicationDateOfLastPostedArticle(), "Publication date should be null.");
    }

    @Test
    void shouldReadSyncStatusFromFile() throws Exception {
        // given
        final FeedReaderService feedReader = new FeedReaderService(feedServer.getFeedUrl());
        final List<Item> testFeedArticles = feedReader.getItems();
        final String title = testFeedArticles.get(0).getTitle().orElseThrow();
        final ZonedDateTime publicationDate = testFeedArticles.get(0).getPubDateZonedDateTime().orElseThrow();
        final File statusFile = prepareStatusFile(title, publicationDate);
        syncService.setStatusFile(statusFile);
        // when
        final SyncStatus syncStatus = syncService.readSyncStatusFromFile();
        // then
        assertEquals(title, syncStatus.getTitleOfLastPostedArticle(), "Title should match the RSS feed item.");
        assertEquals(publicationDate, syncStatus.getPublicationDateOfLastPostedArticle(), "Publication date should match the RSS feed item.");
    }

    @Test
    void shouldReturnANewSyncStatusObjectIfFileIsEmpty() throws Exception {
        // given
        final File emptyStatusFile = File.createTempFile("emptyStatusFile", ".json");
        emptyStatusFile.deleteOnExit();
        syncService.setStatusFile(emptyStatusFile);
        // when
        final SyncStatus syncStatus = syncService.readSyncStatusFromFile();
        // then
        assertNotNull(syncStatus, "Sync status should not be null.");
        assertNull(syncStatus.getTitleOfLastPostedArticle(), "Title should be null.");
        assertNull(syncStatus.getPublicationDateOfLastPostedArticle(), "Publication date should be null.");
    }

    @Test
    void shouldRemoveProcessedArticles() {
        // given
        final ZonedDateTime cutOffDate = ZonedDateTime.now();
        final List<Article> articles = new ArrayList<>();
        articles.add(Article.builder().title("new").publicationDate(ZonedDateTime.now().plusMinutes(1)).build());
        articles.add(Article.builder().title("last").publicationDate(cutOffDate).build());
        articles.add(Article.builder().title("old").publicationDate(ZonedDateTime.now().minusMinutes(1)).build());
        final SyncStatus syncStatus = SyncStatus.builder().publicationDateOfLastPostedArticle(cutOffDate).build();
        // when
        syncService.removeProcessedArticles(articles, syncStatus);
        // then
        assertEquals(1, articles.size(), "Only 1 article should be left.");
        assertEquals("new", articles.get(0).getTitle(), "The newest article should be left.");
    }

    @Test
    void shouldCheckForPresenceOfAPaywallLabel() {
        // given
        final String paywalledArticleUrl = feedServer.getBaseUrl() + "/" + TestFeedServer.PAYWALLED_ARTICLE_ENDPOINT;
        final String freeArticleUrl = feedServer.getBaseUrl() + "/" + TestFeedServer.FREE_ARTICLE_ENDPOINT;
        // when, then
        assertTrue(syncService.hasPaywallLabel(paywalledArticleUrl), "Paywalled article should have a paywall label.");
        assertFalse(syncService.hasPaywallLabel(freeArticleUrl), "Free article should not have a paywall label.");
    }

    @Test
    void shouldExcludeUnwantedCategories() {
        // given
        final List<Item> rssItems = new ArrayList<>(Arrays.asList(
                ItemWrapper.builder().title("interesting 1").categories(List.of("news", "technology")).build().toRssItem(),
                ItemWrapper.builder().title("interesting 2").categories(List.of("news", "science")).build().toRssItem(),
                ItemWrapper.builder().title("boring 1").categories(List.of("news", "gossip")).build().toRssItem(),
                ItemWrapper.builder().title("boring 2").categories(List.of("news", "technology", "pascal")).build().toRssItem()
        ));
        final SyncSettings syncSettings = SyncSettings.builder()
                .excludedCategories(List.of("gossip", "pascal"))
                .build();
        syncService = new SyncService(syncSettings);
        // when
        syncService.removeExcludedCategories(rssItems);
        // then
        assertEquals(2, rssItems.size(), "Only 2 articles should be left.");
        assertEquals("interesting 1", rssItems.get(0).getTitle().orElse(""), "The first article should be left.");
        assertEquals("interesting 2", rssItems.get(1).getTitle().orElse(""), "The second article should be left.");
    }

    /**
     * Prepares a temporary status file with the given title and publication date.
     *
     * @param title           the title of the last posted article
     * @param publicationDate the publication date of the last posted article
     * @return the prepared status file
     * @throws Exception if an error occurs
     */
    private File prepareStatusFile(String title, ZonedDateTime publicationDate) throws Exception {
        final SyncStatus syncStatus = SyncStatus.builder()
                .titleOfLastPostedArticle(title)
                .publicationDateOfLastPostedArticle(publicationDate)
                .build();
        final String jsonString = JsonUtils.getGson().toJson(syncStatus);
        final File statusFile = File.createTempFile("tmpStatus", ".json");
        statusFile.deleteOnExit();
        Files.write(statusFile.toPath(), jsonString.getBytes());
        return statusFile;
    }

}