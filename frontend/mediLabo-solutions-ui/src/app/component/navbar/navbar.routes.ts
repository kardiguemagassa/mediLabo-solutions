import { Routes } from "@angular/router";

export const NAVBAR_ROUTES: Routes = [
     { path: '', redirectTo: '', pathMatch: 'full'
    },
    {
        path: '',
        loadComponent: () => import('./navbar.component').then(c => c.NavbarComponent),
        children: [
            {
                path: 'dashboard',
                loadComponent: () => import('./dashboard/dashboard.component').then(c => c.DashboardComponent)
            },
            {
                path: 'assessments',
                loadComponent: () => import('./assessment/assessments/assessments.component').then(c => c.AssessmentsComponent)
            },
             {
                path: 'assessments/:assessmentUuid',
                loadComponent: () => import('./assessment/assessment-detail/assessment-detail.component').then(c => c.AssessmentDetailComponent)
            },
            {
                path: 'patients',
                loadComponent: () => import('./patient/patients/patients.component').then(c => c.PatientsComponent)
            },
            {
                path: 'patients/:patientUuid',
                loadComponent: () => import('./patient/patient-detail/patient-detail.component').then(c => c.PatientDetailComponent)
            },
            {
                path: 'notes',
                loadComponent: () => import('./note/notes/notes.component').then(c => c.NotesComponent)
            },
            {
                path: 'notes/:noteUuid',
                loadComponent: () => import('./note/note-detail/note-detail.component').then(c => c.NoteDetailComponent)
            },
            {
                path: 'profile',
                loadComponent: () => import('./profile/profile.component').then(c => c.ProfileComponent)
            },
            {
                path: 'users',
                loadComponent: () => import('./user/users/users.component').then(c => c.UsersComponent)
            },
            {
                path: 'users/:userUuid',
                loadComponent: () => import('./user/user-detail/user-detail.component').then(c => c.UserDetailComponent)
            },
            {
                path: 'messages',
                loadComponent: () => import('./message/messages/messages.component').then(c => c.MessagesComponent)
            },
            {
                path: 'messages/:conversationId',
                loadComponent: () => import('./message/message-detail/message-detail.component').then(c => c.MessageDetailComponent)
            }
        ]
    }
]
