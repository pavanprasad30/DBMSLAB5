# Diagnostic Lab Test and Report Management System

A beginner-friendly HTML, CSS, JavaScript, JDBC, and Oracle project. It stores real records in Oracle; the page does not use mock data or browser storage.

> **Not connected yet:** Configure your Oracle database and Java environment below, then test the connection. This workspace has no JDK, Maven, or Oracle instance, so I could not compile or test the backend here.

## Files

- `frontend/` — webpage, styling, and calls to the Java API
- `backend/src/main/java/com/college/diagnosticlab/Main.java` — small API and HTTP server
- `backend/src/main/java/com/college/diagnosticlab/DatabaseConnection.java` — Oracle JDBC Thin connection
- `backend/pom.xml` — JDBC driver and JSON dependency
- `database/schema.sql` — table creation command

## Setup

You need JDK 17+, Maven, and a running Oracle database. Connect to the Oracle schema the app will use and run `database/schema.sql`.

Set these environment variables in a terminal, using your own Oracle service and credentials (do not put real credentials in frontend files):

```bash
export DB_URL='jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
export DB_USERNAME='your_schema_user'
export DB_PASSWORD='your_password'
cd backend
mvn clean compile exec:java
```

For Oracle XE, the service in the URL may be `XEPDB1`. Open **http://localhost:8080**. The Java server also serves the frontend, so do not open `index.html` directly.

The table has one record per Patient ID. The Java code uses `PreparedStatement` for database operations. The page loads records from Oracle and uses these endpoints:

- `GET /api/records` — list records
- `POST /api/records` — Addition
- `PUT /api/records/{patientId}` — Updation
- `DELETE /api/records/{patientId}` — Deletion
