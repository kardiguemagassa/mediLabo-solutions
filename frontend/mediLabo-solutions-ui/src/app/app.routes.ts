import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // Public routes (Landing, Login, Register)
  {
    path: '',
    loadChildren: () => import('./features/auth/auth.routes').then(r => r.AUTH_ROUTES)
  },
  
  // Protected routes (Dashboard, Patients, etc.)
  {
    path: 'app',
    canActivate: [authGuard],
    loadChildren: () => import('./features/app/app.routes').then(r => r.APP_ROUTES)
  },
  
  // Fallback
  {
    path: '**',
    redirectTo: ''
  }
];
