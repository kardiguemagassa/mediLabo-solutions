import { Pipe, PipeTransform } from '@angular/core';
import { IAssessment } from '../interface/assessment';

@Pipe({ name: 'ChartDataValue', standalone: true })
export class ChartDataValue implements PipeTransform {

  transform(data: any[], args?: string[]): { name: string; data: number[] }[] {
    if (!data || data.length === 0) return [];

    if (args[0] === 'ageGroups') {
      return [{ name: 'Patients', data: this.getAgeGroups(data) }];
    }
    if (args[0] === 'riskColumns') {
      return this.getRiskColumns(data);
    }
    if (args[0] === 'trends') {
      return this.getTrends(data as IAssessment[]);
    }
    if (args[0] === 'genderByMonth') {
      return this.getGenderByMonth(data);
    }
    if (args[0] === 'line') {
      const sorted = data
        .map(p => ({ ...p, createdAt: new Date(p.createdAt) }))
        .sort((a, b) => a.createdAt.getTime() - b.createdAt.getTime());
      const groups = sorted.reduce((acc, p) => {
        const yearWeek = this.getWeekNumber(p.createdAt);
        if (!acc[yearWeek]) { acc[yearWeek] = []; }
        acc[yearWeek].push(p);
        return acc;
      }, {});
      const values: number[] = [];
      for (const key in groups) {
        values.push(groups[key].length);
      }
      return [{ name: 'Patients', data: values }];
    }
    return [];
  }

  private getAgeGroups(patients: any[]): number[] {
    const groups = [0, 0, 0, 0, 0];
    patients.forEach(p => {
      const age = p.age ?? 0; 
      if (age <= 18) groups[0]++;
      else if (age <= 30) groups[1]++;
      else if (age <= 45) groups[2]++;
      else if (age <= 60) groups[3]++;
      else groups[4]++;
    });
    return groups;
  }

  private getRiskColumns(patients: any[]): { name: string; data: number[] }[] {
    const months = Array.from({ length: 12 }, () => ({ borderline: 0, inDanger: 0, earlyOnset: 0 }));
    patients.forEach(p => {
      const month = new Date(p.createdAt).getMonth();
      if (p.riskLevel === 'BORDERLINE' || p.riskLevel === 'NONE') months[month].borderline++;
      else if (p.riskLevel === 'IN_DANGER') months[month].inDanger++;
      else if (p.riskLevel === 'EARLY_ONSET') months[month].earlyOnset++;
    });
    return [
      { name: 'Faible', data: months.map(m => m.borderline) },
      { name: 'Modéré', data: months.map(m => m.inDanger) },
      { name: 'Élevé', data: months.map(m => m.earlyOnset) }
    ];
  }

  private getTrends(assessments: IAssessment[]): { name: string; data: number[] }[] {
    const months = Array.from({ length: 12 }, () => 0);
    assessments.forEach(a => {
      const month = new Date(a.assessedAt).getMonth();
      months[month]++;
    });
    return [{ name: 'Évaluations', data: months }];
  }

  private getGenderByMonth(patients: any[]): { name: string; data: number[] }[] {
    const months = Array.from({ length: 12 }, () => ({ male: 0, female: 0 }));
    patients.forEach(p => {
      const month = new Date(p.createdAt).getMonth();
      if (p.gender === 'MALE') months[month].male++;
      else if (p.gender === 'FEMALE') months[month].female++;
    });
    return [
      { name: 'Hommes', data: months.map(m => m.male) },
      { name: 'Femmes', data: months.map(m => m.female) }
    ];
  }

  private getWeekNumber = (date: Date | any): string => {
    const firstDayOfYear: Date | any = new Date(date.getFullYear(), 0, 1);
    const daysPassed = Math.floor((date - firstDayOfYear) / (24 * 60 * 60 * 1000));
    const weekNumber = Math.ceil((daysPassed + firstDayOfYear.getDay() + 1) / 7);
    return `Year ${date.getFullYear()} - W${weekNumber.toString().padStart(2, '0')}`;
  };
}