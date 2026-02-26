package io.github.trethore.jcefgithub.impl.step.init;

import io.github.trethore.jcefgithub.CefInitializationException;
import io.github.trethore.jcefgithub.EnumPlatform;
import io.github.trethore.jcefgithub.UnsupportedPlatformException;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.SystemBootstrap;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Platform dependent initialization code for JCef.
 *
 * @author Fritz Windisch
 */
public class CefInitializer {

    private static final Logger LOGGER = Logger.getLogger(CefInitializer.class.getName());

    private static final String JAVA_LIBRARY_PATH = "java.library.path";

    public static CefApp initialize(File installDir, List<String> cefArgs, CefSettings cefSettings) throws UnsupportedPlatformException, CefInitializationException {
        Objects.requireNonNull(installDir, "installDir cannot be null");
        Objects.requireNonNull(cefArgs, "cefArgs cannot be null");
        Objects.requireNonNull(cefSettings, "cefSettings cannot be null");

        try {
            patchJavaLibraryPath(installDir);
            disableJcefDependencyLoader();
            loadJawtLibrary();
            loadPlatformLibraries(EnumPlatform.getCurrentPlatform(), installDir, cefArgs, cefSettings);
            return CefApp.getInstance(cefArgs.toArray(new String[0]), cefSettings);
        } catch (RuntimeException | LinkageError e) {
            throw new CefInitializationException("Error while initializing JCef", e);
        }
    }

    private static void patchJavaLibraryPath(File installDir) {
        String path = System.getProperty(JAVA_LIBRARY_PATH);
        if (path == null || path.isEmpty()) {
            System.setProperty(JAVA_LIBRARY_PATH, installDir.getAbsolutePath());
            return;
        }
        if (!path.endsWith(File.pathSeparator)) {
            path = path + File.pathSeparator;
        }
        path += installDir.getAbsolutePath();
        System.setProperty(JAVA_LIBRARY_PATH, path);
    }

    private static void disableJcefDependencyLoader() {
        SystemBootstrap.setLoader(libname -> {
        });
    }

    private static void loadJawtLibrary() {
        try {
            System.loadLibrary("jawt");
        } catch (UnsatisfiedLinkError e) {
            LOGGER.warning("Error while loading jawt library: " + e.getMessage());
        }
    }

    private static void loadPlatformLibraries(EnumPlatform platform, File installDir, List<String> cefArgs,
                                              CefSettings cefSettings) throws CefInitializationException {
        if (platform.getOs().isWindows()) {
            loadWindowsLibraries(installDir);
            return;
        }
        if (platform.getOs().isLinux()) {
            loadLinuxLibraries(installDir, cefArgs);
            return;
        }
        if (platform.getOs().isMacOSX()) {
            loadMacLibraries(installDir, cefArgs, cefSettings);
        }
    }

    private static void loadWindowsLibraries(File installDir) {
        System.load(new File(installDir, "chrome_elf.dll").getAbsolutePath());
        System.load(new File(installDir, "libcef.dll").getAbsolutePath());
        System.load(new File(installDir, "jcef.dll").getAbsolutePath());
    }

    private static void loadLinuxLibraries(File installDir, List<String> cefArgs) throws CefInitializationException {
        System.load(new File(installDir, "libjcef.so").getAbsolutePath());
        startupCef(cefArgs);
        System.load(new File(installDir, "libcef.so").getAbsolutePath());
    }

    private static void loadMacLibraries(File installDir, List<String> cefArgs, CefSettings cefSettings)
            throws CefInitializationException {
        String basePath = installDir.getAbsolutePath();
        String helperPath = basePath + "/jcef Helper.app/Contents/MacOS/jcef Helper";
        System.load(new File(installDir, "libjcef.dylib").getAbsolutePath());
        cefArgs.add(0, "--framework-dir-path=" + basePath + "/Chromium Embedded Framework.framework");
        cefArgs.add(0, "--main-bundle-path=" + basePath + "/jcef Helper.app");
        cefArgs.add(0, "--browser-subprocess-path=" + helperPath);
        cefSettings.browser_subprocess_path = helperPath;
        startupCef(cefArgs);
    }

    private static void startupCef(List<String> cefArgs) throws CefInitializationException {
        boolean success = CefApp.startup(cefArgs.toArray(new String[0]));
        if (!success) {
            throw new CefInitializationException("JCef did not initialize correctly!");
        }
    }
}
