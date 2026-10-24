package com.college.diagnosticlab;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        Path frontendRoot = locateFrontend();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/records", new ApiHandler(new PatientTestRecordRepository()));
        server.createContext("/", new StaticFileHandler(frontendRoot));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("Diagnostic Lab application is running at http://localhost:" + port);
        System.out.println("Frontend files: " + frontendRoot);
        System.out.println("Oracle is connected when DB_URL, DB_USERNAME, DB_PASSWORD and the table are configured.");
    }

    private static Path locateFrontend() throws IOException {
        String configuredDirectory = System.getenv("FRONTEND_DIR");
        if (configuredDirectory != null && !configuredDirectory.isBlank()) {
            Path configuredPath = Paths.get(configuredDirectory).toAbsolutePath().normalize();
            if (Files.isDirectory(configuredPath)) {
                return configuredPath;
            }
            throw new IOException("FRONTEND_DIR is not a directory: " + configuredPath);
        }

        Path fromProjectRoot = Paths.get("frontend").toAbsolutePath().normalize();
        if (Files.isDirectory(fromProjectRoot)) {
            return fromProjectRoot;
        }

        Path fromBackendDirectory = Paths.get("..", "frontend").toAbsolutePath().normalize();
        if (Files.isDirectory(fromBackendDirectory)) {
            return fromBackendDirectory;
        }

        throw new IOException("Could not find frontend/. Start Maven from the backend directory or set FRONTEND_DIR.");
    }
}
