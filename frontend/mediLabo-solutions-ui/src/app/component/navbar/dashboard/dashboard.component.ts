import { Component, computed, inject } from '@angular/core';
import { AppStore } from '../../../store/app.store';
import { BarChartOptions, LineChartOptions, PieChartOptions } from '../../../interface/chartoption';

import { CommonModule } from '@angular/common';
import { NgApexchartsModule } from 'ng-apexcharts';
import { RouterLink } from '@angular/router';
import { LabelValue } from '../../../pipe/label.pipe';
import { DataValue } from '../../../pipe/data.pipe';
import { PieDataValue } from '../../../pipe/pie-data.pipe';
import { ChartDataValue } from '../../../pipe/chart-data.pipe';


@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, RouterLink, DataValue, PieDataValue, ChartDataValue, NgApexchartsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  protected store = inject(AppStore);

  // Signal computed qui enrichit les patients avec les données user + assessment
  allPatients = computed(() => {
    const patients = this.store.allPatients() ?? [];
    const users = this.store.users() ?? [];
    const assessments = this.store.allAssessments() ?? [];

    return patients.map(patient => {
      const user = users.find(u => u.userUuid === patient.userUuid);
      const assessment = assessments.find(a => a.patientUuid === patient.patientUuid);
      return {
        ...patient,
        firstName: user?.firstName ?? '',
        lastName: user?.lastName ?? '',
        riskLevel: assessment?.riskLevel ?? 'NONE'
      };
    });
  });

  highRiskPatients = computed(() => {
    return this.allPatients().filter(
      (p: any) => p.riskLevel === 'EARLY_ONSET' || p.riskLevel === 'IN_DANGER'
    );
  });

  lineChartOptions: Partial<LineChartOptions>;
  barChartOptions: Partial<BarChartOptions>;
  columnChartOptions: Partial<BarChartOptions>;
  donutChartOptions: Partial<PieChartOptions>;
  pieChartOptions: Partial<PieChartOptions>;

  constructor() {
    this.lineChartOptions = {
      chart: {
        height: 350,
        type: "line",
        zoom: { enabled: false }
      },
      dataLabels: { enabled: false },
      stroke: { curve: "straight" },
      grid: {
        row: { colors: ["#f3f3f3", "transparent"], opacity: 0.5 }
      },
      xaxis: {
        categories: ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Jul', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc']
      }
    };
    this.pieChartOptions = {
      chart: {
        height: 400,
        type: 'pie',
      },
      legend: { position: 'bottom', fontWeight: 300 }
    };
    this.donutChartOptions = {
      chart: { height: 400, type: 'donut' },
      legend: { position: 'bottom', fontWeight: 300 },
    };
    this.barChartOptions = {
      chart: { height: 400, type: 'bar' },
      plotOptions: {
        bar: { dataLabels: { position: 'top' } }
      },
      dataLabels: {
        enabled: true,
        formatter: (value) => `${value}`,
        offsetY: -20,
        style: { fontSize: '12px', colors: ['#304758'] }
      },
      xaxis: {
        categories: ['0-18', '19-30', '31-45', '46-60', '60+'],
        position: 'bottom'
      },
      legend: { position: 'bottom', fontWeight: 300 }
    };
    this.columnChartOptions = {
      chart: { height: 500, type: 'bar' },
      plotOptions: {
        bar: { dataLabels: { position: 'top' } }
      },
      dataLabels: {
        enabled: true,
        formatter: (value) => `${value} total`,
        offsetY: -20,
        style: { fontSize: '12px', colors: ['#304758'] }
      },
      xaxis: {
        categories: ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Jul', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'],
        position: 'top',
        labels: { offsetY: -18 },
        axisBorder: { show: false },
        axisTicks: { show: false },
        crosshairs: {
          fill: {
            type: 'gradient',
            gradient: { colorFrom: '#D8E3F0', colorTo: '#BED1E6', stops: [0, 100], opacityFrom: 0.4, opacityTo: 0.5 }
          }
        },
        tooltip: { enabled: true, offsetY: -35 }
      },
      fill: { colors: ['#22c55e', '#eab308', '#ef4444'] },
      yaxis: {
        axisBorder: { show: false },
        axisTicks: { show: false },
        labels: { show: false, formatter: (val) => `${val} total` }
      },
      title: {
        text: 'Répartition des patients par risque',
        floating: false,
        offsetY: 480,
        align: 'center',
        style: { color: '#444' }
      }
    };
  }

  ngOnInit() {
    this.store.getAllPatients();
    this.store.getUsers();
    this.store.getAllAssessments();
    this.store.getAllNotes();
  }
}