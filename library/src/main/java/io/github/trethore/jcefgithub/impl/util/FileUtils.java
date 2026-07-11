package io.github.trethore.jcefgithub.impl.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Util providing utils for files.
 *
 * @author Fritz Windisch
 */
public final class FileUtils {
    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());
    private FileUtils() { }

    public static void deleteDir(File dir) {
        Objects.requireNonNull(dir, "dir cannot be null");
        try {
            deleteRecursivelyIfExists(dir.toPath());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not delete " + dir.getAbsolutePath(), e);
        }
    }

    public static void deleteRecursivelyIfExists(Path path) throws IOException {
        Objects.requireNonNull(path, "path cannot be null");
        if (Files.exists(path)) {
            deleteRecursively(path);
        }
    }

    public static void deleteRecursively(Path path) throws IOException {
        Objects.requireNonNull(path, "path cannot be null");
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) {
                    throw error;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
