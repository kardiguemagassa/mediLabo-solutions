-- MediLabo Solutions - Patient Service Database Schema
-- Author: FirstName LastName
-- Date: January 09, 2026
-- Version: 1.0


-- General Rules ---
-- Use underscore_names instead of CamelCase --
-- Table names should be plural --
-- Spell out id fields (item_id instead of id) --
-- Don't use ambiguous column names --
-- Name foreign key columns the same as the columns they refer to --
-- Use caps for all SQL keywords --


BEGIN;

CREATE TABLE IF NOT EXISTS patients (
    patient_id BIGSERIAL PRIMARY KEY,
    patient_uuid VARCHAR(40) NOT NULL UNIQUE,
    user_uuid VARCHAR(40) NOT NULL UNIQUE,  -- Référence Authorization Server
    -- Données médicales
    date_of_birth DATE NOT NULL, -- User Story: voir date de naissance
    gender VARCHAR(10), -- User Story: voir genre (optionnel)
    blood_type VARCHAR(5),
    height_cm INTEGER, -- calcul IMC 
    weight_kg NUMERIC(5, 2),-- calcul IMC
    allergies TEXT,
    chronic_conditions TEXT,
    current_medications TEXT,
    -- Contact d'urgence
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    emergency_contact_relationship VARCHAR(50),
    -- Assurance et dossier
    medical_record_number VARCHAR(20) UNIQUE,
    insurance_number VARCHAR(50),
    insurance_provider VARCHAR(100),
    -- Métadonnées
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_patients_patient_uuid UNIQUE (patient_uuid),
    CONSTRAINT uq_patients_user_uuid UNIQUE (user_uuid),
    CONSTRAINT uq_patients_medical_record_number UNIQUE (medical_record_number)
);

-- INDEXES pour améliorer les performances
CREATE INDEX IF NOT EXISTS idx_patients_user_uuid ON patients(user_uuid);
CREATE INDEX IF NOT EXISTS idx_patients_active ON patients(active);
CREATE INDEX IF NOT EXISTS idx_patients_date_of_birth ON patients(date_of_birth);
CREATE INDEX IF NOT EXISTS idx_patients_blood_type ON patients(blood_type);
CREATE INDEX IF NOT EXISTS idx_patients_medical_record_number ON patients(medical_record_number);


-- TRIGGER: Mise à jour automatique de updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_patients_updated_at ON patients;

CREATE TRIGGER update_patients_updated_at
    BEFORE UPDATE ON patients
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- COMMENT
COMMENT ON TABLE patients IS 'Table des dossiers patients - Données médicales uniquement';
COMMENT ON COLUMN patients.patient_uuid IS 'UUID unique du patient (exposé dans API)';
COMMENT ON COLUMN patients.user_uuid IS 'UUID de l''utilisateur (référence Authorization Server)';
COMMENT ON COLUMN patients.medical_record_number IS 'Numéro de dossier médical unique (format: MED-YYYY-XXXXXX)';
COMMENT ON COLUMN patients.active IS 'Patient actif (false = soft delete)';

-- VÉRIFICATION 
SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';

-- Compter les patients
SELECT COUNT(*) FROM patients;

COMMIT;