import { Component, computed, effect, inject } from '@angular/core';
import { AppStore } from '../../../store/app.store';
import { BarChartOptions, LineChartOptions, PieChartOptions } from '../../../interface/chartoption';
import { CommonModule } from '@angular/common';
import { NgApexchartsModule } from 'ng-apexcharts';
import { RouterLink } from '@angular/router';
import { DataValue } from '../../../pipe/data.pipe';
import { PieDataValue } from '../../../pipe/pie-data.pipe';
import { ChartDataValue } from '../../../pipe/chart-data.pipe';
import { PermissionService } from '../../../service/permission.service';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, RouterLink, DataValue, PieDataValue, ChartDataValue, NgApexchartsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  protected readonly store = inject(AppStore);
  protected readonly perm = inject(PermissionService);
  private dataLoaded = false;

  allPatients = computed(() => {
    const patients = this.store.allPatients() ?? [];
    const assessments = this.store.allAssessments() ?? [];
    return patients.map(patient => {
      const assessment = assessments.find(a => a.patientUuid === patient.patientUuid);
      return {
        ...patient,
        firstName: patient.userInfo?.firstName ?? '',
        lastName: patient.userInfo?.lastName ?? '',
        riskLevel: assessment?.riskLevel ?? 'NONE'
      };
    });
  });

  highRiskPatients = computed(() => {
    return this.allPatients().filter(
      (p: any) => p.riskLevel === 'EARLY_ONSET' || p.riskLevel === 'IN_DANGER'
    );
  });

  myRecord = computed(() => this.store.patient());

  myAssessment = computed(() => {
    const record = this.myRecord();
    if (!record) return null;
    const assessments = this.store.allAssessments() ?? [];
    return assessments.find(a => a.patientUuid === record.patientUuid) ?? null;
  });

  lineChartOptions: Partial<LineChartOptions>;
  barChartOptions: Partial<BarChartOptions>;
  columnChartOptions: Partial<BarChartOptions>;
  donutChartOptions: Partial<PieChartOptions>;
  pieChartOptions: Partial<PieChartOptions>;

  constructor() {

    // Charger les données UNE FOIS quand le profil est disponible
    effect(() => {
      const profile = this.store.profile();
      if (!profile || this.dataLoaded) return;
      this.dataLoaded = true;

      if (this.perm.canViewPatients()) {
        this.store.getAllPatients();
      }
      if (this.perm.canViewAssessments()) {
        this.store.getAllAssessments();
      }
      // Patient uniquement : charger son propre dossier
      if (!this.perm.isStaff()) {
        this.store.getMyPatientRecord();
        this.store.getAllAssessments();
      }
    });

    this.lineChartOptions = {
      chart: { height: 350, type: 'line', zoom: { enabled: false } },
      dataLabels: { enabled: false },
      stroke: { curve: 'straight' },
      grid: { row: { colors: ['#f3f3f3', 'transparent'], opacity: 0.5 } },
      xaxis: { categories: ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Jul', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'] }
    };
    this.pieChartOptions = {
      chart: { height: 400, type: 'pie' },
      legend: { position: 'bottom', fontWeight: 300 }
    };
    this.donutChartOptions = {
      chart: { height: 400, type: 'donut' },
      legend: { position: 'bottom', fontWeight: 300 }
    };
    this.barChartOptions = {
      chart: { height: 400, type: 'bar' },
      plotOptions: { bar: { dataLabels: { position: 'top' } } },
      dataLabels: { enabled: true, formatter: (value) => `${value}`, offsetY: -20, style: { fontSize: '12px', colors: ['#304758'] } },
      xaxis: { categories: ['0-18', '19-30', '31-45', '46-60', '60+'], position: 'bottom' },
      legend: { position: 'bottom', fontWeight: 300 }
    };
    this.columnChartOptions = {
      chart: { height: 400, type: 'bar' },
      plotOptions: { bar: { dataLabels: { position: 'top' }, columnWidth: '60%' } },
      dataLabels: {
        enabled: true,
        formatter: (value) => Number(value) > 0 ? `${value}` : '',
        offsetY: -20,
        style: { fontSize: '12px', colors: ['#304758'] }
      },
      xaxis: {
        categories: ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Jul', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'],
        position: 'bottom',
      },
      fill: { colors: ['#22c55e', '#eab308', '#ef4444'] },
        yaxis: {
          labels: { formatter: (val) => `${val}` }
        },
        legend: { position: 'bottom', fontWeight: 300 }
        
    };
  }

  getRiskBadgeClass(risk: string | null): string {
    switch (risk) {
      case 'NONE': return 'bg-emerald-100 text-emerald-700 border-emerald-200';
      case 'BORDERLINE': return 'bg-amber-100 text-amber-700 border-amber-200';
      case 'IN_DANGER': return 'bg-orange-100 text-orange-700 border-orange-200';
      case 'EARLY_ONSET': return 'bg-red-100 text-red-700 border-red-200';
      default: return 'bg-gray-100 text-gray-600 border-gray-200';
    }
  }

  getRiskLabel(risk: string | null): string {
    switch (risk) {
      case 'NONE': return 'Aucun risque';
      case 'BORDERLINE': return 'Risque limité';
      case 'IN_DANGER': return 'En danger';
      case 'EARLY_ONSET': return 'Apparition précoce';
      default: return 'Non évalué';
    }
  }
}