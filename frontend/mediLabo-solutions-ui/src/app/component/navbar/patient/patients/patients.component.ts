import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppStore } from '../../../../store/app.store';
import { PermissionService } from '../../../../service/permission.service';

@Component({
  selector: 'app-patients',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './patients.component.html',
  styleUrl: './patients.component.scss',
})
export class PatientsComponent {
  protected store = inject(AppStore);
  protected permission = inject(PermissionService);

  searchQuery = signal('');
  genderFilter = signal('');
  riskFilter = signal('');
  statusFilter = signal('');
  pageSize = 10;

  // Formulaire création patient
  showCreateForm = signal(false);
  selectedUserUuid = signal('');
  patientForm = signal<any>({
    dateOfBirth: '',
    gender: '',
    phone: '',
    address: ''
  });

  // User sélectionné (auto-rempli)
  selectedUser = computed(() => {
    const uuid = this.selectedUserUuid();
    if (!uuid) return null;
    return this.availableUsers().find(u => u.userUuid === uuid) ?? null;
  });

  // Users qui n'ont PAS encore de dossier patient
  availableUsers = computed(() => {
    const users = this.store.users() ?? [];
    const patients = this.store.allPatients() ?? [];
    const patientUserUuids = patients.map(p => p.userUuid);
    return users.filter(u => !patientUserUuids.includes(u.userUuid));
  });

  ngOnInit() {
    this.store.getAllPatients();
    this.store.getAllAssessments();
  }

  toggleCreateForm() {
    this.showCreateForm.update(v => !v);
    this.selectedUserUuid.set('');
    this.patientForm.set({ dateOfBirth: '', gender: '', phone: '', address: '' });
  }

  onUserSelected(event: Event) {
    this.selectedUserUuid.set((event.target as HTMLSelectElement).value);
  }

  updatePatientField(field: string, event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.patientForm.update(f => ({ ...f, [field]: value }));
  }

  createPatient() {
    const user = this.selectedUser();
    const form = this.patientForm();
    if (!user || !form.dateOfBirth || !form.gender) return;

    this.store.createPatient({
      userUuid: user.userUuid,
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      dateOfBirth: form.dateOfBirth,
      gender: form.gender,
      phone: form.phone,
      address: form.address
    });

    this.toggleCreateForm();
  }

  enrichedPatients = computed(() => {
    const patients = this.store.allPatients() ?? [];
    const assessments = this.store.allAssessments() ?? [];

    return patients.map((patient) => {
      const assessment = assessments.find(
        (a) => a.patientUuid === patient.patientUuid,
      );
      return {
        ...patient,
        active: patient.active ?? true,
        riskLevel: assessment?.riskLevel ?? 'NONE',
        riskLevelDescription: assessment?.riskLevelDescription ?? 'Non évalué',
        triggerCount: assessment?.triggerCount ?? 0,
        triggersFound: assessment?.triggersFound ?? [],
      };
    });
  });

  filteredPatients = computed(() => {
    let patients = this.enrichedPatients();
    const query = this.searchQuery().toLowerCase();
    const gender = this.genderFilter();
    const risk = this.riskFilter();
    const status = this.statusFilter();

    if (query) {
      patients = patients.filter(
        (p) =>
          p.userInfo?.firstName?.toLowerCase().includes(query) ||
          p.userInfo?.lastName?.toLowerCase().includes(query) ||
          p.userInfo?.email?.toLowerCase().includes(query) ||
          p.medicalRecordNumber?.toLowerCase().includes(query),
      );
    }
    if (gender) {
      patients = patients.filter((p) => p.gender === gender);
    }
    if (risk) {
      patients = patients.filter((p) => p.riskLevel === risk);
    }
    if (status === 'active')
      patients = patients.filter((p) => p.active !== false);
    else if (status === 'inactive')
      patients = patients.filter((p) => p.active === false);
    return patients;
  });

  onStatusChange(event: Event) {
    this.statusFilter.set((event.target as HTMLSelectElement).value);
    this.store.setCurrentPage(0);
  }

  paginatedPatients = computed(() => {
    const start = (this.store.currentPage() ?? 0) * this.pageSize;
    return this.filteredPatients().slice(start, start + this.pageSize);
  });

  totalPages = computed(() =>
    Math.ceil(this.filteredPatients().length / this.pageSize),
  );

  // Stats
  totalPatients = computed(() => this.enrichedPatients().length);
  malePatients = computed(
    () => this.enrichedPatients().filter((p) => p.gender === 'MALE').length,
  );
  femalePatients = computed(
    () => this.enrichedPatients().filter((p) => p.gender === 'FEMALE').length,
  );
  atRiskPatients = computed(
    () =>
      this.enrichedPatients().filter(
        (p) => p.riskLevel === 'IN_DANGER' || p.riskLevel === 'EARLY_ONSET',
      ).length,
  );

  onSearch(event: Event) {
    this.searchQuery.set((event.target as HTMLInputElement).value);
    this.store.setCurrentPage(0);
  }

  onGenderChange(event: Event) {
    this.genderFilter.set((event.target as HTMLSelectElement).value);
    this.store.setCurrentPage(0);
  }

  onRiskChange(event: Event) {
    this.riskFilter.set((event.target as HTMLSelectElement).value);
    this.store.setCurrentPage(0);
  }

  goToPage(page: number) {
    this.store.setCurrentPage(page);
  }

  previousPage() {
    const current = this.store.currentPage() ?? 0;
    if (current > 0) this.store.setCurrentPage(current - 1);
  }

  nextPage() {
    const current = this.store.currentPage() ?? 0;
    if (current < this.totalPages() - 1) this.store.setCurrentPage(current + 1);
  }

  getRiskBadgeClass(risk: string): string {
    switch (risk) {
      case 'NONE':
        return 'bg-emerald-100 text-emerald-700';
      case 'BORDERLINE':
        return 'bg-amber-100 text-amber-700';
      case 'IN_DANGER':
        return 'bg-orange-100 text-orange-700';
      case 'EARLY_ONSET':
        return 'bg-red-100 text-red-700';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  }

  getRiskLabel(risk: string): string {
    switch (risk) {
      case 'NONE':
        return 'Aucun';
      case 'BORDERLINE':
        return 'Borderline';
      case 'IN_DANGER':
        return 'En danger';
      case 'EARLY_ONSET':
        return 'Précoce';
      default:
        return 'Non évalué';
    }
  }

  getGenderLabel(gender: string): string {
    return gender === 'MALE' ? 'Homme' : gender === 'FEMALE' ? 'Femme' : gender;
  }

  assessPatient(patientUuid: string) {
    this.store.assessPatient(patientUuid);
  }

  deletePatient(patientUuid: string) {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce patient ?')) {
      this.store.deletePatient(patientUuid);
    }
  }

  restorePatient(patientUuid: string) {
    this.store.restorePatient(patientUuid);
  }

  isInactive(patient: any): boolean {
    return !patient.active;
  }

  clearFilters() {
    this.searchQuery.set('');
    this.genderFilter.set('');
    this.riskFilter.set('');
    this.statusFilter.set('');
    this.store.setCurrentPage(0);
  }
}
