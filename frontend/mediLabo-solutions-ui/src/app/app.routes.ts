import { Routes } from '@angular/router';


export const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./component/authorizationserver/authorizationserver.routes').then(r => r.AUTHORIZATIONSERVER_ROUTES)
  },
  {
    path: '',
    loadChildren: () => import('./component/navbar/navbar.routes').then(r => r.NAVBAR_ROUTES)
  }
];