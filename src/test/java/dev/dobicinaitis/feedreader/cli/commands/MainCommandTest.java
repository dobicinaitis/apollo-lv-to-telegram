package dev.dobicinaitis.feedreader.cli.commands;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import dev.dobicinaitis.feedreader.Application;
import dev.dobicinaitis.feedreader.helpers.TestMemoryAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.condition.OS.LINUX;
import static org.junit.jupiter.api.condition.OS.MAC;

class MainCommandTest {

    private CommandLine commandLine;
    private StringWriter commandOutput;
    private TestMemoryAppender testMemoryAppender;
    private static final String EXAMPLE_URL = "https://example.com/rss";

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

    @Test
    void shouldUseDefaultUrlWhenNoParameterIsProvided() {
        CommandLine.ParseResult parseResult = commandLine.parseArgs();
        assertEquals(EXAMPLE_URL, parseResult.matchedOptionValue("--url", EXAMPLE_URL),
                "Default URL should be used when no parameter is provided.");
    }

    @Test
    void shouldUseCustomUrlWhenParameterIsProvided() {
        CommandLine.ParseResult parseResult = commandLine.parseArgs("--url", EXAMPLE_URL);
        assertEquals(EXAMPLE_URL, parseResult.matchedOptionValue("--url", null),
                "Custom URL should be used when parameter is provided.");
    }

    @Test
    void shouldThrowAParameterExceptionWhenStatusFileIsNotAFile() throws IOException {
        final File tempDirectory = Files.createTempDirectory("tmpDirPrefix").toFile();
        tempDirectory.deleteOnExit();

        assertThrows(CommandLine.ParameterException.class, () -> {
            commandLine.parseArgs("--no-sync", "--status-file", tempDirectory.getAbsolutePath());
        }, "Should throw a ParameterException when status file is indeed a directory.");
    }

    @Test
    @EnabledOnOs({LINUX, MAC})
    void shouldThrowAParameterExceptionWhenStatusFileIsNotReadable() throws IOException {
        FileAttribute<Set<PosixFilePermission>> fileAttribute = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("-w--w--w-"));
        final File nonReadableFile = Files.createTempFile("tmpFile", ".tmp", fileAttribute).toFile();
        nonReadableFile.deleteOnExit();

        assertThrows(CommandLine.ParameterException.class, () -> {
            commandLine.parseArgs("--no-sync", "--status-file", nonReadableFile.getAbsolutePath());
        }, "Should throw a ParameterException when status file is not readable.");
    }

    @Test
    void shouldNotAttemptToExcludeAnyCategoriesWhenParameterIsNotProvided() {
        commandLine.execute("--debug", "--test-option=true");
        assertTrue(testMemoryAppender.contains("Will exclude articles in categories: [].", Level.DEBUG));
    }

    @Test
    void shouldAttemptToExcludeAllGivenCategories() {
        commandLine.execute("--debug", "--test-option=true", "--exclude-categories=sports,horoscopes");
        assertTrue(testMemoryAppender.contains("Will exclude articles in categories: [sports, horoscopes].", Level.DEBUG));
    }
}