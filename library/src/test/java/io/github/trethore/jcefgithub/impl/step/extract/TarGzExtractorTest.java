package io.github.trethore.jcefgithub.impl.step.extract;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TarGzExtractorTest {
    @TempDir Path tempDir;

    @Test
    void rejectsPathTraversal() throws Exception {
        assertThrows(IOException.class, () -> TarGzExtractor.extractTarGZ(tempDir.toFile(), archive("../escape", false)));
    }

    @Test
    void rejectsLinks() throws Exception {
        assertThrows(IOException.class, () -> TarGzExtractor.extractTarGZ(tempDir.toFile(), archive("link", true)));
    }

    private static ByteArrayInputStream archive(String name, boolean link) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(new GzipCompressorOutputStream(bytes))) {
            TarArchiveEntry entry = new TarArchiveEntry(name, link ? TarArchiveEntry.LF_SYMLINK : TarArchiveEntry.LF_NORMAL);
            if (link) entry.setLinkName("target");
            entry.setSize(0);
            tar.putArchiveEntry(entry);
            tar.closeArchiveEntry();
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }
}
