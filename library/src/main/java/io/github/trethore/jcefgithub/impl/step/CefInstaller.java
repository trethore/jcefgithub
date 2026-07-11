package io.github.trethore.jcefgithub.impl.step;

import io.github.trethore.jcefgithub.CefBuildInfo;
import io.github.trethore.jcefgithub.EnumPlatform;
import io.github.trethore.jcefgithub.EnumProgress;
import io.github.trethore.jcefgithub.IProgressHandler;
import io.github.trethore.jcefgithub.UnsupportedPlatformException;
import io.github.trethore.jcefgithub.impl.step.check.CefInstallationChecker;
import io.github.trethore.jcefgithub.impl.step.extract.TarGzExtractor;
import io.github.trethore.jcefgithub.impl.step.fetch.PackageClasspathStreamer;
import io.github.trethore.jcefgithub.impl.step.fetch.PackageDownloader;
import io.github.trethore.jcefgithub.impl.util.FileUtils;
import io.github.trethore.jcefgithub.impl.util.macos.UnquarantineUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Installs and verifies the platform-specific JCEF native bundle. */
public final class CefInstaller {
    private static final Logger LOGGER = Logger.getLogger(CefInstaller.class.getName());

    private final Path installDir;
    private final IProgressHandler progressHandler;
    private final List<String> mirrors;

    public CefInstaller(Path installDir, IProgressHandler progressHandler, Collection<String> mirrors) {
        this.installDir = Objects.requireNonNull(installDir, "installDir cannot be null").toAbsolutePath();
        this.progressHandler = Objects.requireNonNull(progressHandler, "progressHandler cannot be null");
        this.mirrors = List.copyOf(Objects.requireNonNull(mirrors, "mirrors cannot be null"));
    }

    public void install() throws IOException, UnsupportedPlatformException {
        progressHandler.handleProgress(EnumProgress.LOCATING, EnumProgress.NO_ESTIMATION);
        Path parent = installationParent();
        Path lockPath = parent.resolve(installDir.getFileName() + ".installing.lock");
        try (RandomAccessFile lockAccess = new RandomAccessFile(lockPath.toFile(), "rw");
                FileLock ignored = lockAccess.getChannel().lock()) {
            if (!CefInstallationChecker.checkInstallation(installDir.toFile())) {
                installNativeBundle(parent);
            }
        }
    }

    private void installNativeBundle(Path parent) throws IOException, UnsupportedPlatformException {
        CefBuildInfo buildInfo = CefBuildInfo.fromClasspath();
        EnumPlatform platform = EnumPlatform.getCurrentPlatform();
        Path staging = Files.createTempDirectory(parent, installDir.getFileName() + ".staging-");
        Path download = staging.resolve("download.zip.temp");

        try {
            try (InputStream in = resolveNativeBundleStream(buildInfo, platform, download)) {
                progressHandler.handleProgress(EnumProgress.EXTRACTING, EnumProgress.NO_ESTIMATION);
                TarGzExtractor.extractTarGZ(staging.toFile(), in);
            }
            Files.deleteIfExists(download);
            completeInstallation(staging, platform);
            replaceInstallation(staging, installDir);
        } finally {
            cleanupStaging(staging);
        }
    }

    private Path installationParent() throws IOException {
        Path parent = installDir.getParent();
        Files.createDirectories(parent);
        return parent;
    }

    private InputStream resolveNativeBundleStream(CefBuildInfo info, EnumPlatform platform, Path download)
            throws IOException {
        InputStream classpathBundle = PackageClasspathStreamer.streamNatives(info, platform);
        if (classpathBundle != null) {
            return classpathBundle;
        }

        progressHandler.handleProgress(EnumProgress.DOWNLOADING, EnumProgress.NO_ESTIMATION);
        PackageDownloader.downloadNatives(info, platform, download.toFile(),
                progress -> progressHandler.handleProgress(EnumProgress.DOWNLOADING, progress), mirrors);
        return openTarGzFromArchive(download);
    }

    private void completeInstallation(Path destination, EnumPlatform platform) throws IOException {
        progressHandler.handleProgress(EnumProgress.INSTALL, EnumProgress.NO_ESTIMATION);
        if (platform.getOs().isMacOSX()) {
            UnquarantineUtil.unquarantine(destination);
        }
        Files.createFile(destination.resolve("install.lock"));
    }

    private static InputStream openTarGzFromArchive(Path archive) throws IOException {
        ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive));
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            if (!entry.isDirectory() && entry.getName().endsWith(".tar.gz")) {
                return zip;
            }
        }
        zip.close();
        throw new IOException("Downloaded artifact did not contain a .tar.gz archive");
    }

    static void replaceInstallation(Path staging, Path destination) throws IOException {
        Path parent = destination.toAbsolutePath().getParent();
        if (parent == null) {
            throw new IOException("Installation directory has no parent: " + destination);
        }
        Path backup = parent.resolve(destination.getFileName() + ".backup");
        FileUtils.deleteRecursivelyIfExists(backup);
        boolean hadExisting = Files.exists(destination);

        if (hadExisting) {
            move(destination, backup);
        }
        try {
            move(staging, destination);
        } catch (IOException installFailure) {
            if (hadExisting && Files.exists(backup)) {
                try {
                    move(backup, destination);
                } catch (IOException rollbackFailure) {
                    installFailure.addSuppressed(rollbackFailure);
                }
            }
            throw installFailure;
        }
        FileUtils.deleteRecursivelyIfExists(backup);
    }

    private static void move(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, destination);
        }
    }

    private static void cleanupStaging(Path staging) {
        try {
            FileUtils.deleteRecursivelyIfExists(staging);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not remove staging directory " + staging, e);
        }
    }
}
