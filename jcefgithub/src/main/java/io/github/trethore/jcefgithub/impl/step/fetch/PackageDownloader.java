package io.github.trethore.jcefgithub.impl.step.fetch;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.trethore.jcefgithub.CefBuildInfo;
import io.github.trethore.jcefgithub.EnumPlatform;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class used to download the native packages from GitHub or central repository.
 * Central repository is only used as fallback.
 *
 * @author Fritz Windisch
 */
public class PackageDownloader {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = Logger.getLogger(PackageDownloader.class.getName());

    private static final int BUFFER_SIZE = 16 * 1024;
    private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
    private static final int READ_TIMEOUT_MILLIS = 60_000;

    public static void downloadNatives(CefBuildInfo info, EnumPlatform platform, File destination,
                                       Consumer<Float> progressConsumer, Collection<String> mirrors) throws IOException {
        Objects.requireNonNull(info, "info cannot be null");
        Objects.requireNonNull(platform, "platform cannot be null");
        Objects.requireNonNull(destination, "destination cannot be null");
        Objects.requireNonNull(progressConsumer, "progressConsumer cannot be null");
        Objects.requireNonNull(mirrors, "mirrors can not be null");
        if (mirrors.isEmpty()) {
            throw new RuntimeException("mirrors can not be empty");
        }

        //Create target file
        if (!destination.createNewFile()) {
            throw new IOException("Could not create target file " + destination.getAbsolutePath());
        }
        //Load maven version
        String mvn_version = loadJCefMavenVersion();

        //Try all mirrors
        Exception lastException = null;
        for (String mirror : mirrors) {
            String m = mirror
                    .replace("{platform}", platform.getIdentifier())
                    .replace("{tag}", info.getReleaseTag())
                    .replace("{mvn_version}", mvn_version);
            try {
                if (downloadFromMirror(m, destination, progressConsumer)) {
                    return;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Request failed with exception on mirror: " + m, e);
                lastException = e;
            }
        }
        //Throw exception if no download was successful
        if (lastException != null) {
            throw new IOException("None of the supplied mirrors were working", lastException);
        } else {
            throw new IOException("None of the supplied mirrors were working");
        }
    }

    private static String loadJCefMavenVersion() throws IOException {
        Map<String, Object> object;
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        try (InputStream in = PackageDownloader.class.getResourceAsStream("/jcefgithub_build_meta.json")) {
            if (in == null) {
                throw new IOException("/jcefgithub_build_meta.json not found on class path");
            }
            object = GSON.fromJson(new InputStreamReader(in), type);
        } catch (Exception e) {
            throw new IOException("Invalid json content in jcefgithub_build_meta.json", e);
        }
        return (String) object.get("version");
    }

    private static boolean downloadFromMirror(String mirrorUrl, File destination, Consumer<Float> progressConsumer) throws IOException {
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

    private static void transfer(HttpURLConnection connection, File destination, Consumer<Float> progressConsumer) throws IOException {
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
                if (length > 0) {
                    long nextProgress = Math.min(100, transferred * 100 / length);
                    if (nextProgress > progress) {
                        progress = nextProgress;
                        progressConsumer.accept((float) progress);
                    }
                }
            }
            fos.flush();

            if (length <= 0 || progress < 100) {
                progressConsumer.accept(100f);
            }
        }
    }
}
