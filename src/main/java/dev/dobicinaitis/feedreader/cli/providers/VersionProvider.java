package dev.dobicinaitis.feedreader.cli.providers;

import dev.dobicinaitis.feedreader.dto.ApplicationVersion;
import lombok.SneakyThrows;
import picocli.CommandLine;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class VersionProvider implements CommandLine.IVersionProvider {

    private static final String MANIFEST_RESOURCE = "META-INF/MANIFEST.MF";
    private static final String VERSION_PROPERTY = "Application-Version";
    private static final String BUILD_PROPERTY = "Application-Build";

    /**
     * Returns version information for -V, --version option outputs.
     *
     * @return version string
     */
    public String[] getVersion() {
        final ApplicationVersion versionInfo = getVersionFromManifestFile();
        final String picocliVersionOutput = String.format("${COMMAND-FULL-NAME} version %s, build %s",
                versionInfo.getVersion(), versionInfo.getBuild());
        return new String[]{picocliVersionOutput};
    }

    /**
     * Reads version information from the MANIFEST.MF file.
     *
     * @return version information
     */
    @SneakyThrows
    public static ApplicationVersion getVersionFromManifestFile() {
        final Manifest manifest = new Manifest();
        manifest.read(VersionProvider.class.getClassLoader().getResourceAsStream(MANIFEST_RESOURCE));
        final Attributes attributes = manifest.getMainAttributes();
        return ApplicationVersion.builder()
                .version(attributes.getValue(VERSION_PROPERTY))
                .build(attributes.getValue(BUILD_PROPERTY))
                .build();
    }

    public static String getVersionNumber() {
        return getVersionFromManifestFile().getVersion();
    }

    public static String getBuildNumber() {
        return getVersionFromManifestFile().getBuild();
    }
}
