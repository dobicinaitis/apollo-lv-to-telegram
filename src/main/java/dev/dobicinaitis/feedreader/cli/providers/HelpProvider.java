package dev.dobicinaitis.feedreader.cli.providers;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        name = "apollo-lv-to-telegram",
        headerHeading = "@|bold NAME|@%n      apollo-lv-to-telegram%n",
        synopsisHeading = "%n@|bold USAGE|@%n      ",
        descriptionHeading = "%n@|bold DESCRIPTION|@%n      ",
        description = "Posts https://apollo.lv news headlines to Telegram Channel \"apollo.lv ziņas\".",
        parameterListHeading = "%n@|bold PARAMETERS|@%n",
        optionListHeading = "%n@|bold OPTIONS|@%n",
        mixinStandardHelpOptions = true,
        usageHelpAutoWidth = true,
        versionProvider = VersionProvider.class,
        sortSynopsis = false
)
public class HelpProvider {
}
