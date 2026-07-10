package io.github.trethore.jcefgithub;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnumPlatformTest {
    @ParameterizedTest
    @CsvSource({
            "Linux,amd64,LINUX_AMD64", "Linux,aarch64,LINUX_ARM64",
            "Mac OS X,x86_64,MACOSX_AMD64", "Darwin,arm64,MACOSX_ARM64",
            "Windows 11,amd64,WINDOWS_AMD64", "Windows 11,aarch64,WINDOWS_ARM64"
    })
    void detectsSupportedPlatforms(String os, String arch, EnumPlatform expected) throws Exception {
        Method detect = EnumPlatform.class.getDeclaredMethod("detectPlatform", String.class, String.class);
        detect.setAccessible(true);
        assertEquals(expected, detect.invoke(null, os, arch));
    }
}
