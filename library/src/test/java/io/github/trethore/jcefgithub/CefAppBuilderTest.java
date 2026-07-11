package io.github.trethore.jcefgithub;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CefAppBuilderTest {
    @Test
    void skipInstallationStateIsIndependentFromInstallation() {
        CefAppBuilder builder = new CefAppBuilder();
        assertFalse(builder.getSkipInstallation());
        builder.setSkipInstallation(true);
        assertTrue(builder.getSkipInstallation());
        builder.setSkipInstallation(false);
        assertFalse(builder.getSkipInstallation());
    }

    @Test
    void rejectsInvalidMirrorConfiguration() {
        CefAppBuilder builder = new CefAppBuilder();
        assertThrows(IllegalArgumentException.class, () -> builder.setMirrors(List.of()));
        assertThrows(IllegalArgumentException.class, () -> builder.setMirrors(java.util.Arrays.asList("", null)));
    }
}
