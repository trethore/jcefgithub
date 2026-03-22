package io.github.trethore.jcefgithub.impl.util;

import java.io.File;
import java.nio.file.Files;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Util providing utils for files.
 *
 * @author Fritz Windisch
 */
public class FileUtils {
    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());

    public static void deleteDir(File dir) {
        Objects.requireNonNull(dir, "dir cannot be null");
        if (!dir.exists())
            return;
        if (dir.isDirectory() && !Files.isSymbolicLink(dir.toPath())) {
            File[] children = dir.listFiles();
            if (children == null) {
                LOGGER.log(Level.WARNING, "Could not read contents of " + dir.getAbsolutePath());
            } else {
                for (File f : children) {
                    deleteDir(f);
                }
            }
        }
        if (!dir.delete()) {
            LOGGER.log(Level.WARNING, "Could not delete " + dir.getAbsolutePath());
        }
    }
}
