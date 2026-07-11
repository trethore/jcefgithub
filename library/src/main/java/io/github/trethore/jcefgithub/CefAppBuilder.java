package io.github.trethore.jcefgithub;

import io.github.trethore.jcefgithub.impl.progress.ConsoleProgressHandler;
import io.github.trethore.jcefgithub.impl.step.CefInstaller;
import io.github.trethore.jcefgithub.impl.step.init.CefInitializer;
import org.cef.CefApp;
import org.cef.CefSettings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Class used to configure the JCef environment. Specify
 * an installation directory, arguments to be passed to JCef
 * and configure the embedded {@link org.cef.CefSettings} to
 * your needs. When done, call
 * {@link io.github.trethore.jcefgithub.CefAppBuilder#build()}
 * to create an {@link org.cef.CefApp} instance.
 * <p>
 * Example use:
 * 
 * <pre>
 * {@code
 * //Create a new CefAppBuilder instance
 * CefAppBuilder builder = new CefAppBuilder();
 *
 * //Configure the builder instance
 * builder.setInstallDir(new File("jcef-bundle")); //Default
 * builder.setProgressHandler(new ConsoleProgressHandler()); //Default
 * builder.addJcefArgs("--disable-gpu"); //Just an example
 * builder.getCefSettings().windowless_rendering_enabled = true; //Default - select OSR mode
 *
 * //Set an app handler. Do not use CefApp.addAppHandler(...), it will break your code on MacOSX!
 * builder.setAppHandler(new MavenCefAppHandlerAdapter(){...});
 *
 * //Build a CefApp instance using the configuration above
 * CefApp app = builder.build();
 * }
 * </pre>
 *
 * @author Titouan Réthoré
 */
public class CefAppBuilder {
    private static final File DEFAULT_INSTALL_DIR = new File("jcef-bundle");
    private static final IProgressHandler DEFAULT_PROGRESS_HANDLER = new ConsoleProgressHandler();
    private static final List<String> DEFAULT_JCEF_ARGS = new LinkedList<>();
    private static final CefSettings DEFAULT_CEF_SETTINGS = new CefSettings();
    private static final Object APP_LOCK = new Object();
    private static volatile CefApp sharedInstance;
    private static boolean building;
    private final List<String> jcefArgs;
    private final CefSettings cefSettings;
    private File installDir;
    private IProgressHandler progressHandler;
    private boolean skipInstallation = false;
    private final List<String> mirrors;

    /**
     * Constructs a new CefAppBuilder instance.
     */
    public CefAppBuilder() {
        installDir = DEFAULT_INSTALL_DIR;
        progressHandler = DEFAULT_PROGRESS_HANDLER;
        jcefArgs = new LinkedList<>();
        jcefArgs.addAll(DEFAULT_JCEF_ARGS);
        cefSettings = DEFAULT_CEF_SETTINGS.clone();
        mirrors = new ArrayList<>();
        mirrors.add(
                "https://repo1.maven.org/maven2/io/github/trethore/jcef-natives-{platform}/{tag}/jcef-natives-{platform}-{tag}.jar");
        mirrors.add(
                "https://github.com/trethore/jcefgithub/releases/download/{mvn_version}/jcef-natives-{platform}-{tag}.jar");
    }

    /**
     * Sets the install directory to use. Defaults to "./jcef-bundle".
     *
     * @param installDir the directory to install to
     */
    public void setInstallDir(File installDir) {
        Objects.requireNonNull(installDir, "installDir cannot be null");
        this.installDir = installDir;
    }

    /**
     * Specify a progress handler to receive install progress updates.
     * Defaults to "new ConsoleProgressHandler()".
     *
     * @param progressHandler a progress handler to use
     */
    public void setProgressHandler(IProgressHandler progressHandler) {
        Objects.requireNonNull(progressHandler, "progressHandler cannot be null");
        this.progressHandler = progressHandler;
    }

    /**
     * Retrieves a mutable list of arguments to pass to the JCef library.
     * Arguments may contain spaces.
     * <p>
     * Due to installation using maven some arguments may be overwritten
     * again depending on your platform. Make sure to not specify arguments
     * that break the installation process (e.g. subprocess path, resources
     * path...)!
     *
     * @return A mutable list of arguments to pass to the JCef library
     */
    public List<String> getJcefArgs() {
        return jcefArgs;
    }

    /**
     * Add one or multiple arguments to pass to the JCef library.
     * Arguments may contain spaces.
     * <p>
     * Due to installation using maven some arguments may be overwritten
     * again depending on your platform. Make sure to not specify arguments
     * that break the installation process (e.g. subprocess path, resources
     * path...)!
     *
     * @param args the arguments to add
     */
    public void addJcefArgs(String... args) {
        Objects.requireNonNull(args, "args cannot be null");
        jcefArgs.addAll(Arrays.asList(args));
    }

    /**
     * Retrieve the embedded {@link org.cef.CefSettings} instance to change
     * configuration parameters.
     * <p>
     * Due to installation using maven some settings may be overwritten
     * again depending on your platform.
     *
     * @return the embedded {@link org.cef.CefSettings} instance
     */
    public CefSettings getCefSettings() {
        return cefSettings;
    }

    /**
     * Attach your own adapter to handle certain events in CEF.
     *
     * @param handlerAdapter the adapter to attach
     */
    public void setAppHandler(MavenCefAppHandlerAdapter handlerAdapter) {
        CefApp.addAppHandler(Objects.requireNonNull(handlerAdapter, "handlerAdapter cannot be null"));
    }

    /**
     * Get a copy of all mirrors that are currently in use. To add another mirror,
     * use the setter.
     * Mirror urls can contain placeholders that are replaced when a fetch is
     * attempted:
     * <br/>
     * {mvn_version}: The version of jcefgithub (e.g. 100.0.14.3) <br/>
     * {platform}: The desired platform for the download (e.g. linux-amd64) <br/>
     * {tag}: The desired version tag for the download (e.g.
     * jcef-08efede+cef-100.0.14+g4e5ba66+chromium-100.0.4896.75)
     *
     * @return A copy of all mirrors that are currently in use. First element will
     *         be attempted first.
     */
    public Collection<String> getMirrors() {
        return new ArrayList<>(mirrors);
    }

    /**
     * Set mirror urls that should be used when downloading jcef. First element will
     * be attempted first.
     * Mirror urls can contain placeholders that are replaced when a fetch is
     * attempted:
     * <br/>
     * {mvn_version}: The version of jcefgithub (e.g. 100.0.14.3) <br/>
     * {platform}: The desired platform for the download (e.g. linux-amd64) <br/>
     * {tag}: The desired version tag for the download (e.g.
     * jcef-08efede+cef-100.0.14+g4e5ba66+chromium-100.0.4896.75)
     */
    public void setMirrors(Collection<String> mirrors) {
        Objects.requireNonNull(mirrors, "mirrors can not be null");
        if (mirrors.isEmpty()) {
            throw new IllegalArgumentException("mirrors cannot be empty");
        }
        for (String mirror : mirrors) {
            if (mirror == null || mirror.isBlank()) {
                throw new IllegalArgumentException("mirrors cannot contain null or blank values");
            }
        }
        this.mirrors.clear();
        this.mirrors.addAll(mirrors);
    }

    /**
     * If installation skipping is enabled, no checks against the installation
     * directory will be performed and the download,
     * installation and verification of the jcef natives has to be performed by the
     * individual developer.
     * 
     * @param skipInstallation true if the installation process should be skipped,
     *                         false otherwise
     */
    public void setSkipInstallation(boolean skipInstallation) {
        this.skipInstallation = skipInstallation;
    }

    /**
     * If installation skipping is enabled, no checks against the installation
     * directory will be performed and the download,
     * installation and verification of the jcef natives has to be performed by the
     * individual developer.
     * 
     * @return true if the installation process should be skipped, false otherwise
     */
    public boolean getSkipInstallation() {
        return this.skipInstallation;
    }

    /**
     * Helper method to install the native libraries/resources. Useful for
     * triggering an install ahead of actually needing to create a CEF app instance.
     * Calls are serialized within this builder and across processes targeting the
     * same installation directory.
     *
     * @return This builder instance
     * @throws IOException                  if an artifact could not be fetched or
     *                                      IO-actions on disk failed
     * @throws UnsupportedPlatformException if the platform is not supported
     */
    public synchronized CefAppBuilder install() throws IOException, UnsupportedPlatformException {
        if (this.skipInstallation) {
            return this;
        }
        new CefInstaller(installDir.toPath(), progressHandler, mirrors).install();
        return this;
    }

    /**
     * Builds a {@link org.cef.CefApp} instance. When called multiple times,
     * will return the previously built instance. This method is thread-safe.
     *
     * @return a built {@link org.cef.CefApp} instance
     * @throws IOException                  if an artifact could not be fetched or
     *                                      IO-actions on disk failed
     * @throws UnsupportedPlatformException if the platform is not supported
     * @throws InterruptedException         if the installation process got
     *                                      interrupted
     * @throws CefInitializationException   if the initialization of JCef failed
     */
    public CefApp build()
            throws IOException, UnsupportedPlatformException, InterruptedException, CefInitializationException {
        if (sharedInstance != null) {
            return sharedInstance;
        }

        if (!beginBuild()) {
            return sharedInstance;
        }

        try {
            this.install();
            this.progressHandler.handleProgress(EnumProgress.INITIALIZING, EnumProgress.NO_ESTIMATION);
            List<String> argsSnapshot = new ArrayList<>(this.jcefArgs);
            CefSettings settingsSnapshot = this.cefSettings.clone();
            CefApp created = CefInitializer.initialize(this.installDir, argsSnapshot, settingsSnapshot);
            return publishInstance(created);
        } finally {
            endBuild();
        }
    }

    private boolean beginBuild() throws InterruptedException {
        synchronized (APP_LOCK) {
            while (building && sharedInstance == null) {
                APP_LOCK.wait();
            }
            if (sharedInstance != null) {
                return false;
            }
            building = true;
            return true;
        }
    }

    private CefApp publishInstance(CefApp created) {
        synchronized (APP_LOCK) {
            if (sharedInstance == null) {
                sharedInstance = created;
                Runtime.getRuntime().addShutdownHook(new Thread(created::dispose, "jcef-shutdown"));
                this.progressHandler.handleProgress(EnumProgress.INITIALIZED, EnumProgress.NO_ESTIMATION);
            }
            return sharedInstance;
        }
    }

    private void endBuild() {
        synchronized (APP_LOCK) {
            building = false;
            APP_LOCK.notifyAll();
        }
    }
}
