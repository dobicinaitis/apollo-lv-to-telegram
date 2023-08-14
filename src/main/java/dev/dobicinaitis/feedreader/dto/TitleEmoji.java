package dev.dobicinaitis.feedreader.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public enum TitleEmoji {
    VIDEO("\uD83C\uDFAC"), // 🎬
    PHOTO("\uD83D\uDCF7"), // 📷
    PHOTO_AND_VIDEO(" \uD83D\uDCF7\uD83C\uDFAC"), // 📷🎬
    NOTICE("❗"), //❗️
    ARTICLE("\uD83D\uDCF0"); // 📰

    private final String unicode;

    /**
     * Returns the corresponding {@link TitleEmoji} for a given title tag.
     *
     * @param tag the tag
     * @return the corresponding {@link TitleEmoji}
     */
    public static TitleEmoji fromTag(@NonNull String tag) {
        return switch (tag) {
            case "VIDEO" -> VIDEO;
            case "FOTO" -> PHOTO;
            case "FOTO UN VIDEO" -> PHOTO_AND_VIDEO;
            case "ŅEM VĒRĀ" -> NOTICE;
            default -> ARTICLE;
        };
    }
}
