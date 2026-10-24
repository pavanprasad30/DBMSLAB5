# Diagnostic Lab Test and Report Management System

A small college DBMS project with a plain HTML/CSS/JavaScript page, a Java HTTP backend, and Oracle database access through the Oracle JDBC Thin driver. It supports adding, updating, deleting, and viewing patient test records.

> **Connection note:** The source code is provided, but the application is not fully connected until you configure a running Oracle database, set the database environment variables, run the SQL script, and verify the connection locally. This workspace has no JDK, Maven, or Oracle instance, so the backend could not be compiled or tested against Oracle here. The page does not contain database credentials or pretend records.

## Project files

```text
frontend/
  index.html                 Form and records table
  styles.css                 White and light-blue styling
  app.js                     Calls the Java API; no localStorage or mock records
backend/
  pom.xml                    Maven project and JDBC/JSON dependencies
  src/main/java/com/college/diagnosticlab/
    Main.java                Starts the HTTP server and serves the frontend
    ApiHandler.java          GET, addition, updation, and deletion endpoints
    DatabaseConnection.java  Oracle JDBC Thin connection configuration
    PatientTestRecord.java   Record fields shared by API and JDBC code
    PatientTestRecordRepository.java  Prepared JDBC statements
    StaticFileHandler.java   Serves the plain frontend from the same origin
database/
  schema.sql                 Oracle table definition
```

## Requirements

- JDK 17 or later
- Apache Maven 3.8 or later
- An Oracle database/service that is reachable from this computer (for example, Oracle XE or Oracle Database Free)

Maven downloads the Oracle `ojdbc11` JDBC Thin driver and Jackson JSON library when you first build the backend.

## Set up the database

1. Start your Oracle database and identify its service name. Common local service names include `XEPDB1` (XE) and `FREEPDB1` (Oracle Database Free); use the service name for your own installation.
2. Connect to the schema/user that the application will use, for example with SQL Developer or SQL*Plus.
3. Run [`database/schema.sql`](database/schema.sql) as the same schema/user configured for the application, so that user owns the table.

The table has one row per Patient ID. The Patient ID is its primary key, so it is used to find records for updation and deletion.

## Configure and run

Set these variables in the terminal that will start the backend. Keep the password out of frontend files and source control.

**macOS / Linux (bash):**

```bash
export DB_URL='jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
export DB_USERNAME='your_oracle_schema'
export DB_PASSWORD='your_database_password'
cd backend
mvn clean compile exec:java
```

Change `FREEPDB1`, host, and port to match your Oracle installation. For Oracle XE, the URL may instead be `jdbc:oracle:thin:@//localhost:1521/XEPDB1`.

**Windows PowerShell:**

```powershell
$env:DB_URL = 'jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
$env:DB_USERNAME = 'your_oracle_schema'
$env:DB_PASSWORD = 'your_database_password'
cd backend
mvn clean compile exec:java
```

The backend also reads Java system properties `db.url`, `db.username`, and `db.password` if supplied by your Java launch setup. Keep credentials out of frontend files and source control; avoid placing real passwords in commands that may be saved in shell history. The HTTP port defaults to `8080` and can be changed with the `PORT` environment variable.

Open **http://localhost:8080** in a browser. Use the page's three buttons to add, update, or delete records. The table is loaded from Oracle when the page opens and refreshed after each successful change. Do not open `index.html` directly as a file; the Java server serves it and the API from the same origin.

## What the backend does

- `GET /api/records` — reads records from Oracle for the table.
- `POST /api/records` — addition.
- `PUT /api/records/{patientId}` — updation of the record with that Patient ID.
- `DELETE /api/records/{patientId}` — deletion of the record with that Patient ID.

The Java code uses prepared SQL statements. The Java server can start even when Oracle is not configured, so the page can explain the connection problem; database requests will return an error until the URL, credentials, Oracle service, and table are correct.

## If it does not connect

- Confirm the Oracle listener and service are running and that `DB_URL` names the correct service.
- Confirm `DB_USERNAME` and `DB_PASSWORD` can connect to that database and that `PATIENT_TEST_RECORDS` exists in that user's schema.
- Check the backend terminal for the reported database error. The web page also shows a setup/connection status without displaying credentials.
- Ensure JDK 17+ and Maven are available with `java -version` and `mvn -version`.
