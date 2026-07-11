package io.github.trethore.jcefgithub.impl.step;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CefInstallerTest {
    @TempDir
    Path tempDir;

    @Test
    void replacesExistingInstallation() throws Exception {
        Path destination = Files.createDirectory(tempDir.resolve("install"));
        Files.writeString(destination.resolve("version"), "old");
        Path staging = Files.createDirectory(tempDir.resolve("staging"));
        Files.writeString(staging.resolve("version"), "new");

        CefInstaller.replaceInstallation(staging, destination);

        assertEquals("new", Files.readString(destination.resolve("version")));
        assertFalse(Files.exists(tempDir.resolve("install.backup")));
    }

    @Test
    void restoresExistingInstallationWhenReplacementFails() throws Exception {
        Path destination = Files.createDirectory(tempDir.resolve("install"));
        Files.writeString(destination.resolve("version"), "old");
        Path missingStaging = tempDir.resolve("missing-staging");

        assertThrows(IOException.class, () -> CefInstaller.replaceInstallation(missingStaging, destination));

        assertEquals("old", Files.readString(destination.resolve("version")));
        assertFalse(Files.exists(tempDir.resolve("install.backup")));
    }
}
