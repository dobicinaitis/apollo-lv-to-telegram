package dev.dobicinaitis.feedreader.services;

import com.google.common.util.concurrent.RateLimiter;
import dev.dobicinaitis.feedreader.dto.Article;
import dev.dobicinaitis.feedreader.dto.TitleEmoji;
import dev.dobicinaitis.feedreader.misc.LabelHolder;
import dev.dobicinaitis.feedreader.util.UrlUtils;
import dev.failsafe.Failsafe;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static dev.dobicinaitis.feedreader.configuration.FailsafeConfiguration.RETRY_POLICY;

@Slf4j
public class TelegramService {

    private static final char[] SPECIAL_CHARACTERS = {'\\', '`', '*', '_', '{', '}', '[', ']', '<', '>', '(', ')', '#', '+', '-', '.', '!', '|'};
    private static final int MESSAGES_PER_MINUTE = 20;
    @SuppressWarnings("UnstableApiUsage")
    private static final RateLimiter rateLimiter = RateLimiter.create(MESSAGES_PER_MINUTE / 60.0);

    private final TelegramClient client;
    private final String channelId;

    public TelegramService(String token, String channelId) {
        this.client = new OkHttpTelegramClient(token);
        this.channelId = channelId;
        log.debug("Started Telegram bot, Channel ID: {}", channelId);
    }

    /**
     * Posts new articles from the RSS feed to Telegram.
     *
     * @param articles articles to post
     * @return last posted article
     */
    @SuppressWarnings("UnstableApiUsage")
    public Article postArticles(final List<Article> articles) {
        Article lastPostedArticle = null;
        int articlesPosted = 0;
        for (Article article : articles) {
            // acquire a permit from the rate limiter before sending a new message
            rateLimiter.acquire();

            // do some sanity checks
            if (article == null || article.getTitle().isEmpty() || article.getLink().isEmpty()) {
                log.warn("Article is missing the title or link. Skipping.");
                continue;
            }

            log.debug("Posting article: {}", article);
            boolean wasPosted;

            // decide whether to post a text-only article or an article with an image
            if (UrlUtils.isUrlValid(article.getImageUrl())) {
                wasPosted = postArticle(article);
            } else {
                wasPosted = postTextOnlyArticle(article);
            }

            if (wasPosted) {
                log.debug("Article posted successfully.");
                lastPostedArticle = article;
                articlesPosted++;
            }
        }
        log.info("Successfully posted {} out of {} articles.", articlesPosted, articles.size());
        return lastPostedArticle;
    }

    /**
     * Posts a single article with an image to Telegram.
     *
     * @param article article to post
     * @return true if the article was posted successfully, false otherwise
     */
    private boolean postArticle(final Article article) {
        final SendPhoto message;
        try {
            final InputFile imageFile = Failsafe.with(RETRY_POLICY)
                    .get(() -> prepareImage(article.getImageUrl())); // obfuscate an IOException
            message = SendPhoto.builder()
                    .chatId(channelId)
                    .photo(imageFile)
                    .caption(prepareCaption(article))
                    .parseMode("MarkdownV2")
                    .disableNotification(true)
                    .replyMarkup(prepareKeyboard(article))
                    .build();
        } catch (Exception e) {
            log.info("Failed to prepare the image file, will try to post a text-only article instead.");
            return postTextOnlyArticle(article);
        }

        try {
            Failsafe.with(RETRY_POLICY).run(() -> client.execute(message)); // obfuscate a TelegramApiException
            return true;
        } catch (Exception e) {
            log.error("Failed to send message: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Posts a single text-based article to Telegram.
     *
     * @param article article to post
     * @return true if the article was posted successfully, false otherwise
     */
    private boolean postTextOnlyArticle(final Article article) {
        final SendMessage message = SendMessage.builder()
                .chatId(channelId)
                .text(prepareCaption(article))
                .parseMode("MarkdownV2")
                .disableNotification(true)
                .replyMarkup(prepareKeyboard(article))
                .build();

        try {
            Failsafe.with(RETRY_POLICY).run(() -> client.execute(message)); // obfuscate a TelegramApiException
            return true;
        } catch (Exception e) {
            log.error("Failed to send message: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Prepares a caption for a Telegram message.
     *
     * @param article to prepare the caption for
     * @return caption
     */
    private String prepareCaption(final Article article) {
        final String paywallEmoji = article.isPaywalled() ? TitleEmoji.PAYWALL.getUnicode() : "";
        return """
                %s *%s*

                %s
                """.formatted(
                paywallEmoji + article.getTitleEmoji().getUnicode(),
                escapeSpecialCharacters(article.getTitle()),
                escapeSpecialCharacters(article.getDescription())
        );
    }

    /**
     * Prepares an inline keyboard with a button for opening the article.
     *
     * @param article to prepare the keyboard for
     * @return keyboard
     */
    private InlineKeyboardMarkup prepareKeyboard(final Article article) {
        final InlineKeyboardButton readButton = InlineKeyboardButton.builder()
                .text(LabelHolder.getReadButtonLabel())
                .url(article.getLink())
                .build();
        final InlineKeyboardRow buttonRow = new InlineKeyboardRow(readButton);
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(buttonRow))
                .build();
    }

    /**
     * Prepares an image file for the Telegram API.
     *
     * @param imageUrl URL of the image to prepare
     * @return InputFile
     * @throws IOException if the image could not be loaded
     */
    protected static InputFile prepareImage(final String imageUrl) throws IOException {
        final URL url = new URL(imageUrl);
        final InputStream imageStream = url.openStream();
        final String randomFilename = UUID.randomUUID() + "." + FilenameUtils.getExtension(url.getPath());
        return new InputFile(imageStream, randomFilename);
    }

    /**
     * Escapes special characters in a String for it to be usable in a Telegram markdown message.
     *
     * @param text String to escape
     * @return escaped String
     */
    protected static String escapeSpecialCharacters(final String text) {
        if (text == null) {
            return null;
        }
        final String regex = "([" + Pattern.quote(new String(SPECIAL_CHARACTERS)) + "])";
        return text.replaceAll(regex, "\\\\$1");
    }
}
