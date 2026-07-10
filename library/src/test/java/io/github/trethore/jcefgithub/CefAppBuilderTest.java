package io.github.trethore.jcefgithub;

import org.junit.jupiter.api.Test;

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
}
