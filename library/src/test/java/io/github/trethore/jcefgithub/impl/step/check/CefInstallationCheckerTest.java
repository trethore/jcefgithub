package io.github.trethore.jcefgithub.impl.step.check;

import io.github.trethore.jcefgithub.CefBuildInfo;
import io.github.trethore.jcefgithub.EnumPlatform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CefInstallationCheckerTest {
    @TempDir
    Path tempDir;

    @Test
    void acceptsMatchingInstallationAndRejectsMismatch() throws Exception {
        CefBuildInfo required = CefBuildInfo.fromClasspath();
        Files.createFile(tempDir.resolve("install.lock"));
        writeMetadata(required.getReleaseTag(), EnumPlatform.getCurrentPlatform().getIdentifier());
        assertTrue(CefInstallationChecker.checkInstallation(tempDir.toFile()));

        writeMetadata("different-tag", EnumPlatform.getCurrentPlatform().getIdentifier());
        assertFalse(CefInstallationChecker.checkInstallation(tempDir.toFile()));
    }

    @Test
    void rejectsMalformedInstallationMetadata() throws Exception {
        Files.createFile(tempDir.resolve("install.lock"));
        Files.writeString(tempDir.resolve("build_meta.json"), "not-json");
        assertFalse(CefInstallationChecker.checkInstallation(tempDir.toFile()));
    }

    private void writeMetadata(String tag, String platform) throws Exception {
        Files.writeString(tempDir.resolve("build_meta.json"), """
                {
                  "jcef_url": "jcef",
                  "release_tag": "%s",
                  "release_url": "release",
                  "platform": "%s"
                }
                """.formatted(tag, platform));
    }
}
