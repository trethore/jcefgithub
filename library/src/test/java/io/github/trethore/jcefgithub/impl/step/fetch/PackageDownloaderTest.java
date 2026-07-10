package io.github.trethore.jcefgithub.impl.step.fetch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PackageDownloaderTest {
    @TempDir Path tempDir;

    @Test
    void verifiesExpectedSha256() throws Exception {
        Path artifact = Files.writeString(tempDir.resolve("artifact.jar"), "native artifact");
        String expected = "9eaa01bd3b56258e0e41821a383e1e6282090e0f355fdf6c10883b38c612e8a8";

        assertDoesNotThrow(() -> PackageDownloader.verifySha256(artifact.toFile(), expected));
        assertThrows(IOException.class, () -> PackageDownloader.verifySha256(artifact.toFile(), "0".repeat(64)));
    }
}
