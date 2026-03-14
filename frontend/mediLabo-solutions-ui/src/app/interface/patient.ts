export interface IPatient {
  patientId: number;
  patientUuid: string;
  userUuid: string;

  dateOfBirth: string;
  gender: string;
  bloodType: string;

  heightCm: number;
  weightKg: number;

  allergies: string;
  chronicConditions: string;
  currentMedications: string;

  emergencyContactName: string;
  emergencyContactPhone: string;
  emergencyContactRelationship: string;

  medicalRecordNumber: string;
  insuranceNumber: string;
  insuranceProvider: string;
  insurancePolicyNumber: string;

  active: boolean;

  createdAt: string;
  updatedAt: string;
}

export type Patient = {'patient:': IPatient};
export type Patients = {'patients:': IPatient[]};