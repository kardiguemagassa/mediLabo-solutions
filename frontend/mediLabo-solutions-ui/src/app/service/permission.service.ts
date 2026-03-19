// import { computed, inject, Injectable } from '@angular/core';
// import { AppStore } from '../store/app.store';

// @Injectable({ providedIn: 'root' })
// export class PermissionService {
//   private store = inject(AppStore);

//   private profile = computed(() => this.store.profile());

//   // Rôle courant
//   role = computed(() => this.profile()?.role ?? '');

//   // Authorities (le backend renvoie "authorities", IUser a "permissions")
//   private authorities = computed(() => {
//     const p = this.profile();
//     const raw = (p as any)?.authorities ?? p?.permissions ?? '';
//     return raw ? raw.split(',').map((a: string) => a.trim()) : [];
//   });

//   // Vérifier un rôle
//   hasRole = (...roles: string[]) => roles.includes(this.role());

//   // Vérifier une permission
//   hasPermission = (...permissions: string[]) => {
//     const userPerms = this.authorities();
//     return permissions.some(p => userPerms.includes(p));
//   };

//   // Raccourcis computed pour les templates
//   canViewDashboard = computed(() => this.hasPermission('patient:read'));
//   canViewPatients = computed(() => this.hasPermission('patient:read'));
//   canCreatePatient = computed(() => this.hasPermission('patient:create'));
//   canDeletePatient = computed(() => this.hasPermission('patient:delete'));
//   canViewNotes = computed(() => this.hasPermission('note:read'));
//   canCreateNote = computed(() => this.hasPermission('note:create'));
//   canDeleteNote = computed(() => this.hasPermission('note:delete'));
//   canViewAssessments = computed(() => this.hasPermission('assessment:read'));
//   canCreateAssessment = computed(() => this.hasPermission('assessment:create'));
//   canDeleteAssessment = computed(() => this.hasPermission('assessment:delete'));
//   canManageUsers = computed(() => this.hasPermission('user:create') || this.hasRole('ADMIN', 'SUPER_ADMIN'));
//   canViewMessages = computed(() => true); // Tous les utilisateurs connectés
//   isAdmin = computed(() => this.hasRole('ADMIN', 'SUPER_ADMIN'));
//   isStaff = computed(() => this.hasRole('PRACTITIONER', 'HEAD_PRACTITIONER', 'ADMIN', 'SUPER_ADMIN'));
// }