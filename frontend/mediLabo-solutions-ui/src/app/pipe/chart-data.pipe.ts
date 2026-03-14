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
    return [];
  }

  private getAgeGroups(patients: any[]): number[] {
    const groups = [0, 0, 0, 0, 0];
    patients.forEach(p => {
      const age = this.calculateAge(p.dateOfBirth);
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

  private calculateAge(dateOfBirth: string): number {
    if (!dateOfBirth) return 0;
    const today = new Date();
    const birth = new Date(dateOfBirth);
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) age--;
    return age;
  }
}