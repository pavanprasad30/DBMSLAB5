package com.college.diagnosticlab;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PatientTestRecordRepository {
    public List<PatientTestRecord> findAll() throws SQLException {
        String sql = "SELECT PATIENT_ID, PATIENT_NAME, AGE, GENDER, TEST_NAME, TEST_RESULT "
                + "FROM PATIENT_TEST_RECORDS ORDER BY PATIENT_ID";
        List<PatientTestRecord> records = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                PatientTestRecord record = new PatientTestRecord();
                record.patientId = results.getString("PATIENT_ID");
                record.patientName = results.getString("PATIENT_NAME");
                record.age = results.getInt("AGE");
                record.gender = results.getString("GENDER");
                record.testName = results.getString("TEST_NAME");
                record.testResult = results.getString("TEST_RESULT");
                records.add(record);
            }
        }
        return records;
    }

    public void add(PatientTestRecord record) throws SQLException {
        String sql = "INSERT INTO PATIENT_TEST_RECORDS "
                + "(PATIENT_ID, PATIENT_NAME, AGE, GENDER, TEST_NAME, TEST_RESULT) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.patientId);
            statement.setString(2, record.patientName);
            statement.setInt(3, record.age);
            statement.setString(4, record.gender);
            statement.setString(5, record.testName);
            statement.setString(6, record.testResult);
            statement.executeUpdate();
        }
    }

    public boolean update(PatientTestRecord record) throws SQLException {
        String sql = "UPDATE PATIENT_TEST_RECORDS SET PATIENT_NAME = ?, AGE = ?, GENDER = ?, "
                + "TEST_NAME = ?, TEST_RESULT = ? WHERE PATIENT_ID = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.patientName);
            statement.setInt(2, record.age);
            statement.setString(3, record.gender);
            statement.setString(4, record.testName);
            statement.setString(5, record.testResult);
            statement.setString(6, record.patientId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(String patientId) throws SQLException {
        String sql = "DELETE FROM PATIENT_TEST_RECORDS WHERE PATIENT_ID = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patientId);
            return statement.executeUpdate() > 0;
        }
    }
}
