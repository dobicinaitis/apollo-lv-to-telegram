package dev.dobicinaitis.feedreader.cli.commands;

import dev.dobicinaitis.feedreader.cli.options.CommonOptions;
import dev.dobicinaitis.feedreader.cli.providers.VersionProvider;
import dev.dobicinaitis.feedreader.misc.LabelHolder;
import dev.dobicinaitis.feedreader.services.SyncService;
import dev.dobicinaitis.feedreader.util.UrlUtils;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

import java.io.File;

@Slf4j
@Command(sortSynopsis = false)
public class MainCommand extends CommonOptions implements Runnable {

    private String url;
    private File statusFile;

    @Spec
    CommandSpec spec;

    @Option(names = {"-u", "--url"}, paramLabel = "URL", defaultValue = "${FEED_READER_URL:-https://www.apollo.lv/rss}",
            description = "News feed RSS URL (default: ${DEFAULT-VALUE}).", order = 1)
    private void setUrl(final String url) {
        if (!UrlUtils.isUrlValid(url)) {
            throw new ParameterException(spec.commandLine(), String.format("Invalid news feed URL: '%s'.", url));
        }
        this.url = url;
    }

    @Option(names = {"-t", "--token"}, paramLabel = "TOKEN", required = true, defaultValue = "${FEED_READER_TOKEN}",
            description = "Telegram bot token.", order = 2)
    private String botToken;

    @Option(names = {"-c", "--channel-id"}, paramLabel = "CHANNEL_ID", required = true, defaultValue = "${FEED_READER_CHANNEL_ID}",
            description = "Telegram Channel ID.", order = 3)
    private String channelId;

    @Option(names = {"-s", "--status-file"}, paramLabel = "FILE", defaultValue = "${FEED_READER_STATUS_FILE}",
            description = "File to store information about the last processed article.%n" +
                    "Used to prevent posting duplicates on repeated runs.", order = 4)
    private void setStatusFile(final File inputFile) {
        // this could be a new file, so validate it only if the file exists
        if (inputFile != null && inputFile.exists()) {
            validateStatusFile(inputFile);
        }
        this.statusFile = inputFile;
    }

    @Option(names = {"-r", "--read-button-label"}, paramLabel = "LABEL", defaultValue = "${FEED_READER_READ_BUTTON_LABEL:-Read}",
            description = "Label for the \"Read\" button in Telegram.", order = 5)
    private void setReadButtonLabel(final String label) {
        LabelHolder.setReadButtonLabel(label);
    }

    @Option(names = "--no-sync", hidden = true, defaultValue = "${FEED_NO_SYNC:-false}",
            description = "A hidden parameter used to ease testing.")
    private boolean syncDisabled;


    public void run() {
        log.debug("Application version {}, build {}", VersionProvider.getVersionNumber(), VersionProvider.getBuildNumber());

        if (syncDisabled || isOptionTest()) {
            log.info("Sync has been disabled. Won't do anything.");
            return;
        }

        log.info("Starting feed sync, RSS URL: {}", url);
        final SyncService syncService = new SyncService(url, botToken, channelId);
        syncService.setStatusFile(statusFile);
        syncService.sync();
    }

    /**
     * Validates the status file.
     *
     * @param inputFile the status file
     * @throws ParameterException if the status file is a directory or is not readable
     */
    private void validateStatusFile(File inputFile) {
        if (inputFile.isDirectory()) {
            throw new ParameterException(spec.commandLine(),
                    String.format("'%s' is a directory.", inputFile.getAbsolutePath()));
        }
        if (!inputFile.canRead()) {
            throw new ParameterException(spec.commandLine(),
                    String.format("Status file '%s' is not readable.", inputFile.getAbsolutePath()));
        }
    }
}
