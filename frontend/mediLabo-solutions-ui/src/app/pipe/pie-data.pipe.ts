import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'PieDataValue', standalone: true })
export class PieDataValue implements PipeTransform {

  transform(patients: any[], args?: string[]): number[] {
    if (!patients || patients.length === 0) return [0, 0, 0];

    if (args[0] === 'riskCounts') {
      return [
        patients.filter(p => p.riskLevel === 'BORDERLINE' || p.riskLevel === 'NONE').length,
        patients.filter(p => p.riskLevel === 'IN_DANGER').length,
        patients.filter(p => p.riskLevel === 'EARLY_ONSET').length
      ];
    }
    if (args[0] === 'genderCounts') {
      return [
        patients.filter(p => p.gender === 'MALE').length,
        patients.filter(p => p.gender === 'FEMALE').length
      ];
    }
    if (args[0] === 'ageCounts') {
      return this.getAgeGroups(patients);
    }
    return [0, 0, 0];
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
}