package io.github.trethore.jcefgithub.impl.platform;

import java.util.List;

/**
 * Defined patterns for different platforms.
 * Used to detect the current platform from the system properties.
 *
 * @author Fritz Windisch
 */
public final class PlatformPatterns {
    private PlatformPatterns() { }
    public static final List<String> OS_MACOSX = List.of("mac", "darwin");
    public static final List<String> OS_LINUX = List.of("nux");
    public static final List<String> OS_WINDOWS = List.of("win");

    public static final List<String> ARCH_AMD64 = List.of("amd64", "x86_64");
    public static final List<String> ARCH_ARM64 = List.of("arm64", "aarch64");
}
