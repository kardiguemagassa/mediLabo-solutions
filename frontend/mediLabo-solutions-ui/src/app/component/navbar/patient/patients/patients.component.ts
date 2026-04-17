import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppStore } from '../../../../store/app.store';
import { defaultQuery } from '../../../../interface/query';

@Component({
  selector: 'app-patients',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './patients.component.html',
  styleUrl: './patients.component.scss',
})
export class PatientsComponent {
  protected store = inject(AppStore);

  // Filtres locaux (côté client sur la page courante)
  searchQuery = signal('');
  genderFilter = signal('');
  riskFilter = signal('');
  statusFilter = signal('');

  // Formulaire création patient
  showCreateForm = signal(false);
  selectedUserUuid = signal('');
  patientForm = signal<any>({
    dateOfBirth: '',
    gender: '',
    phone: '',
    address: ''
  });

  selectedUser = computed(() => {
    const uuid = this.selectedUserUuid();
    if (!uuid) return null;
    return this.availableUsers().find(u => u.userUuid === uuid) ?? null;
  });

  availableUsers = computed(() => {
    const users = this.store.users() ?? [];
    const patients = this.store.allPatients() ?? [];
    const patientUserUuids = patients.map(p => p.userUuid);
    return users.filter(u => !patientUserUuids.includes(u.userUuid));
  });

  // Données  paginées depuis le backend
  patientPage = computed(() => this.store.patientPage());
  currentPage = computed(() => this.patientPage()?.currentPage ?? 0);
  totalPages = computed(() => this.patientPage()?.totalPages ?? 0);
  totalElements = computed(() => this.patientPage()?.totalElements ?? 0);
  pageSize = computed(() => this.patientPage()?.size ?? 10);

  // Enrichir les patients de la page courante avec les assessments
  enrichedPatients = computed(() => {
    const patients = this.patientPage()?.content ?? [];
    const assessments = this.store.allAssessments() ?? [];

    return patients.map(patient => {
      const assessment = assessments.find(a => a.patientUuid === patient.patientUuid);
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

  // Filtrage côté client sur la page courante
  filteredPatients = computed(() => {
    let patients = this.enrichedPatients();
    const query = this.searchQuery().toLowerCase();
    const gender = this.genderFilter();
    const risk = this.riskFilter();
    const status = this.statusFilter();

    if (query) {
      // filtre en mémoire
      patients = patients.filter(p =>
        p.userInfo?.firstName?.toLowerCase().includes(query) ||
        p.userInfo?.lastName?.toLowerCase().includes(query) ||
        p.userInfo?.email?.toLowerCase().includes(query) ||
        p.medicalRecordNumber?.toLowerCase().includes(query)
      );
    }
    if (gender) patients = patients.filter(p => p.gender === gender);
    if (risk) patients = patients.filter(p => p.riskLevel === risk);
    if (status === 'active') patients = patients.filter(p => p.active !== false);
    else if (status === 'inactive') patients = patients.filter(p => p.active === false);

    return patients;
  });

  // Stats basées sur allPatients (chargement complet pour avoir les vrais totaux)
  allEnrichedPatients = computed(() => {
    const patients = this.store.allPatients() ?? [];
    const assessments = this.store.allAssessments() ?? [];
    return patients.map(patient => {
      const assessment = assessments.find(a => a.patientUuid === patient.patientUuid);
      return { ...patient, riskLevel: assessment?.riskLevel ?? 'NONE' };
    });
  });

  totalPatients = computed(() => this.allEnrichedPatients().length);
  malePatients = computed(() => this.allEnrichedPatients().filter(p => p.gender === 'MALE').length);
  femalePatients = computed(() => this.allEnrichedPatients().filter(p => p.gender === 'FEMALE').length);
  atRiskPatients = computed(() => this.allEnrichedPatients().filter(p => p.riskLevel === 'IN_DANGER' || p.riskLevel === 'EARLY_ONSET').length);

  hasActiveFilters = computed(() => !!(this.searchQuery() || this.genderFilter() || this.riskFilter() || this.statusFilter()));

  // lifecycle
  ngOnInit() {
    this.store.getAllPatientsPageable(defaultQuery);
    this.store.getAllPatients();
    this.store.getAllAssessments();
  }

 //paginées depuis le backend
  loadPage(page: number) {
    const current = this.store.query() ?? defaultQuery;
    this.store.getAllPatientsPageable({ ...current, page });
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages()) {
      this.loadPage(page);
    }
  }

  previousPage() { this.goToPage(this.currentPage() - 1); }
  nextPage() { this.goToPage(this.currentPage() + 1); }

  changeSort(sortBy: string) {
    const current = this.store.query() ?? defaultQuery;
    const direction = current.sortBy === sortBy && current.direction === 'desc' ? 'asc' : 'desc';
    this.store.getAllPatientsPageable({ ...current, sortBy, direction, page: 0 });
  }

  changePageSize(event: Event) {
    const current = this.store.query() ?? defaultQuery;
    const size = +(event.target as HTMLSelectElement).value;
    this.store.getAllPatientsPageable({ ...current, size, page: 0 });
  }

  // filtres

  onSearch(event: Event) {
    this.searchQuery.set((event.target as HTMLInputElement).value);
  }

  onGenderChange(event: Event) {
    this.genderFilter.set((event.target as HTMLSelectElement).value);
  }

  onRiskChange(event: Event) {
    this.riskFilter.set((event.target as HTMLSelectElement).value);
  }

  onStatusChange(event: Event) {
    this.statusFilter.set((event.target as HTMLSelectElement).value);
  }

  clearFilters() {
    this.searchQuery.set('');
    this.genderFilter.set('');
    this.riskFilter.set('');
    this.statusFilter.set('');
  }

  // Formulaire de creation
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
    // Recharger la page courante après création
    setTimeout(() => {
      this.loadPage(0);
      this.store.getAllPatients();
    }, 500);
  }

  // Actions
  assessPatient(patientUuid: string) {
    this.store.assessPatient(patientUuid);
  }

  deletePatient(patientUuid: string) {
    if (confirm('Êtes-vous sûr de vouloir désactiver ce patient ?')) {
      this.store.deletePatient(patientUuid);
      // Attendre que le store ait fini, puis recharger
      setTimeout(() => {
        this.loadPage(this.currentPage());
        this.store.getAllPatients();
      }, 1000); 
    }
  }

  restorePatient(patientUuid: string) {
    this.store.restorePatient(patientUuid);
    setTimeout(() => {
      this.loadPage(this.currentPage());
      this.store.getAllPatients();
    }, 1000);
  }

  // Helpers

  isInactive(patient: any): boolean { return !patient.active; }

  getRiskBadgeClass(risk: string): string {
    switch (risk) {
      case 'NONE': return 'bg-emerald-100 text-emerald-700';
      case 'BORDERLINE': return 'bg-amber-100 text-amber-700';
      case 'IN_DANGER': return 'bg-orange-100 text-orange-700';
      case 'EARLY_ONSET': return 'bg-red-100 text-red-700';
      default: return 'bg-gray-100 text-gray-700';
    }
  }

  getRiskLabel(risk: string): string {
    switch (risk) {
      case 'NONE': return 'Aucun';
      case 'BORDERLINE': return 'Borderline';
      case 'IN_DANGER': return 'En danger';
      case 'EARLY_ONSET': return 'Précoce';
      default: return 'Non évalué';
    }
  }
}