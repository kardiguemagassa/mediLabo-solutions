import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppStore } from '../../../../store/app.store';

@Component({
  selector: 'app-assessments',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './assessments.component.html',
  styleUrl: './assessments.component.scss',
})
export class AssessmentsComponent {
  protected store = inject(AppStore);

  searchQuery = signal('');
  riskFilter = signal('');
  genderFilter = signal('');
  pageSize = signal(10);

  ngOnInit() {
    this.store.getAllAssessments();
    this.store.getAllPatients();
  }

  // Enrichir assessments avec l'image patient
  enrichedAssessments = computed(() => {
    const assessments = this.store.allAssessments() ?? [];
    const patients = this.store.allPatients() ?? [];
    return assessments.map(a => {
      const patient = patients.find(p => p.patientUuid === a.patientUuid);
      return {
        ...a,
        patientImageUrl: patient?.userInfo?.imageUrl || 'https://cdn-icons-png.flaticon.com/512/149/149071.png'
      };
    });
  });

  // Filtrage
  filteredAssessments = computed(() => {
    let assessments = this.enrichedAssessments();
    const query = this.searchQuery().toLowerCase();
    const risk = this.riskFilter();
    const gender = this.genderFilter();

    if (query) {
      assessments = assessments.filter(a =>
        a.patientName?.toLowerCase().includes(query) ||
        a.triggersFound?.some(t => t.toLowerCase().includes(query))
      );
    }
    if (risk) {
      assessments = assessments.filter(a => a.riskLevel === risk);
    }
    if (gender) {
      assessments = assessments.filter(a => a.gender === gender);
    }
    return assessments;
  });

  // Pagination
  paginatedAssessments = computed(() => {
    const start = (this.store.currentPage() ?? 0) * this.pageSize();
    return this.filteredAssessments().slice(start, start + this.pageSize());
  });

  totalPages = computed(() => Math.ceil(this.filteredAssessments().length / this.pageSize()));

  changePageSize(event: Event) {
    const newSize = +(event.target as HTMLSelectElement).value;
    this.pageSize.set(newSize);
    this.store.setCurrentPage(0);
  }

  // Stats
  totalAssessments = computed(() => this.enrichedAssessments().length);
  borderlineCount = computed(() => this.enrichedAssessments().filter(a => a.riskLevel === 'BORDERLINE').length);
  inDangerCount = computed(() => this.enrichedAssessments().filter(a => a.riskLevel === 'IN_DANGER').length);
  earlyOnsetCount = computed(() => this.enrichedAssessments().filter(a => a.riskLevel === 'EARLY_ONSET').length);

  // Moyenne des déclencheurs
  avgTriggers = computed(() => {
    const all = this.enrichedAssessments();
    if (all.length === 0) return 0;
    return +(all.reduce((sum, a) => sum + a.triggerCount, 0) / all.length).toFixed(1);
  });

  onSearch(event: Event) {
    this.searchQuery.set((event.target as HTMLInputElement).value);
    this.store.setCurrentPage(0);
  }

  onRiskChange(event: Event) {
    this.riskFilter.set((event.target as HTMLSelectElement).value);
    this.store.setCurrentPage(0);
  }

  onGenderChange(event: Event) {
    this.genderFilter.set((event.target as HTMLSelectElement).value);
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

  reassess(patientUuid: string) {
    this.store.assessPatient(patientUuid);
  }

  getRiskBadgeClass(risk: string): string {
    switch (risk) {
      case 'NONE': return 'bg-emerald-100 text-emerald-700 border-emerald-200';
      case 'BORDERLINE': return 'bg-amber-100 text-amber-700 border-amber-200';
      case 'IN_DANGER': return 'bg-orange-100 text-orange-700 border-orange-200';
      case 'EARLY_ONSET': return 'bg-red-100 text-red-700 border-red-200';
      default: return 'bg-gray-100 text-gray-700 border-gray-200';
    }
  }

  getRiskIconBg(risk: string): string {
    switch (risk) {
      case 'NONE': return 'bg-emerald-500';
      case 'BORDERLINE': return 'bg-amber-500';
      case 'IN_DANGER': return 'bg-orange-500';
      case 'EARLY_ONSET': return 'bg-red-500';
      default: return 'bg-gray-500';
    }
  }

  clearFilters() {
    this.searchQuery.set('');
    this.riskFilter.set('');
    this.genderFilter.set('');
    this.store.setCurrentPage(0);
  }
}