package dev.dobicinaitis.feedreader.cli.options;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import dev.dobicinaitis.feedreader.Application;
import dev.dobicinaitis.feedreader.cli.commands.MainCommand;
import dev.dobicinaitis.feedreader.helpers.TestMemoryAppender;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class CommonOptionsTest {

    private CommandLine commandLine;
    private StringWriter commandOutput;
    private TestMemoryAppender testMemoryAppender;

    @BeforeEach
    void setUp() {
        commandLine = new CommandLine(new MainCommand());
        commandOutput = new StringWriter();
        commandLine.setOut(new PrintWriter(commandOutput));
        // capture log messages
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Application.class.getPackageName());
        testMemoryAppender = new TestMemoryAppender();
        testMemoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        rootLogger.addAppender(testMemoryAppender);
        testMemoryAppender.start();
        // set required options
        System.setProperty("FEED_READER_TOKEN", "bot-token");
        System.setProperty("FEED_READER_CHANNEL_ID", "1234");
    }

    @AfterEach
    void tearDown() {
        // unset required options
        System.clearProperty("FEED_READER_TOKEN");
        System.clearProperty("FEED_READER_CHANNEL_ID");
    }

    private void resetOutputs() {
        commandOutput.getBuffer().setLength(0);
        testMemoryAppender.reset();
    }

    @Test
    void shouldReturnDebugLevelLogMessagesWhenDebugOptionIsUsed() {
        final String expectedMessage = "Debug log level was enabled by --debug option.";
        commandLine.execute("--debug", "--test-option=true");
        assertTrue(testMemoryAppender.contains(expectedMessage, Level.DEBUG), "Expected debug log message not found in logs.");
        resetOutputs();
        commandLine.execute("-d", "--test-option=true");
        assertTrue(testMemoryAppender.contains(expectedMessage, Level.DEBUG), "Expected info log message not found in logs.");
    }

    @Test
    void shouldReturnApplicationVersionWhenVersionOptionIsUsed() {
        commandLine.execute("--version");
        assertTrue(commandOutput.toString().contains("version"), "Expected version string not found in output.");
        resetOutputs();
        commandLine.execute("-V");
        assertTrue(commandOutput.toString().contains("version"), "Expected version string not found in output.");
    }

    @Test
    void shouldReturnHelpMessageWhenHelpOptionIsUsed() {
        commandLine.execute("--help");
        assertTrue(commandOutput.toString().contains("USAGE"), "Expected help usage string not found in output.");
        resetOutputs();
        commandLine.execute("-h");
        assertTrue(commandOutput.toString().contains("USAGE"), "Expected help usage string not found in output.");
    }

    @Test
    @SetEnvironmentVariable(key = "FEED_READER_TEST_OPTION", value = "some-value")
    void shouldUseEnvironmentVariablesSetByTests() {
        assertEquals("some-value", System.getenv("FEED_READER_TEST_OPTION"), "Expected environment variable not found.");
    }

    @Test
    @SetEnvironmentVariable(key = "FEED_READER_TEST_OPTION", value = "true")
    void shouldUseEnvironmentVariableAsDefaultOptionValue() {
        commandLine.execute("-d");
        assertTrue(testMemoryAppender.contains("--test-option was set to true via the FEED_READER_TEST_OPTION environment variable.", Level.INFO),
                "Expected info log message not found in logs.");
    }
}