package io.github.trethore.jcefgithub.impl.step.check;

import io.github.trethore.jcefgithub.CefBuildInfo;
import io.github.trethore.jcefgithub.EnumPlatform;
import io.github.trethore.jcefgithub.UnsupportedPlatformException;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class used to check for already installed native bundles.
 *
 * @author Fritz Windisch
 */
public final class CefInstallationChecker {
    private static final Logger LOGGER = Logger.getLogger(CefInstallationChecker.class.getName());
    private CefInstallationChecker() { }

    public static boolean checkInstallation(File installDir) throws UnsupportedPlatformException {
        Objects.requireNonNull(installDir, "installDir cannot be null");
        File buildInfo = new File(installDir, "build_meta.json");
        if (!(new File(installDir, "install.lock").exists()))
            return false;
        if (!(buildInfo.exists()))
            return false;
        CefBuildInfo installed;
        try {
            installed = CefBuildInfo.fromFile(buildInfo);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Existing installation metadata is invalid; reinstalling: {0}", e.getMessage());
            return false;
        }
        CefBuildInfo required;
        try {
            required = CefBuildInfo.fromClasspath();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read required JCEF build metadata", e);
        }
        // The install is ok when tag and platform match
        return required.getReleaseTag().equals(installed.getReleaseTag())
                && installed.getPlatform().equals(EnumPlatform.getCurrentPlatform().getIdentifier());
    }
}
