package dev.dobicinaitis.feedreader.cli.options;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import dev.dobicinaitis.feedreader.Application;
import dev.dobicinaitis.feedreader.cli.providers.HelpProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Option;

@Slf4j
@Getter
public class CommonOptions extends HelpProvider {

    private boolean optionTest;

    @Option(names = {"-d", "--debug"}, defaultValue = "${FEED_READER_DEBUG:-false}",
            description = "Print debug information.")
    public void setDebug(boolean debug) {
        if (debug) {
            final Logger rootLogger = (Logger) LoggerFactory.getLogger(Application.class.getPackageName());
            rootLogger.setLevel(Level.DEBUG);
        }
    }

    /**
     * Test option used to verify the verbosity flag and usage of environment variables.
     */
    @Option(names = "--test-option", hidden = true, defaultValue = "${FEED_READER_TEST_OPTION:-false}")
    private void setOptionTest(final boolean enableOptionTest) {
        if (enableOptionTest) {
            log.debug("Debug log level was enabled by --debug option.");
            log.info("--test-option was set to true via the FEED_READER_TEST_OPTION environment variable.");
            this.optionTest = true;
        }
    }
}
