package dev.dobicinaitis.feedreader.dto;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class SyncStatus {
    private ZonedDateTime publicationDateOfLastPostedArticle;
    private String titleOfLastPostedArticle;
}
