package dev.dobicinaitis.feedreader;

import dev.dobicinaitis.feedreader.cli.commands.MainCommand;
import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.CommandLinePropertySource;
import io.micronaut.context.env.Environment;
import picocli.CommandLine;

public class Application {
    public static void main(String[] args) {
        ApplicationContext ctx = ApplicationContext.builder(MainCommand.class, Environment.CLI)
                .propertySources(new CommandLinePropertySource(io.micronaut.core.cli.CommandLine.parse(args)))
                .start();
        CommandLine cmd = new CommandLine(MainCommand.class, new MicronautFactory(ctx));
        cmd.setUsageHelpLongOptionsMaxWidth(35);
        final int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}