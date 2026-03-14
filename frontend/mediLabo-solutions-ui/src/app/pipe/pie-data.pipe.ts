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
    return [0, 0, 0];
  }
}