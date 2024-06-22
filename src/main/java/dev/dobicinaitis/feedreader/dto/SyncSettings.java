package dev.dobicinaitis.feedreader.dto;

import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.util.List;

@Data
@Builder
public class SyncSettings {

    private String rssUrl;
    private String telegramBotToken;
    private String telegramChannelId;
    private File statusFile;
    private List<String> excludedCategories;
}
