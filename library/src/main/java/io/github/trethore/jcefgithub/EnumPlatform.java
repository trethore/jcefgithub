package io.github.trethore.jcefgithub;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.github.trethore.jcefgithub.impl.platform.PlatformPatterns.*;

/**
 * Enum representing all supported operating system and architecture
 * combinations.
 * Fetch the currently applying platform using {@link #getCurrentPlatform()}.
 * Use {@link #getOs()} to extract the operating system only.
 *
 * @author Titouan Réthoré
 */
public enum EnumPlatform {
    // Order is important to try bigger matches first
    // (darwin before win, x86_64 before x86, arm64 before arm)
    MACOSX_AMD64(OS_MACOSX, ARCH_AMD64, EnumOS.MACOSX),
    MACOSX_ARM64(OS_MACOSX, ARCH_ARM64, EnumOS.MACOSX),
    LINUX_AMD64(OS_LINUX, ARCH_AMD64, EnumOS.LINUX),
    LINUX_ARM64(OS_LINUX, ARCH_ARM64, EnumOS.LINUX),
    WINDOWS_AMD64(OS_WINDOWS, ARCH_AMD64, EnumOS.WINDOWS),
    WINDOWS_ARM64(OS_WINDOWS, ARCH_ARM64, EnumOS.WINDOWS);

    public static final String PROPERTY_OS_NAME = "os.name";
    public static final String PROPERTY_OS_ARCH = "os.arch";

    private static final Logger LOGGER = Logger.getLogger(EnumPlatform.class.getName());

    private static EnumPlatform DETECTED_PLATFORM = null;

    private final List<String> osMatch, archMatch;
    private final String identifier;
    private final EnumOS os;

    EnumPlatform(List<String> osMatch, List<String> archMatch, EnumOS os) {
        Objects.requireNonNull(osMatch, "osMatch cannot be null");
        Objects.requireNonNull(archMatch, "archMatch cannot be null");
        Objects.requireNonNull(os, "os cannot be null");
        this.osMatch = osMatch;
        this.archMatch = archMatch;
        this.identifier = name().toLowerCase(Locale.ENGLISH).replace("_", "-");
        this.os = os;
    }

    /**
     * Fetches the platform this program is running on.
     *
     * @return the current platform
     * @throws UnsupportedPlatformException if the platform could not be determined
     */
    public static EnumPlatform getCurrentPlatform() throws UnsupportedPlatformException {
        if (DETECTED_PLATFORM != null)
            return DETECTED_PLATFORM;

        String osName = System.getProperty(PROPERTY_OS_NAME);
        String osArch = System.getProperty(PROPERTY_OS_ARCH);

        EnumPlatform detectedPlatform = detectPlatform(osName, osArch);
        if (detectedPlatform != null) {
            DETECTED_PLATFORM = detectedPlatform;
            return detectedPlatform;
        }

        logUnsupportedPlatform(osName, osArch);
        throw new UnsupportedPlatformException(osName, osArch);
    }

    private static EnumPlatform detectPlatform(String osName, String osArch) {
        for (EnumPlatform platform : values()) {
            if (platform.matches(osName, osArch)) {
                return platform;
            }
        }
        return null;
    }

    private static void logUnsupportedPlatform(String osName, String osArch) {
        LOGGER.log(Level.SEVERE, "Can not detect your current platform. Is it supported?\n" +
                "If you think that this is in error, please open an issue " +
                "providing your " + PROPERTY_OS_NAME + " and " + PROPERTY_OS_ARCH + " from below!\n" +
                "\n" +
                "Your platform specs:\n" +
                PROPERTY_OS_NAME + ": \"" + osName + "\"\n" +
                PROPERTY_OS_ARCH + ": \"" + osArch + "\"\n" +
                "\n" +
                "Supported platforms:\n" +
                supportedPlatformsDescription());
    }

    private static String supportedPlatformsDescription() {
        StringBuilder supported = new StringBuilder();
        for (EnumPlatform platform : values()) {
            supported.append(platform.name())
                    .append('(')
                    .append(PROPERTY_OS_NAME).append(": ").append(platform.osMatch)
                    .append(", ")
                    .append(PROPERTY_OS_ARCH).append(": ").append(platform.archMatch)
                    .append(")\n");
        }
        return supported.toString();
    }

    private boolean matches(String osName, String osArch) {
        Objects.requireNonNull(osName, "osName cannot be null");
        Objects.requireNonNull(osArch, "osArch cannot be null");

        String normalizedOsName = osName.toLowerCase(Locale.ENGLISH);
        String normalizedOsArch = osArch.toLowerCase(Locale.ENGLISH);
        return containsAny(normalizedOsName, osMatch) && equalsAny(normalizedOsArch, archMatch);
    }

    private static boolean containsAny(String value, List<String> fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private static boolean equalsAny(String value, List<String> candidates) {
        for (String candidate : candidates) {
            if (value.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method used internally to fetch the identifier used in jcefbuild.
     *
     * @return the platform identifier, a lower case string in the format of os-arch
     *         (e.g. linux-amd64)
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Fetch the operating system of this platform
     *
     * @return the os
     */
    public EnumOS getOs() {
        return os;
    }
}
