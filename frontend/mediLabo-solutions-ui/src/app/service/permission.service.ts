import { computed, inject, Injectable } from '@angular/core';
import { AppStore } from '../store/app.store';

/**
 * Service centralisé de gestion des permissions (RBAC).
 *
 * Architecture :
 * - Les permissions viennent du profil utilisateur (authorities du JWT)
 * - Chaque méthode retourne un computed() réactif utilisable dans les templates
 * - La navbar, le dashboard et les guards consomment ce service
 *
 * Hiérarchie des rôles MediLabo :
 *   USER < PRACTITIONER < HEAD_PRACTITIONER < ADMIN < SUPER_ADMIN
 */
@Injectable({ providedIn: 'root' })
export class PermissionService {
  private readonly store = inject(AppStore);
  private readonly profile = computed(() => this.store.profile());

  /** Rôle courant de l'utilisateur connecté */
  readonly role = computed(() => this.profile()?.role ?? '');

  /** Liste des authorities (permissions unitaires) */
  private readonly authorities = computed(() => {
    const p = this.profile();
    const raw = (p as any)?.authorities ?? p?.permissions ?? '';
    return raw ? raw.split(',').map((a: string) => a.trim()) : [];
  });

  // ─── Vérifications génériques ───

  hasRole(...roles: string[]): boolean {
    return roles.includes(this.role());
  }

  hasPermission(...permissions: string[]): boolean {
    const userPerms = this.authorities();
    return permissions.some(p => userPerms.includes(p));
  }

  // ─── Rôles ───

  readonly isAdmin    = computed(() => this.hasRole('ADMIN', 'SUPER_ADMIN'));
  readonly isStaff    = computed(() => this.hasRole('PRACTITIONER', 'HEAD_PRACTITIONER', 'ADMIN', 'SUPER_ADMIN'));
  readonly isClinical = computed(() => this.hasRole('PRACTITIONER', 'HEAD_PRACTITIONER'));

  // ─── Patients ───

  readonly canViewPatients  = computed(() => this.hasPermission('patient:read'));
  readonly canCreatePatient = computed(() => this.hasPermission('patient:create'));
  readonly canDeletePatient = computed(() => this.hasPermission('patient:delete'));

  // ─── Notes ───

  readonly canViewNotes  = computed(() => this.hasPermission('note:read'));
  readonly canCreateNote = computed(() => this.hasPermission('note:create'));
  readonly canDeleteNote = computed(() => this.hasPermission('note:delete'));

  // ─── Assessments ───

  readonly canViewAssessments  = computed(() => this.hasPermission('assessment:read'));
  readonly canCreateAssessment = computed(() => this.hasPermission('assessment:create'));
  readonly canDeleteAssessment = computed(() => this.hasPermission('assessment:delete'));

  // ─── Users ───

  readonly canViewUsers   = computed(() => this.hasPermission('user:create') || this.isAdmin());
  readonly canManageUsers = computed(() => this.hasPermission('user:delete') || this.hasRole('SUPER_ADMIN'));

  // ─── Système ───

  readonly canViewSystem = computed(() => this.hasPermission('system:read'));

  // ─── Transversal ───

  readonly canViewMessages = computed(() => true);
}