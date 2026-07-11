package io.github.trethore.jcefgithub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CefBuildInfoTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsCompleteMetadata() throws Exception {
        Path file = metadata("""
                {
                  "jcef_url": "jcef",
                  "release_tag": "tag",
                  "release_url": "release",
                  "platform": "linux-amd64"
                }
                """);

        CefBuildInfo info = CefBuildInfo.fromFile(file.toFile());

        assertEquals("tag", info.getReleaseTag());
        assertEquals("linux-amd64", info.getPlatform());
    }

    @Test
    void rejectsMalformedAndIncompleteMetadata() throws Exception {
        assertThrows(IOException.class, () -> CefBuildInfo.fromFile(metadata("not-json").toFile()));
        assertThrows(IOException.class, () -> CefBuildInfo.fromFile(metadata("""
                {
                  "jcef_url": "jcef"
                }
                """).toFile()));
    }

    private Path metadata(String content) throws IOException {
        return Files.writeString(tempDir.resolve("metadata-" + System.nanoTime() + ".json"), content);
    }
}
