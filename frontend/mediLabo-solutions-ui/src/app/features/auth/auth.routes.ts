import { Routes } from '@angular/router';
import { guestGuard } from '../../core/guards/guest.guard';

export const AUTH_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./auth.component').then(c => c.AuthComponent),
    children: [
      {
        path: '',
        loadComponent: () => import('../../features/landing/landing.component').then(c => c.LandingComponent)
      },
      {
        path: 'login',
        canActivate: [guestGuard],
        loadComponent: () => import('./login/login.component').then(c => c.LoginComponent)
      },
      {
        path: 'register',
        canActivate: [guestGuard],
        loadComponent: () => import('./register/register.component').then(c => c.RegisterComponent)
      }
    ]
  }
];
