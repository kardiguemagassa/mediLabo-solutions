import { Pipe, PipeTransform } from "@angular/core";
import { IPatient } from "../interface/patient";

@Pipe({ name: 'DataValue', standalone: true })
export class DataValue implements PipeTransform {
  
  transform(patients: IPatient[], args?: string[]) {

    if (!patients || patients.length === 0) return args[0] === 'highRisk' ? [] : 0;

    // dashboard
    if (args[0] === 'risk') {
      return (patients as any[]).filter(p => p.riskLevel === args[1]).length;
    }
    if (args[0] === 'highRisk') {
      return (patients as any[]).filter(p => p.riskLevel === 'EARLY_ONSET' || p.riskLevel === 'IN_DANGER');
    }

   
    if (args[0] === 'count') {
      const statuses = [...new Set(patients?.map(patient => patient.active ? 'Actif' : 'Inactif'))];
      return statuses.map(status => this.patientTotalByStatus(patients, status));
    }
    if (args[0] === 'status') {
      return this.patientTotalByStatus(patients, args[1]);
    }
    if (args[0] === 'type') {
      const types = [...new Set(patients?.map(patient => patient.allergies))];
      return types.map(type => this.patientTotalByType(patients, type));
    }
    if (args[0] === 'line') {
      const newPatients = patients?.map(patient => ({ ...patient, createdAt: new Date(patient.createdAt) }))
      .sort((patient1, patient2) => patient1.createdAt.getTime() - patient2.createdAt.getTime());
      const groups = newPatients?.reduce((acc, patient) => {
        const yearWeek = this.getWeekNumber(patient.createdAt);
        if (!acc[yearWeek]) { acc[yearWeek] = []; }
        acc[yearWeek].push(patient);
        return acc;
      }, {});
      const data: number[] = []
      for(const key in groups) {
        data.push(groups[key].length);
      }
      return [{ name: 'Patients', data }];
    }
    const statuses = [...new Set(patients?.map(patient => patient.active ? 'Actif' : 'Inactif'))];
    const data = statuses.map(status => this.patientTotalByStatus(patients, status));
    return [{ name: 'Patients', data }];
  }

  private patientTotalByStatus = (patients: IPatient[], status: string) => patients?.filter(patient => (patient.active ? 'Actif' : 'Inactif') === status).length;
  private patientTotalByType = (patients: IPatient[], type: string) => patients?.filter(patient => patient.allergies === type).length;

  private getWeekNumber = (date: Date | any) => {
    const firstDayOfYear: Date | any = new Date(date.getFullYear(), 0, 1);
    const daysPassed = Math.floor((date - firstDayOfYear) / (24 * 60 * 60 * 1000));
    const weekNumber = Math.ceil((daysPassed + firstDayOfYear.getDay() + 1) / 7);
    return `Year ${date.getFullYear()} - W${weekNumber.toString().padStart(2, '0')}`;
  }
}