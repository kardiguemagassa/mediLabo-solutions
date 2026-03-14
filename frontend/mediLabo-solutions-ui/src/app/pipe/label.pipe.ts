import { Pipe, PipeTransform } from '@angular/core';
import { IPatient } from '../interface/patient';

@Pipe({ name: 'LabelValue', standalone: true })
export class LabelValue implements PipeTransform {
  transform(data: any[], args?: string[]): any {

    // dashboard
    if (args[0] === 'months') {
      return {
        categories: ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Jul', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc']
      };
    }
    if (args[0] === 'ageGroups') {
      return {
        categories: ['0-18', '19-30', '31-45', '46-60', '60+']
      };
    }

    const patients = data as IPatient[];
    if (args[0] === 'count') {
      return [...new Set(patients?.map(patient => patient.active ? 'Actif' : 'Inactif'))];
    }
    if (args[0] === 'type') {
      const types = [...new Set(patients?.map(patient => patient.active ? 'Actif' : 'Inactif'))];
      return types.map(type => [`${type}: ${this.patientTotalByType(patients, type)}`]);
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
      const data: number[] = [];
      for (const key in groups) {
        data.push(groups[key].length);
      }
      return { categories: data };
    }
    return {
      categories: [...new Set(patients?.map(patient => patient.active ? 'Actif' : 'Inactif'))],
      position: 'top',
      labels: { offsetY: -18 },
      axisBorder: { show: false },
      axisTicks: { show: false },
      crosshairs: { fill: { colors: ['#F44336', '#E91E63', '#9C27B0'] } },
      tooltip: { enabled: true, offsetY: -35 }
    };
  }

  private patientTotalByType = (patients: IPatient[], type: string) => patients.filter(patient => patient.active === (type === 'Actif')).length;

  private getWeekNumber = (date: Date | any): string => {
    const firstDayOfYear: Date | any = new Date(date.getFullYear(), 0, 1);
    const daysPassed = Math.floor((date - firstDayOfYear) / (24 * 60 * 60 * 1000));
    const weekNumber = Math.ceil((daysPassed + firstDayOfYear.getDay() + 1) / 7);
    return `Year ${date.getFullYear()} - W${weekNumber.toString().padStart(2, '0')}`;
  };
}