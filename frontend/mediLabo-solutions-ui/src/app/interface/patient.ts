export interface IPatient {
  id: number;
  patientId: number;
  patientUuid: string;
  userUuid: string;
  content: string;
  date: string;

  dateOfBirth: string;
  age: number; 
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

  active?: boolean;

  createdAt: string;
  updatedAt: string;

  userInfo?: {
    firstName: string;
    lastName: string;
    email: string;
    imageUrl: string;
    address: string;
    phone: string;
  };
}

export type Patient = { 'patient:': IPatient };
export type Patients = { 'patients:': IPatient[] };
