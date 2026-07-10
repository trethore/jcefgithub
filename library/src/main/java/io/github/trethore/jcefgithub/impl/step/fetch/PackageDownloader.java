package io.github.trethore.jcefgithub.impl.step.fetch;

import io.github.trethore.jcefgithub.CefBuildInfo;
import io.github.trethore.jcefgithub.EnumPlatform;
import io.github.trethore.jcefgithub.impl.util.JsonStringMapLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class used to download the native packages from the configured mirrors.
 *
 * @author Titouan Réthoré
 */
public final class PackageDownloader {
    private PackageDownloader() { }
    private static final Logger LOGGER = Logger.getLogger(PackageDownloader.class.getName());

    private static final int BUFFER_SIZE = 16 * 1024;
    private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
    private static final int READ_TIMEOUT_MILLIS = 60_000;

    public static void downloadNatives(CefBuildInfo info, EnumPlatform platform, File destination,
            Consumer<Float> progressConsumer, Collection<String> mirrors) throws IOException {
        validateDownloadRequest(info, platform, destination, progressConsumer, mirrors);
        Map<String, String> metadata = loadMetadata();
        String mavenVersion = requireMetadata(metadata, "version");
        String expectedSha256 = requireMetadata(metadata, "sha256_" + platform.getIdentifier().replace('-', '_'));
        createDestinationFile(destination);
        try {
            downloadFromMirrors(info, platform, destination, progressConsumer, mirrors, mavenVersion);
            verifySha256(destination, expectedSha256);
        } catch (IOException e) {
            deleteIncompleteDestination(destination);
            throw e;
        }
    }

    private static Map<String, String> loadMetadata() throws IOException {
        return JsonStringMapLoader.loadClasspathStringMap(
                PackageDownloader.class,
                "/jcefgithub_build_meta.json");
    }

    private static String requireMetadata(Map<String, String> metadata, String key) throws IOException {
        String value = metadata.get(key);
        if (value == null || value.isBlank() || value.startsWith("${")) {
            throw new IOException("Missing " + key + " in jcefgithub_build_meta.json");
        }
        return value;
    }

    static void verifySha256(File file, String expected) throws IOException {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
            String actual = HexFormat.of().formatHex(digest.digest());
            if (!actual.equalsIgnoreCase(expected)) {
                throw new IOException("SHA-256 mismatch for downloaded native bundle");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 is unavailable", e);
        }
    }

    private static void validateDownloadRequest(CefBuildInfo info, EnumPlatform platform, File destination,
            Consumer<Float> progressConsumer, Collection<String> mirrors) {
        Objects.requireNonNull(info, "info cannot be null");
        Objects.requireNonNull(platform, "platform cannot be null");
        Objects.requireNonNull(destination, "destination cannot be null");
        Objects.requireNonNull(progressConsumer, "progressConsumer cannot be null");
        Objects.requireNonNull(mirrors, "mirrors can not be null");
        if (mirrors.isEmpty()) {
            throw new IllegalArgumentException("mirrors can not be empty");
        }
    }

    private static void createDestinationFile(File destination) throws IOException {
        if (!destination.createNewFile()) {
            throw new IOException("Could not create target file " + destination.getAbsolutePath());
        }
    }

    private static void deleteIncompleteDestination(File destination) {
        if (!destination.exists()) {
            return;
        }
        if (!destination.delete()) {
            LOGGER.log(Level.WARNING, "Could not remove incomplete target file {0}", destination.getAbsolutePath());
        }
    }

    private static void downloadFromMirrors(CefBuildInfo info, EnumPlatform platform, File destination,
            Consumer<Float> progressConsumer, Collection<String> mirrors,
            String mavenVersion) throws IOException {
        IOException lastException = null;
        for (String mirror : mirrors) {
            String resolvedMirror = resolveMirrorUrl(mirror, info, platform, mavenVersion);
            try {
                if (downloadFromMirror(resolvedMirror, destination, progressConsumer)) {
                    return;
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Request failed with exception on mirror: " + resolvedMirror, e);
                lastException = e;
            }
        }
        throwIfDownloadFailed(lastException);
    }

    private static String resolveMirrorUrl(String mirror, CefBuildInfo info, EnumPlatform platform,
            String mavenVersion) {
        return mirror
                .replace("{platform}", platform.getIdentifier())
                .replace("{tag}", info.getReleaseTag())
                .replace("{mvn_version}", mavenVersion);
    }

    private static void throwIfDownloadFailed(IOException lastException) throws IOException {
        if (lastException == null) {
            throw new IOException("None of the supplied mirrors were working");
        }
        throw new IOException("None of the supplied mirrors were working", lastException);
    }

    private static boolean downloadFromMirror(String mirrorUrl, File destination, Consumer<Float> progressConsumer)
            throws IOException {
        HttpURLConnection connection = openConnection(mirrorUrl);
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                LOGGER.log(Level.WARNING, "Request to mirror failed with code " + responseCode
                        + " from server: " + mirrorUrl);
                return false;
            }
            transfer(connection, destination, progressConsumer);
            return true;
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection openConnection(String mirrorUrl) throws IOException {
        URL url = new URL(mirrorUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        return connection;
    }

    private static void transfer(HttpURLConnection connection, File destination, Consumer<Float> progressConsumer)
            throws IOException {
        long length = connection.getContentLengthLong();
        try (InputStream in = connection.getInputStream();
                FileOutputStream fos = new FileOutputStream(destination, false)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long transferred = 0;
            long progress = 0;

            progressConsumer.accept(0f);
            int read;
            while ((read = in.read(buffer)) != -1) {
                if (read == 0) {
                    continue;
                }
                fos.write(buffer, 0, read);
                transferred += read;
                progress = updateProgress(length, transferred, progress, progressConsumer);
            }
            fos.flush();

            if (length <= 0 || progress < 100) {
                progressConsumer.accept(100f);
            }
        }
    }

    private static long updateProgress(long length, long transferred, long currentProgress,
            Consumer<Float> progressConsumer) {
        if (length <= 0) {
            return currentProgress;
        }

        long nextProgress = Math.min(100, transferred * 100 / length);
        if (nextProgress <= currentProgress) {
            return currentProgress;
        }

        progressConsumer.accept((float) nextProgress);
        return nextProgress;
    }
}
