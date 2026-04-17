import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'DataValue', standalone: true })
export class DataValue implements PipeTransform {
  transform(patients: any[], args?: string[]) {
    if (!patients || patients.length === 0) return 0;

    if (args[0] === 'risk') {
      return this.patientTotalByRisk(patients, args[1]);
    }
    if (args[0] === 'highRisk') {
      return patients.filter(p => p.riskLevel === 'EARLY_ONSET' || p.riskLevel === 'IN_DANGER').length;
    }
    if (args[0] === 'gender') {
      return this.patientTotalByGender(patients, args[1]);
    }
    if (args[0] === 'riskCounts') {
      const risks = [...new Set(patients?.map(p => p.riskLevel))];
      return risks.map(risk => this.patientTotalByRisk(patients, risk));
    }
    if (args[0] === 'sortByDate') {
      return patients
        .map(p => ({ ...p, createdAt: new Date(p.createdAt) }))
        .sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime());
    }
    if (args[0] === 'line') {
      const sorted = patients
        .map(p => ({ ...p, createdAt: new Date(p.createdAt) }))
        .sort((a, b) => a.createdAt.getTime() - b.createdAt.getTime());
      const groups = sorted.reduce((acc, p) => {
        const yearWeek = this.getWeekNumber(p.createdAt);
        if (!acc[yearWeek]) { acc[yearWeek] = []; }
        acc[yearWeek].push(p);
        return acc;
      }, {});
      const data: number[] = [];
      for (const key in groups) {
        data.push(groups[key].length);
      }
      return [{ name: 'Patients', data }];
    }
    const risks = [...new Set(patients?.map(p => p.riskLevel))];
    return risks.map(risk => this.patientTotalByRisk(patients, risk));
  }

  private patientTotalByRisk = (patients: any[], risk: string) => patients?.filter(p => p.riskLevel === risk).length;
  private patientTotalByGender = (patients: any[], gender: string) => patients?.filter(p => p.gender === gender).length;

  private getWeekNumber = (date: Date | any): string => {
    const firstDayOfYear: Date | any = new Date(date.getFullYear(), 0, 1);
    const daysPassed = Math.floor((date - firstDayOfYear) / (24 * 60 * 60 * 1000));
    const weekNumber = Math.ceil((daysPassed + firstDayOfYear.getDay() + 1) / 7);
    return `Year ${date.getFullYear()} - W${weekNumber.toString().padStart(2, '0')}`;
  };
}