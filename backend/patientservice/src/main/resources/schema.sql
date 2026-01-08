-- Base: medilabo_patients (PostgreSQL séparé)

CREATE TABLE IF NOT EXISTS patients (
    patient_id BIGSERIAL PRIMARY KEY,
    patient_uuid VARCHAR(40) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    birth_date DATE NOT NULL,
    gender VARCHAR(1) NOT NULL CHECK (gender IN ('M', 'F')),
    address VARCHAR(255) DEFAULT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_user_uuid VARCHAR(40),  -- UUID du praticien qui a créé (référence JWT)
    CONSTRAINT uq_patients_patient_uuid UNIQUE (patient_uuid)
);

-- Données de test (du projet OpenClassrooms)
INSERT INTO patients (patient_uuid, first_name, last_name, birth_date, gender) VALUES
('p001', 'Test', 'TestNone', '1966-12-31', 'F'),
('p002', 'Test', 'TestBorderline', '1945-06-24', 'M'),
('p003', 'Test', 'TestDanger', '2004-06-18', 'M'),
('p004', 'Test', 'TestEarlyOnset', '2002-06-28', 'F');