package io.github.trethore.jcefgithub;

import io.github.trethore.jcefgithub.impl.util.JsonStringMapLoader;
import org.cef.CefApp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

/**
 * Provides information about jcefgithub builds
 *
 * @author Titouan Réthoré
 */
public class CefBuildInfo {
    private static CefBuildInfo localBuildInfo;
    private final String jcefUrl;
    private final String releaseTag;
    private final String releaseUrl;
    private final String platform;

    private CefBuildInfo(String jcefUrl, String releaseTag, String releaseUrl, String platform) {
        this.jcefUrl = jcefUrl;
        this.releaseTag = releaseTag;
        this.releaseUrl = releaseUrl;
        this.platform = platform;
    }

    /**
     * Reads the in-use jcefgithub build info from classpath
     *
     * @return jcefgithub build info
     * @throws NullPointerException if the build info "/build_meta.json" does not
     *                              exist on classpath
     * @throws IOException          if reading the build info failed
     */
    public static synchronized CefBuildInfo fromClasspath() throws IOException {
        if (localBuildInfo == null) {
            localBuildInfo = loadData(
                    Objects.requireNonNull(CefApp.class.getResourceAsStream("/build_meta.json"),
                            "The build_meta.json file from the jcef-api artifact could not be read"));
        }
        return localBuildInfo;
    }

    /**
     * Loads a CefBuildInfo instance from a file
     *
     * @param file the file to read
     * @return jcefgithub build info
     * @throws IOException if reading the file failed
     */
    public static CefBuildInfo fromFile(File file) throws IOException {
        return loadData(Files.newInputStream(Objects.requireNonNull(file, "file cannot be null").toPath()));
    }

    private static CefBuildInfo loadData(InputStream in) throws IOException {
        Map<String, String> object = JsonStringMapLoader.loadStringMap(in, "build_meta.json");
        return new CefBuildInfo(
                requireValue(object, "jcef_url"),
                requireValue(object, "release_tag"),
                requireValue(object, "release_url"),
                requireValue(object, "platform"));
    }

    private static String requireValue(Map<String, String> object, String key) throws IOException {
        String value = object.get(key);
        if (value == null || value.isBlank()) {
            throw new IOException("No " + key + " specified in build_meta.json");
        }
        return value;
    }

    public String getJcefUrl() {
        return jcefUrl;
    }

    public String getReleaseTag() {
        return releaseTag;
    }

    public String getReleaseUrl() {
        return releaseUrl;
    }

    public String getPlatform() {
        return platform;
    }
}
