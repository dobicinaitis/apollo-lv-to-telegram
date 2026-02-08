package dev.dobicinaitis.feedreader.services;

import com.apptasticsoftware.rssreader.Enclosure;
import com.apptasticsoftware.rssreader.Item;
import dev.dobicinaitis.feedreader.dto.Article;
import dev.dobicinaitis.feedreader.dto.SyncSettings;
import dev.dobicinaitis.feedreader.dto.SyncStatus;
import dev.dobicinaitis.feedreader.dto.TitleEmoji;
import dev.dobicinaitis.feedreader.exceptions.FeedReaderRuntimeException;
import dev.dobicinaitis.feedreader.misc.ArticleComparator;
import dev.dobicinaitis.feedreader.util.JsonUtils;
import dev.dobicinaitis.feedreader.util.UrlUtils;
import io.micronaut.core.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SyncService {

    public static final String TITLE_TAG_SEPARATOR = "⟩";
    public static final String PAYWALL_CSS_SELECTOR = "li:containsOwn(ABONENTIEM), li:contains(ABONENTIEM)";

    private final TelegramService telegram;
    private final FeedReaderService feedReader;
    private final SyncSettings settings;

    public SyncService(final SyncSettings settings) {
        this.settings = settings;
        this.telegram = new TelegramService(settings.getTelegramBotToken(), settings.getTelegramChannelId());
        this.feedReader = new FeedReaderService(settings.getRssUrl());
    }

    /**
     * Posts new articles from the RSS feed to Telegram.
     */
    public void sync() {
        log.info("Starting sync.");
        SyncStatus syncStatus = null;

        log.info("Loading RSS feed items.");
        final List<Item> items = new ArrayList<>(feedReader.getItems());
        log.info("Received {} items.", items.size());
        removeExcludedCategories(items);
        final List<Article> articles = convertRssItemsToArticles(items);

        if (isStatusFileUsed()) {
            log.info("Status file is used. Will remove previously processed items.");
            syncStatus = readSyncStatusFromFile();
            log.debug("Last sync status data: {}", syncStatus);
            removeProcessedArticles(articles, syncStatus);
        }

        if (articles.isEmpty()) {
            log.info("No new articles to post.");
            return;
        }

        // The short links from the RSS feed point to a different domain name.
        // As these will be visible in the Telegram channel, we want to replace them with
        // the more trustworthy post-redirect links that use the actual news site domain.
        log.info("Replacing shortened links with post-redirect ones.");
        articles.parallelStream().forEach(article -> article.setLink(UrlUtils.getRedirectUrl(article.getLink())));

        // Check if any of the new articles are subscription-only and set the paywalled flag accordingly.
        log.info("Updating paywall flags.");
        articles.parallelStream().forEach(article -> article.setPaywalled(hasPaywallLabel(article.getLink())));

        if (settings.isExcludePaywalled()) {
            removePaywalledArticles(articles);
        }

        log.info("Posting {} new articles to Telegram.", articles.size());
        final Article lastPostedArticle = telegram.postArticles(articles);

        if (isStatusFileUsed() && lastPostedArticle != null) {
            log.info("Saving sync status to file.");
            if (lastPostedArticle.getPublicationDate() == null) {
                log.warn("Something is off. The last posted article is missing a publication date. Will use the current system time instead.");
                lastPostedArticle.setPublicationDate(ZonedDateTime.now());
            }
            assert syncStatus != null;
            syncStatus.setPublicationDateOfLastPostedArticle(lastPostedArticle.getPublicationDate());
            syncStatus.setTitleOfLastPostedArticle(lastPostedArticle.getTitle());
            writeSyncStatusToFile(syncStatus);
        }
        log.info("Sync finished.");
    }

    private boolean isStatusFileUsed() {
        return settings.getStatusFile() != null;
    }

    /**
     * Converts the RSS feed items to Article objects.
     *
     * @param items RSS feed items
     * @return list of Article objects
     */
    protected List<Article> convertRssItemsToArticles(List<Item> items) {
        log.debug("Converting RSS items to Article objects.");
        final List<Article> articles = new ArrayList<>();
        for (Item item : items) {
            if (item.getTitle().isPresent()) {
                final String title = item.getTitle().get();
                final String sanitizedTitle = sanitizeTitle(title);
                final TitleEmoji titleEmoji = pickEmoji(title);
                final String description = item.getDescription().orElse("");
                final String link = item.getLink().orElse("");
                final ZonedDateTime publicationDate = item.getPubDateZonedDateTime().orElse(null);
                String imageUrl = "";
                if (item.getEnclosure().isPresent() && item.getEnclosure().get().getUrl() != null) {
                    imageUrl = item.getEnclosure().stream()
                            .filter(enclosure -> enclosure.getType().contains("image"))
                            .findFirst()
                            .map(Enclosure::getUrl)
                            .orElse("");
                }

                final Article article = Article.builder()
                        .title(sanitizedTitle)
                        .titleEmoji(titleEmoji)
                        .description(description)
                        .link(link)
                        .imageUrl(imageUrl)
                        .publicationDate(publicationDate)
                        .build();

                log.debug("Converted RSS item to Article object: {}", article);
                articles.add(article);
            }
        }
        log.info("Converted {} RSS items to {} article objects.", items.size(), articles.size());
        return articles;
    }

    /**
     * Reads the last sync status from a file.
     *
     * @return last sync status object
     */
    protected SyncStatus readSyncStatusFromFile() {
        if (!settings.getStatusFile().exists()) {
            log.info("Status file {} does not exist, will create a new file.", settings.getStatusFile().getAbsolutePath());
            return SyncStatus.builder().build();
        }

        try (final Reader reader = Files.newBufferedReader(settings.getStatusFile().toPath())) {
            SyncStatus syncStatus = JsonUtils.getGson().fromJson(reader, SyncStatus.class);
            if (syncStatus == null) {
                log.warn("Status file {} was empty.", settings.getStatusFile().getAbsolutePath());
                syncStatus = SyncStatus.builder().build();
            }
            return syncStatus;
        } catch (IOException e) {
            log.error("Failed to read status from file {}", settings.getStatusFile().getAbsolutePath(), e);
            throw new FeedReaderRuntimeException(e);
        }
    }

    /**
     * Removes articles already posted to Telegram from the latest RSS item list.
     *
     * @param articles   articles to be filtered
     * @param syncStatus last sync status containing the date of the latest processed article
     */
    protected void removeProcessedArticles(List<Article> articles, SyncStatus syncStatus) {
        if (syncStatus == null || syncStatus.getPublicationDateOfLastPostedArticle() == null) {
            log.warn("Last sync status lacks values. Will not remove any articles.");
            return;
        }
        final int initialSize = articles.size();
        articles.sort(ArticleComparator.oldestArticleFirst());
        articles.removeIf(article -> !article.getPublicationDate().isAfter(syncStatus.getPublicationDateOfLastPostedArticle()));
        log.debug("Remaining articles: {}", articles);
        final int removedCount = initialSize - articles.size();
        log.info("Removed {} old article{}, {} remaining.", removedCount, removedCount == 1 ? "" : "s", articles.size());
    }

    /**
     * Removes RSS items that belong to excluded article categories.
     *
     * @param items RSS items to be filtered
     */
    protected void removeExcludedCategories(List<Item> items) {
        if (CollectionUtils.isNotEmpty(settings.getExcludedCategories())) {
            log.info("Excluding items in unwanted/boring categories.");
            final List<String> excludedCategoriesLower = convertToLowerCase(settings.getExcludedCategories());
            items.removeIf(item -> {
                final List<String> itemCategoriesLower = convertToLowerCase(item.getCategories());
                final List<String> interestedCategories = new ArrayList<>(itemCategoriesLower);
                interestedCategories.retainAll(excludedCategoriesLower);
                return !interestedCategories.isEmpty();
            });
        }
    }

    /**
     * Removes paywalled articles from the list.
     *
     * @param articles excluding paywalled ones
     */
    protected void removePaywalledArticles(List<Article> articles) {
        final int initialSize = articles.size();
        articles.removeIf(Article::isPaywalled);
        final int removedCount = initialSize - articles.size();
        log.info("Removed {} paywalled article{}, {} remaining.", removedCount, removedCount == 1 ? "" : "s", articles.size());
    }

    /**
     * Converts a list of strings to lowercase.
     *
     * @param list list of strings
     * @return list of strings in lowercase
     */
    private List<String> convertToLowerCase(List<String> list) {
        return list.stream()
                .map(String::toLowerCase)
                .toList();
    }

    /**
     * Removes the news site tag from the article title.
     *
     * @param title title with tag
     * @return title without a tag
     */
    protected String sanitizeTitle(String title) {
        if (title == null || !title.contains(TITLE_TAG_SEPARATOR)) {
            return title;
        }
        return title.split(TITLE_TAG_SEPARATOR)[1].trim();
    }

    /**
     * Picks an emoji for the article title.
     *
     * @param title title with tag
     * @return emoji
     */
    protected TitleEmoji pickEmoji(String title) {
        if (title == null || !title.contains(TITLE_TAG_SEPARATOR)) {
            return TitleEmoji.ARTICLE;
        }
        final String tag = title.split(TITLE_TAG_SEPARATOR)[0].trim();
        return TitleEmoji.fromTag(tag);
    }

    /**
     * Writes the last sync status to a file.
     *
     * @param syncStatus last sync status object
     */
    private void writeSyncStatusToFile(SyncStatus syncStatus) {
        log.debug("Writing sync status to file: {}", syncStatus);
        try {
            final FileWriter fileWriter = new FileWriter(settings.getStatusFile());
            JsonUtils.getGson().toJson(syncStatus, fileWriter);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            log.error("Failed to write sync status to file {}", settings.getStatusFile().getAbsolutePath(), e);
            throw new FeedReaderRuntimeException(e);
        }
    }

    /**
     * Parses the HTML source code of a URL to check for the presence of a paywall label.
     *
     * @param url article URL
     * @return true if the article is paywalled, false otherwise
     */
    protected boolean hasPaywallLabel(final String url) {
        if (url != null) {
            try {
                final Document htmlDocument = Jsoup.connect(url).get();
                final Element paywallLabelElement = htmlDocument.select(PAYWALL_CSS_SELECTOR).first();
                if (paywallLabelElement != null) {
                    log.debug("Article is paywalled: {}", url);
                    return true;
                }
            } catch (IOException e) {
                log.error("Failed to connect to URL: {}", url, e);
                return false;
            }
        }
        return false;
    }

    // For testing purposes
    protected void setStatusFile(final File file) {
        this.settings.setStatusFile(file);
    }
}
