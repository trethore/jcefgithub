package io.github.trethore.jcefgithub.impl.step.extract;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TarGzExtractorTest {
    @TempDir Path tempDir;

    @Test
    void rejectsPathTraversal() throws Exception {
        assertThrows(IOException.class,
                () -> TarGzExtractor.extractTarGZ(tempDir.toFile(), archive("../escape", false)));
    }

    @Test
    void rejectsLinks() throws Exception {
        assertThrows(IOException.class,
                () -> TarGzExtractor.extractTarGZ(tempDir.toFile(), archive("link", true)));
    }

    @Test
    void extractsRegularExecutableFile() throws Exception {
        byte[] content = "#!/bin/sh\necho test\n".getBytes(StandardCharsets.UTF_8);
        TarGzExtractor.extractTarGZ(tempDir.toFile(), archive("bin/helper", content, 0755));

        Path helper = tempDir.resolve("bin/helper");
        assertEquals(new String(content, StandardCharsets.UTF_8), java.nio.file.Files.readString(helper));
        assertTrue(helper.toFile().canExecute());
    }

    private static ByteArrayInputStream archive(String name, boolean link) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(
                name, link ? TarArchiveEntry.LF_SYMLINK : TarArchiveEntry.LF_NORMAL);
        if (link) {
            entry.setLinkName("target");
        }
        return archive(entry, new byte[0]);
    }

    private static ByteArrayInputStream archive(String name, byte[] content, int mode) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setMode(mode);
        return archive(entry, content);
    }

    private static ByteArrayInputStream archive(TarArchiveEntry entry, byte[] content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(new GzipCompressorOutputStream(bytes))) {
            entry.setSize(content.length);
            tar.putArchiveEntry(entry);
            tar.write(content);
            tar.closeArchiveEntry();
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }
}
