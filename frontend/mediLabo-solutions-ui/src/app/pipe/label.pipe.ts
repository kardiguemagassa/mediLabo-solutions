import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'LabelValue', standalone: true })
export class LabelValue implements PipeTransform {
  transform(data: any[], args?: string[]): any {
    if (!data || data.length === 0) return { categories: [] };

    if (args[0] === 'risks') {
      return [...new Set(data?.map(p => p.riskLevel))];
    }
    if (args[0] === 'genders') {
      const genders = [...new Set(data?.map(p => p.gender))];
      return genders.map(g => [`${g === 'MALE' ? 'Homme' : 'Femme'}: ${data.filter(p => p.gender === g).length}`]);
    }
    if (args[0] === 'months') {
      return { categories: ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Jul', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'] };
    }
    if (args[0] === 'ageGroups') {
      return { categories: ['0-18', '19-30', '31-45', '46-60', '60+'] };
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
      return { categories: Object.keys(groups) };
    }
    return {
      categories: [...new Set(data?.map(p => p.riskLevel))],
      position: 'top',
      labels: { offsetY: -18 },
      axisBorder: { show: false },
      axisTicks: { show: false },
      crosshairs: {
        fill: { colors: ['#22c55e', '#eab308', '#ef4444'] }
      },
      tooltip: { enabled: true, offsetY: -35 }
    };
  }

  private getWeekNumber = (date: Date | any): string => {
    const firstDayOfYear: Date | any = new Date(date.getFullYear(), 0, 1);
    const daysPassed = Math.floor((date - firstDayOfYear) / (24 * 60 * 60 * 1000));
    const weekNumber = Math.ceil((daysPassed + firstDayOfYear.getDay() + 1) / 7);
    return `Year ${date.getFullYear()} - W${weekNumber.toString().padStart(2, '0')}`;
  };
}