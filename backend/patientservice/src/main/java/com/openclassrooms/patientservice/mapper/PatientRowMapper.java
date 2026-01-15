package com.openclassrooms.patientservice.mapper;

import com.openclassrooms.patientservice.model.Patient;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RowMapper pour convertir ResultSet en Patient
 * Utilisé par JdbcClient
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-01-09
 */
public class PatientRowMapper implements RowMapper<Patient> {

    @Override
    public Patient mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Patient.builder()
                .patientId(rs.getLong("patient_id"))
                .patientUuid(rs.getString("patient_uuid"))
                .userUuid(rs.getString("user_uuid"))
                .dateOfBirth(rs.getDate("date_of_birth") != null ?
                        rs.getDate("date_of_birth").toLocalDate() : null)
                .gender(rs.getString("gender"))
                .bloodType(rs.getString("blood_type"))
                .heightCm(rs.getObject("height_cm") != null ?
                        rs.getInt("height_cm") : null)
                .weightKg(rs.getBigDecimal("weight_kg"))
                .allergies(rs.getString("allergies"))
                .chronicConditions(rs.getString("chronic_conditions"))
                .currentMedications(rs.getString("current_medications"))
                .emergencyContactName(rs.getString("emergency_contact_name"))
                .emergencyContactPhone(rs.getString("emergency_contact_phone"))
                .emergencyContactRelationship(rs.getString("emergency_contact_relationship"))
                .medicalRecordNumber(rs.getString("medical_record_number"))
                .insuranceNumber(rs.getString("insurance_number"))
                .insuranceProvider(rs.getString("insurance_provider"))
                .active(rs.getBoolean("active"))
                .createdAt(rs.getTimestamp("created_at") != null ?
                        rs.getTimestamp("created_at").toLocalDateTime() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null ?
                        rs.getTimestamp("updated_at").toLocalDateTime() : null)
                .build();
    }
}