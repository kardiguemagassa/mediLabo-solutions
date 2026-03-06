import { Routes } from "@angular/router";

export const AUTHORIZATIONSERVER_ROUTES: Routes = [
    {path: '', redirectTo: '', pathMatch: 'full'},
    {path: '',
        loadComponent: () => import('./authorizationserver.component').then(c => c.AuthorizationserverComponent),
        children: [
            {
                path:'',
                loadComponent: () => import('./home/home.component').then(c => c.HomeComponent)
            },
            {
                path:'register',
                loadComponent: () => import('./register/register.component').then(c => c.RegisterComponent)
            },
            {
                path:'resetpassord',
                loadComponent: () => import('./resetpassword/resetpassword.component').then(c => c.ResetpassordComponent)
            },
            {
                path:'verify/account',
                loadComponent: () => import('./verifyaccount/verifyaccount.component').then(c => c.VerifyaccountComponent)
            },
            {
                path:'verify/password',
                loadComponent: () => import('./verifypassword/verifypassword.component').then(c => c.VerifypasswordComponent)
            },
        ]
    }
]
