package com.college.diagnosticlab;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class Main {
    private static final String RECORDS_PATH = "/api/records";
    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        Path frontend = findFrontend();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(RECORDS_PATH, Main::handleApi);
        server.createContext("/", exchange -> serveFrontend(exchange, frontend));
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Open http://localhost:" + port + " in your browser.");
    }

    private static void handleApi(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if (path.equals(RECORDS_PATH) || path.equals(RECORDS_PATH + "/")) {
                if (method.equals("GET")) {
                    sendJson(exchange, 200, getRecords());
                } else if (method.equals("POST")) {
                    insert(readRecord(exchange));
                    sendMessage(exchange, 201, "Record added successfully.");
                } else {
                    methodNotAllowed(exchange, "GET, POST");
                }
                return;
            }

            String itemPrefix = RECORDS_PATH + "/";
            if (path.startsWith(itemPrefix)) {
                String patientId = path.substring(itemPrefix.length());
                if (!patientId.matches("[A-Za-z0-9_-]{1,20}")) {
                    sendMessage(exchange, 400, "Enter a valid Patient ID.");
                } else if (method.equals("PUT")) {
                    LabRecord record = readRecord(exchange);
                    if (!patientId.equals(record.patientId)) {
                        throw new IllegalArgumentException("Patient ID in the URL must match the form.");
                    }
                    if (update(record)) {
                        sendMessage(exchange, 200, "Record updated successfully.");
                    } else {
                        sendMessage(exchange, 404, "Patient ID was not found.");
                    }
                } else if (method.equals("DELETE")) {
                    if (delete(patientId)) {
                        sendMessage(exchange, 200, "Record deleted successfully.");
                    } else {
                        sendMessage(exchange, 404, "Patient ID was not found.");
                    }
                } else {
                    methodNotAllowed(exchange, "PUT, DELETE");
                }
                return;
            }
            sendMessage(exchange, 404, "API endpoint not found.");
        } catch (JsonProcessingException exception) {
            sendMessage(exchange, 400, "Enter valid record details.");
        } catch (IllegalArgumentException exception) {
            sendMessage(exchange, 400, exception.getMessage());
        } catch (SQLException exception) {
            System.err.println("Oracle error: " + exception.getMessage());
            if (exception.getErrorCode() == 1) {
                sendMessage(exchange, 409, "That Patient ID already exists.");
            } else {
                sendMessage(exchange, 503, "Oracle is not available. Check credentials and run database/schema.sql.");
            }
        } catch (RuntimeException exception) {
            System.err.println("Server error: " + exception.getMessage());
            sendMessage(exchange, 500, "Unexpected server error. Check the backend terminal.");
        } finally {
            exchange.close();
        }
    }

    private static LabRecord readRecord(HttpExchange exchange) throws IOException {
        LabRecord record = JSON.readValue(exchange.getRequestBody().readAllBytes(), LabRecord.class);
        if (record == null) {
            throw new IllegalArgumentException("Enter record details.");
        }
        record.patientId = required(record.patientId, "Patient ID", 20);
        if (!record.patientId.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Use letters, numbers, hyphens, or underscores for Patient ID.");
        }
        record.name = required(record.name, "Name", 100);
        if (record.age == null || record.age < 0 || record.age > 120) {
            throw new IllegalArgumentException("Age must be from 0 to 120.");
        }
        record.testName = required(record.testName, "Test Name", 100);
        record.testResult = required(record.testResult, "Test Result", 2000);
        return record;
    }

    private static String required(String value, String label, int maxLength) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > maxLength) {
            throw new IllegalArgumentException(label + " is required (maximum " + maxLength + " characters).");
        }
        return value.trim();
    }

    private static List<LabRecord> getRecords() throws SQLException {
        List<LabRecord> records = new ArrayList<>();
        String sql = "SELECT PATIENT_ID, PATIENT_NAME, AGE, TEST_NAME, TEST_RESULT "
                + "FROM PATIENT_TEST_RECORDS ORDER BY PATIENT_ID";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                LabRecord record = new LabRecord();
                record.patientId = results.getString("PATIENT_ID");
                record.name = results.getString("PATIENT_NAME");
                record.age = results.getInt("AGE");
                record.testName = results.getString("TEST_NAME");
                record.testResult = results.getString("TEST_RESULT");
                records.add(record);
            }
        }
        return records;
    }

    private static void insert(LabRecord record) throws SQLException {
        String sql = "INSERT INTO PATIENT_TEST_RECORDS "
                + "(PATIENT_ID, PATIENT_NAME, AGE, TEST_NAME, TEST_RESULT) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.patientId);
            statement.setString(2, record.name);
            statement.setInt(3, record.age);
            statement.setString(4, record.testName);
            statement.setString(5, record.testResult);
            statement.executeUpdate();
        }
    }

    private static boolean update(LabRecord record) throws SQLException {
        String sql = "UPDATE PATIENT_TEST_RECORDS SET PATIENT_NAME=?, AGE=?, TEST_NAME=?, TEST_RESULT=? WHERE PATIENT_ID=?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.name);
            statement.setInt(2, record.age);
            statement.setString(3, record.testName);
            statement.setString(4, record.testResult);
            statement.setString(5, record.patientId);
            return statement.executeUpdate() > 0;
        }
    }

    private static boolean delete(String patientId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM PATIENT_TEST_RECORDS WHERE PATIENT_ID=?")) {
            statement.setString(1, patientId);
            return statement.executeUpdate() > 0;
        }
    }

    private static void methodNotAllowed(HttpExchange exchange, String allowed) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowed);
        sendMessage(exchange, 405, "Method not allowed.");
    }

    private static void sendMessage(HttpExchange exchange, int status, String text) throws IOException {
        sendJson(exchange, status, Map.of("message", text));
    }

    private static void sendJson(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] body = JSON.writeValueAsBytes(value);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    private static void serveFrontend(HttpExchange exchange, Path frontend) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("GET") && !exchange.getRequestMethod().equals("HEAD")) {
                sendText(exchange, 405, "Method not allowed.");
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            Path file = frontend.resolve(path.substring(1)).normalize();
            if (!file.startsWith(frontend) || !Files.isRegularFile(file)) {
                sendText(exchange, 404, "Page not found.");
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", contentType(file));
            byte[] content = Files.readAllBytes(file);
            if (exchange.getRequestMethod().equals("HEAD")) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(200, content.length);
                exchange.getResponseBody().write(content);
            }
        } finally {
            exchange.close();
        }
    }

    private static void sendText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        if (exchange.getRequestMethod().equals("HEAD")) exchange.sendResponseHeaders(status, -1);
        else {
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
        }
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) return "text/html; charset=UTF-8";
        if (name.endsWith(".css")) return "text/css; charset=UTF-8";
        if (name.endsWith(".js")) return "text/javascript; charset=UTF-8";
        return "application/octet-stream";
    }

    private static Path findFrontend() throws IOException {
        String folder = System.getenv("FRONTEND_DIR");
        if (folder != null && !folder.isBlank()) {
            Path configured = Paths.get(folder).toAbsolutePath().normalize();
            if (Files.isDirectory(configured)) return configured;
            throw new IOException("FRONTEND_DIR is not a folder: " + configured);
        }
        Path local = Paths.get("frontend").toAbsolutePath().normalize();
        if (Files.isDirectory(local)) return local;
        Path parent = Paths.get("..", "frontend").toAbsolutePath().normalize();
        if (Files.isDirectory(parent)) return parent;
        throw new IOException("Could not find frontend/. Start from backend/ or set FRONTEND_DIR.");
    }

    public static class LabRecord {
        public String patientId;
        public String name;
        public Integer age;
        public String testName;
        public String testResult;
    }
}
