export interface IPatientDetail {
    id: number;
    name: string;
    age: number;
    gender: string;
    diagnosis: string;
    admissionDate: string;
    dischargeDate?: string;
    attendingPhysician: string;
    roomNumber: string;
    status: 'admitted' | 'discharged' | 'under observation';
}