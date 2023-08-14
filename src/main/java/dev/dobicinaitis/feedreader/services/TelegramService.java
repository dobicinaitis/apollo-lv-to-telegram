package dev.dobicinaitis.feedreader.services;

import com.google.common.util.concurrent.RateLimiter;
import dev.dobicinaitis.feedreader.dto.Article;
import dev.dobicinaitis.feedreader.misc.LabelHolder;
import dev.dobicinaitis.feedreader.util.UrlUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
public class TelegramService extends TelegramLongPollingBot {

    private static final char[] SPECIAL_CHARACTERS = {'\\', '`', '*', '_', '{', '}', '[', ']', '<', '>', '(', ')', '#', '+', '-', '.', '!', '|'};
    private static final int MESSAGES_PER_MINUTE = 20;
    private static final RateLimiter rateLimiter = RateLimiter.create(MESSAGES_PER_MINUTE / 60.0);

    private final String channelId;

    public TelegramService(String token, String channelId) {
        super(token);
        this.channelId = channelId;
        log.debug("Started Telegram bot, Channel ID: {}", channelId);
    }

    @Override
    public void onUpdateReceived(Update update) {
        // At this point we don't care about updates.
    }

    @Override
    public String getBotUsername() {
        // Where is this actually used? Let's try not to set the bot name and see what brakes 🙈
        return null;
    }

    /**
     * Posts new articles from the RSS feed to Telegram.
     *
     * @param articles articles to post
     * @return last posted article
     */
    public Article postArticles(List<Article> articles) {
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
    private boolean postArticle(Article article) {
        final SendPhoto message = new SendPhoto();
        message.setChatId(channelId);
        message.setCaption(prepareCaption(article));
        message.setParseMode("MarkdownV2");
        message.setDisableNotification(true);
        message.setPhoto(prepareImage(article.getImageUrl()));
        message.setReplyMarkup(prepareKeyboard(article));

        try {
            execute(message);
            return true;
        } catch (TelegramApiException e) {
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
    private boolean postTextOnlyArticle(Article article) {
        final SendMessage message = new SendMessage();
        message.setChatId(channelId);
        message.setText(prepareCaption(article));
        message.setParseMode("MarkdownV2");
        message.setDisableNotification(true);
        message.setReplyMarkup(prepareKeyboard(article));

        try {
            execute(message);
            return true;
        } catch (TelegramApiException e) {
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
    private String prepareCaption(Article article) {
        return """
                %s *%s*

                %s
                """.formatted(
                article.getTitleEmoji().getUnicode(),
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
    private InlineKeyboardMarkup prepareKeyboard(Article article) {
        final InlineKeyboardButton readButton = new InlineKeyboardButton();
        readButton.setText(LabelHolder.getReadButtonLabel());
        readButton.setUrl(article.getLink());

        final InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(List.of(List.of(readButton)));

        return inlineKeyboardMarkup;
    }

    /**
     * Prepares an image file for the Telegram API.
     *
     * @param imageUrl URL of the image to prepare
     * @return InputFile
     */
    @SneakyThrows
    private InputFile prepareImage(String imageUrl) {
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
    public static String escapeSpecialCharacters(String text) {
        if (text == null) {
            return null;
        }
        final String regex = "([" + Pattern.quote(new String(SPECIAL_CHARACTERS)) + "])";
        return text.replaceAll(regex, "\\\\$1");
    }
}
