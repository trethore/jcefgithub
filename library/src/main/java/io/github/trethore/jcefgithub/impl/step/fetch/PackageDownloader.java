package io.github.trethore.jcefgithub.impl.step.fetch;

import io.github.trethore.jcefgithub.CefBuildInfo;
import io.github.trethore.jcefgithub.EnumPlatform;
import io.github.trethore.jcefgithub.impl.util.JsonStringMapLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
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
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MILLIS))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static void downloadNatives(CefBuildInfo info, EnumPlatform platform, File destination,
            Consumer<Float> progressConsumer, Collection<String> mirrors) throws IOException {
        validateDownloadRequest(info, platform, destination, progressConsumer, mirrors);
        Map<String, String> metadata = loadMetadata();
        String mavenVersion = requireMetadata(metadata, "version");
        String expectedSha256 = requireMetadata(metadata, "sha256_" + platform.getIdentifier().replace('-', '_'));
        List<String> resolvedMirrors = mirrors.stream()
                .map(mirror -> resolveMirrorUrl(mirror, info, platform, mavenVersion))
                .toList();
        try {
            downloadFromMirrors(resolvedMirrors, destination, progressConsumer, expectedSha256);
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

    private static void deleteIncompleteDestination(File destination) {
        if (!destination.exists()) {
            return;
        }
        if (!destination.delete()) {
            LOGGER.log(Level.WARNING, "Could not remove incomplete target file {0}", destination.getAbsolutePath());
        }
    }

    static void downloadFromMirrors(Collection<String> resolvedMirrors, File destination,
            Consumer<Float> progressConsumer, String expectedSha256) throws IOException {
        IOException lastException = null;
        for (String resolvedMirror : resolvedMirrors) {
            try {
                downloadFromMirror(resolvedMirror, destination, progressConsumer);
                verifySha256(destination, expectedSha256);
                return;
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Mirror failed: {0} ({1})",
                        new Object[] { resolvedMirror, e.getMessage() });
                lastException = e;
                deleteIncompleteDestination(destination);
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

    private static void downloadFromMirror(String mirrorUrl, File destination, Consumer<Float> progressConsumer)
            throws IOException {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(mirrorUrl))
                    .timeout(Duration.ofMillis(READ_TIMEOUT_MILLIS))
                    .GET()
                    .build();
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid mirror URL: " + mirrorUrl, e);
        }
        try {
            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                response.body().close();
                throw new IOException("Mirror returned HTTP " + response.statusCode() + ": " + mirrorUrl);
            }
            long length = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            try (InputStream body = response.body()) {
                transfer(body, length, destination, progressConsumer);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading from " + mirrorUrl, e);
        }
    }

    private static void transfer(InputStream in, long length, File destination, Consumer<Float> progressConsumer)
            throws IOException {
        try (var out = Files.newOutputStream(destination.toPath(), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long transferred = 0;
            long progress = 0;

            progressConsumer.accept(0f);
            int read;
            while ((read = in.read(buffer)) != -1) {
                if (read == 0) {
                    continue;
                }
                out.write(buffer, 0, read);
                transferred += read;
                progress = updateProgress(length, transferred, progress, progressConsumer);
            }
            out.flush();

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
