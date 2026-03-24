package io.github.trethore.jcefgithub.impl.platform;

/**
 * Defined patterns for different platforms.
 * Used to detect the current platform from the system properties.
 *
 * @author Fritz Windisch
 */
public class PlatformPatterns {
    public static String[] OS_MACOSX = new String[] { "mac", "darwin" };
    public static String[] OS_LINUX = new String[] { "nux" };
    public static String[] OS_WINDOWS = new String[] { "win" };

    public static String[] ARCH_AMD64 = new String[] { "amd64", "x86_64" };
    public static String[] ARCH_ARM64 = new String[] { "arm64", "aarch64" };
}
