package io.github.trethore.jcefgithub.impl.step.extract;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class used to extract .tar.gz archives.
 * Preserves executable attributes.
 *
 * @author Fritz Windisch
 */
public final class TarGzExtractor {
    private static final int BUFFER_SIZE = 4096;
    private static final Logger LOGGER = Logger.getLogger(TarGzExtractor.class.getName());
    private TarGzExtractor() { }

    public static void extractTarGZ(File installDir, InputStream in) throws IOException {
        Objects.requireNonNull(installDir, "installDir cannot be null");
        Objects.requireNonNull(in, "in cannot be null");
        try (GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);
                TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {
            TarArchiveEntry entry;

            while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
                File f = resolveTarget(installDir, entry.getName());
                if (entry.isSymbolicLink() || entry.isLink()) {
                    throw new IOException("Archive links are not supported: " + entry.getName());
                } else if (entry.isDirectory()) {
                    createDirectory(f);
                    setExecutableIfNeeded(f, entry.getMode());
                } else {
                    createFileParent(f);
                    writeFileContent(tarIn, f);
                    setExecutableIfNeeded(f, entry.getMode());
                }
            }
        }
    }

    private static File resolveTarget(File installDir, String entryName) throws IOException {
        File target = new File(installDir, entryName);
        String rootPath = installDir.getCanonicalPath();
        String targetPath = target.getCanonicalPath();
        if (!targetPath.equals(rootPath) && !targetPath.startsWith(rootPath + File.separator)) {
            throw new IOException("Refusing to extract outside installDir: " + entryName);
        }
        return target;
    }

    private static void createDirectory(File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Unable to create directory during archive extraction: " + directory.getAbsolutePath());
        }
    }

    private static void createFileParent(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Unable to create directory during archive extraction: " + parent.getAbsolutePath());
        }
    }

    private static void writeFileContent(TarArchiveInputStream tarIn, File target) throws IOException {
        byte[] data = new byte[BUFFER_SIZE];
        int count;
        try (BufferedOutputStream dest = new BufferedOutputStream(new FileOutputStream(target, false), BUFFER_SIZE)) {
            while ((count = tarIn.read(data, 0, BUFFER_SIZE)) != -1) {
                dest.write(data, 0, count);
            }
        }
    }

    private static void setExecutableIfNeeded(File target, int mode) {
        if ((mode & 0111) != 0 && !target.setExecutable(true, false)) {
            LOGGER.log(Level.SEVERE,
                    "Unable to mark path executable during archive extraction: " + target.getAbsolutePath());
        }
    }
}
