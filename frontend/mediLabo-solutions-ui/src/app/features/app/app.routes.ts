import { Routes } from '@angular/router';

export const APP_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('../../layouts/private-layout/private-layout.component').then(c => c.PrivateLayoutComponent),
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () => import('../dashboard/dashboard.component').then(c => c.DashboardComponent)
      },
      {
        path: 'patients',
        loadComponent: () => import('../patients/patients.component').then(c => c.PatientsComponent)
      },
      {
        path: 'patients/:id',
        loadComponent: () => import('../patients/patient-detail/patient-detail.component').then(c => c.PatientDetailComponent)
      },
      {
        path: 'notes',
        loadComponent: () => import('../notes/notes.component').then(c => c.NotesComponent)
      },
      {
        path: 'assessment',
        loadComponent: () => import('../assessment/assessment.component').then(c => c.AssessmentComponent)
      },
      {
        path: 'settings',
        loadComponent: () => import('../settings/settings.component').then(c => c.SettingsComponent)
      }
    ]
  }
];
