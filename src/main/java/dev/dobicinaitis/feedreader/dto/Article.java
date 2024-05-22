package dev.dobicinaitis.feedreader.dto;

import dev.dobicinaitis.feedreader.misc.ArticleComparator;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class Article implements Comparable<Article> {
    private String title;
    private TitleEmoji titleEmoji;
    private String description;
    private String link;
    private String imageUrl;
    private ZonedDateTime publicationDate;
    private boolean paywalled;

    @Override
    public int compareTo(Article other) {
        return ArticleComparator.oldestArticleFirst().compare(this, other);
    }
}
