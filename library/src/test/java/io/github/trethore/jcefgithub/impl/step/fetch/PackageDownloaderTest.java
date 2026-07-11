package io.github.trethore.jcefgithub.impl.step.fetch;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackageDownloaderTest {
    @TempDir Path tempDir;

    @Test
    void verifiesExpectedSha256() throws Exception {
        Path artifact = Files.writeString(tempDir.resolve("artifact.jar"), "native artifact");
        String expected = "9eaa01bd3b56258e0e41821a383e1e6282090e0f355fdf6c10883b38c612e8a8";

        assertDoesNotThrow(() -> PackageDownloader.verifySha256(artifact.toFile(), expected));
        assertThrows(IOException.class, () -> PackageDownloader.verifySha256(artifact.toFile(), "0".repeat(64)));
    }

    @Test
    void fallsBackAfterCorruptMirrorAndReportsProgress() throws Exception {
        byte[] corrupt = "corrupt".getBytes();
        byte[] expected = "expected native artifact".getBytes();
        HttpServer server = server(corrupt, expected);
        try {
            Path destination = tempDir.resolve("artifact.jar");
            List<Float> progress = new ArrayList<>();
            PackageDownloader.downloadFromMirrors(
                    List.of(url(server, "/corrupt"), url(server, "/good")),
                    destination.toFile(), progress::add, sha256(expected));

            assertArrayEquals(expected, Files.readAllBytes(destination));
            assertFalse(progress.isEmpty());
            assertEquals(0f, progress.get(0));
            assertEquals(100f, progress.get(progress.size() - 1));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void removesPartialFileWhenAllMirrorsFail() throws Exception {
        byte[] corrupt = "corrupt".getBytes();
        HttpServer server = server(corrupt, "unused".getBytes());
        try {
            Path destination = tempDir.resolve("artifact.jar");
            IOException error = assertThrows(IOException.class,
                    () -> PackageDownloader.downloadFromMirrors(
                            List.of(url(server, "/missing"), url(server, "/corrupt")),
                            destination.toFile(), ignored -> { }, "0".repeat(64)));

            assertTrue(error.getMessage().contains("None of the supplied mirrors"));
            assertFalse(Files.exists(destination));
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer server(byte[] corrupt, byte[] expected) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/corrupt", exchange -> {
            exchange.sendResponseHeaders(200, corrupt.length);
            exchange.getResponseBody().write(corrupt);
            exchange.close();
        });
        server.createContext("/good", exchange -> {
            exchange.sendResponseHeaders(200, expected.length);
            exchange.getResponseBody().write(expected);
            exchange.close();
        });
        server.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static String url(HttpServer server, String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private static String sha256(byte[] value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }
}
