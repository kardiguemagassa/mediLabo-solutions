import { Routes } from '@angular/router';


export const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./component/authorizationserver/authorizationserver.routes').then(r => r.AUTHORIZATIONSERVER_ROUTES)
  }
  // Public routes (Landing, Login, Register)
  /*{
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
  }*/
];
