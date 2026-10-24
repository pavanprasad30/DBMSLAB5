package com.college.diagnosticlab;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class ApiHandler implements HttpHandler {
    private static final String COLLECTION_PATH = "/api/records";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final PatientTestRecordRepository repository;

    public ApiHandler(PatientTestRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (JsonProcessingException exception) {
            sendMessage(exchange, 400, "Request body must be valid JSON.");
        } catch (IllegalArgumentException exception) {
            sendMessage(exchange, 400, exception.getMessage());
        } catch (SQLException exception) {
            System.err.println("Database request failed: " + exception.getMessage());
            if (exception.getErrorCode() == 1) {
                sendMessage(exchange, 409, "A record with this Patient ID already exists.");
            } else {
                sendMessage(exchange, 503,
                        "Oracle is unavailable or the table is missing. Check the database settings and run database/schema.sql.");
            }
        } catch (RuntimeException exception) {
            System.err.println("Unexpected API error: " + exception.getMessage());
            sendMessage(exchange, 500, "Unexpected server error. Check the backend terminal.");
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange) throws IOException, SQLException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (COLLECTION_PATH.equals(path) || (COLLECTION_PATH + "/").equals(path)) {
            if ("GET".equals(method)) {
                sendJson(exchange, 200, repository.findAll());
            } else if ("POST".equals(method)) {
                PatientTestRecord record = readRecord(exchange);
                String validationError = validate(record);
                if (validationError != null) {
                    sendMessage(exchange, 400, validationError);
                    return;
                }
                repository.add(record);
                sendMessage(exchange, 201, "Record added successfully.");
            } else {
                methodNotAllowed(exchange, "GET, POST");
            }
            return;
        }

        String itemPrefix = COLLECTION_PATH + "/";
        if (path.startsWith(itemPrefix)) {
            String patientId = path.substring(itemPrefix.length());
            if (patientId.isBlank() || patientId.contains("/")) {
                sendMessage(exchange, 404, "Record endpoint not found.");
                return;
            }
            if (!patientId.matches("[A-Za-z0-9_-]{1,20}")) {
                sendMessage(exchange, 400, "Patient ID must contain 1–20 letters, numbers, hyphens, or underscores.");
                return;
            }

            if ("PUT".equals(method)) {
                PatientTestRecord record = readRecord(exchange);
                String validationError = validate(record);
                if (validationError != null) {
                    sendMessage(exchange, 400, validationError);
                    return;
                }
                if (!patientId.equals(record.patientId)) {
                    sendMessage(exchange, 400, "Patient ID in the URL must match Patient ID in the form.");
                    return;
                }
                if (repository.update(record)) {
                    sendMessage(exchange, 200, "Record updated successfully.");
                } else {
                    sendMessage(exchange, 404, "No record found for Patient ID " + patientId + ".");
                }
            } else if ("DELETE".equals(method)) {
                if (repository.delete(patientId)) {
                    sendMessage(exchange, 200, "Record deleted successfully.");
                } else {
                    sendMessage(exchange, 404, "No record found for Patient ID " + patientId + ".");
                }
            } else {
                methodNotAllowed(exchange, "PUT, DELETE");
            }
            return;
        }

        sendMessage(exchange, 404, "API endpoint not found.");
    }

    private static PatientTestRecord readRecord(HttpExchange exchange) throws IOException {
        return JSON.readValue(exchange.getRequestBody().readAllBytes(), PatientTestRecord.class);
    }

    private static String validate(PatientTestRecord record) {
        if (record == null) {
            return "Request body must contain a patient/test record.";
        }

        record.patientId = clean(record.patientId);
        record.patientName = clean(record.patientName);
        record.gender = clean(record.gender);
        record.testName = clean(record.testName);
        record.testResult = clean(record.testResult);

        if (record.patientId == null || !record.patientId.matches("[A-Za-z0-9_-]{1,20}")) {
            return "Patient ID must contain 1–20 letters, numbers, hyphens, or underscores.";
        }
        if (record.patientName == null || record.patientName.length() > 100) {
            return "Patient Name is required and must be no more than 100 characters.";
        }
        if (record.age == null || record.age < 0 || record.age > 120) {
            return "Age must be a whole number from 0 to 120.";
        }
        if (record.gender == null || !record.gender.matches("Female|Male|Other")) {
            return "Select Female, Male, or Other for Gender.";
        }
        if (record.testName == null || record.testName.length() > 100) {
            return "Test Name is required and must be no more than 100 characters.";
        }
        if (record.testResult == null || record.testResult.length() > 2000) {
            return "Test Result is required and must be no more than 2000 characters.";
        }
        return null;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static void methodNotAllowed(HttpExchange exchange, String allowed) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowed);
        sendMessage(exchange, 405, "HTTP method not allowed for this endpoint.");
    }

    private static void sendMessage(HttpExchange exchange, int status, String text) throws IOException {
        sendJson(exchange, status, Map.of("message", text));
    }

    private static void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] response = JSON.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
    }
}
