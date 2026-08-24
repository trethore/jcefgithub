package io.github.trethore.jcefgithub.impl.util.macos;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Util used to unquarantine directories recursively on MacOS.
 * Else MacOS would screw the installation.
 *
 * @author Fritz Windisch
 */
public final class UnquarantineUtil {
    private static final Logger LOGGER = Logger.getLogger(UnquarantineUtil.class.getName());
    private static final String XATTR_PATH = "/usr/bin/xattr";

    private UnquarantineUtil() { }

    /** @deprecated Use {@link #unquarantine(Path)}. */
    @Deprecated(forRemoval = false)
    public static void unquarantine(File dir) {
        try {
            unquarantine(Objects.requireNonNull(dir, "dir cannot be null").toPath());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not remove macOS quarantine attributes", e);
        }
    }

    public static void unquarantine(Path dir) throws IOException {
        Objects.requireNonNull(dir, "dir cannot be null");
        Process process = new ProcessBuilder(
                XATTR_PATH, "-r", "-d", "com.apple.quarantine", dir.toAbsolutePath().toString())
                .redirectErrorStream(true)
                .start();
        try {
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("xattr failed with exit code " + exitCode
                        + (output.isEmpty() ? "" : ": " + output));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while removing macOS quarantine attributes", e);
        }
    }
}
