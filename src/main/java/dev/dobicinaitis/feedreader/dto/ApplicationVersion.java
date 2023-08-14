package dev.dobicinaitis.feedreader.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplicationVersion {
    private String version;
    private String build;
}
