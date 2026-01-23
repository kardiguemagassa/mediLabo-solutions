package com.openclassrooms.patientservice.query;

/**
 * Requêtes SQL natives pour Patient
 * Toutes les requêtes sont définies ici pour faciliter la maintenance
 *
 * Bonnes pratiques :
 * - Pas de SELECT *
 * - Colonnes explicites
 * - Base SELECT réutilisable
 *
 * @author FirstName LastName
 * @version 1.1
 * @since 2026-01-22
 */
public final class PatientQuery {

//    private PatientQuery() {
//        // Utility class
//    }


    //SELECT
    public static final String BASE_PATIENT_SELECT =
            """
            SELECT
                patient_id,
                patient_uuid,
                user_uuid,
                date_of_birth,
                gender,
                blood_type,
                height_cm,
                weight_kg,
                allergies,
                chronic_conditions,
                current_medications,
                emergency_contact_name,
                emergency_contact_phone,
                emergency_contact_relationship,
                medical_record_number,
                insurance_number,
                insurance_provider,
                active,
                created_at,
                updated_at
            FROM patients
            """;


    //INSERT
    public static final String INSERT_PATIENT_QUERY =
            """
            INSERT INTO patients (
                patient_uuid,
                user_uuid,
                date_of_birth,
                gender,
                blood_type,
                height_cm,
                weight_kg,
                allergies,
                chronic_conditions,
                current_medications,
                emergency_contact_name,
                emergency_contact_phone,
                emergency_contact_relationship,
                medical_record_number,
                insurance_number,
                insurance_provider,
                active
            ) VALUES (
                :patientUuid,
                :userUuid,
                :dateOfBirth,
                :gender,
                :bloodType,
                :heightCm,
                :weightKg,
                :allergies,
                :chronicConditions,
                :currentMedications,
                :emergencyContactName,
                :emergencyContactPhone,
                :emergencyContactRelationship,
                :medicalRecordNumber,
                :insuranceNumber,
                :insuranceProvider,
                :active
            )
            RETURNING
                patient_id,
                patient_uuid,
                user_uuid,
                date_of_birth,
                gender,
                blood_type,
                height_cm,
                weight_kg,
                allergies,
                chronic_conditions,
                current_medications,
                emergency_contact_name,
                emergency_contact_phone,
                emergency_contact_relationship,
                medical_record_number,
                insurance_number,
                insurance_provider,
                active,
                created_at,
                updated_at
            """;


    // UPDATE
    public static final String UPDATE_PATIENT_QUERY =
            """
            UPDATE patients SET
                date_of_birth = :dateOfBirth,
                gender = :gender,
                blood_type = :bloodType,
                height_cm = :heightCm,
                weight_kg = :weightKg,
                allergies = :allergies,
                chronic_conditions = :chronicConditions,
                current_medications = :currentMedications,
                emergency_contact_name = :emergencyContactName,
                emergency_contact_phone = :emergencyContactPhone,
                emergency_contact_relationship = :emergencyContactRelationship,
                insurance_number = :insuranceNumber,
                insurance_provider = :insuranceProvider,
                updated_at = CURRENT_TIMESTAMP
            WHERE patient_uuid = :patientUuid
              AND active = true
            RETURNING
                patient_id,
                patient_uuid,
                user_uuid,
                date_of_birth,
                gender,
                blood_type,
                height_cm,
                weight_kg,
                allergies,
                chronic_conditions,
                current_medications,
                emergency_contact_name,
                emergency_contact_phone,
                emergency_contact_relationship,
                medical_record_number,
                insurance_number,
                insurance_provider,
                active,
                created_at,
                updated_at
            """;

    //SELECT
    public static final String SELECT_PATIENT_BY_UUID_QUERY =
            BASE_PATIENT_SELECT + """
        WHERE patient_uuid = :patientUuid
          AND active = true
        """;

    public static final String SELECT_PATIENT_BY_USER_UUID_QUERY =
            BASE_PATIENT_SELECT + """
        WHERE user_uuid = :userUuid
          AND active = true
        """;

    public static final String SELECT_PATIENT_BY_MEDICAL_RECORD_NUMBER_QUERY =
            BASE_PATIENT_SELECT + """
        WHERE medical_record_number = :medicalRecordNumber
          AND active = true
        """;

    public static final String SELECT_ALL_ACTIVE_PATIENTS_QUERY =
            BASE_PATIENT_SELECT + """
        WHERE active = true
        ORDER BY created_at DESC
        """;

    public static final String SELECT_PATIENTS_BY_BLOOD_TYPE_QUERY =
            BASE_PATIENT_SELECT + """
        WHERE blood_type = :bloodType
          AND active = true
        ORDER BY created_at DESC
        """;


    //EXISTS / COUNT
    public static final String EXISTS_BY_USER_UUID_QUERY =
            """
            SELECT EXISTS (
                SELECT 1
                FROM patients
                WHERE user_uuid = :userUuid
                  AND active = true
            )
            """;

    public static final String COUNT_ACTIVE_PATIENTS_QUERY =
            """
            SELECT COUNT(*)
            FROM patients
            WHERE active = true
            """;


    //SOFT DELETE
    public static final String SOFT_DELETE_PATIENT_QUERY =
            """
            UPDATE patients
            SET active = false,
                updated_at = CURRENT_TIMESTAMP
            WHERE patient_uuid = :patientUuid
            """;
}
