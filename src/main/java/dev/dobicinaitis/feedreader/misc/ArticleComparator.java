package dev.dobicinaitis.feedreader.misc;

import dev.dobicinaitis.feedreader.dto.Article;

import java.time.Instant;
import java.util.Comparator;

/**
 * Comparator for sorting Article objects.
 */
public final class ArticleComparator {

    private ArticleComparator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Comparator for sorting Articles on publication date in ascending order (oldest first)
     * @param <I> any class that extend Article
     * @return comparator
     */
    public static <I extends Article> Comparator<I> oldestArticleFirst() {
        return Comparator.comparing((I i) -> i == null ? Instant.EPOCH : i.getPublicationDate().toInstant());
    }

    /**
     * Comparator for sorting Articles on publication date in descending order (newest first)
     * @param <I> any class that extend Article
     * @return comparator
     */
    public static <I extends Article> Comparator<I> newestArticleFirst() {
        return Comparator.comparing((I i) -> i == null ? Instant.EPOCH : i.getPublicationDate().toInstant()).reversed();
    }
}