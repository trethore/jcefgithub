package io.github.trethore.jcefgithub.impl.util;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Utility for loading simple JSON string maps.
 *
 * @author Titouan Réthoré
 */
public final class JsonStringMapLoader {
    private static final Gson GSON = new Gson();
    private static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();

    private JsonStringMapLoader() {
    }

    public static Map<String, String> loadStringMap(InputStream in, String sourceName) throws IOException {
        Objects.requireNonNull(sourceName, "sourceName cannot be null");

        try (InputStream input = Objects.requireNonNull(in, "in cannot be null");
                Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            Map<String, String> object = GSON.fromJson(reader, STRING_MAP_TYPE);
            if (object == null) {
                throw new IOException(sourceName + " is empty");
            }
            return object;
        } catch (JsonParseException e) {
            throw new IOException("Invalid json content in " + sourceName, e);
        }
    }

    public static Map<String, String> loadClasspathStringMap(Class<?> resourceOwner, String resourcePath)
            throws IOException {
        Objects.requireNonNull(resourceOwner, "resourceOwner cannot be null");
        Objects.requireNonNull(resourcePath, "resourcePath cannot be null");

        InputStream in = resourceOwner.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException(resourcePath + " not found on class path");
        }

        return loadStringMap(in, trimLeadingSlash(resourcePath));
    }

    private static String trimLeadingSlash(String value) {
        if (value.startsWith("/")) {
            return value.substring(1);
        }
        return value;
    }
}
