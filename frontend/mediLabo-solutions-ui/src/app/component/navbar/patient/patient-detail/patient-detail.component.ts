import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AppStore } from '../../../../store/app.store';
import { PermissionService } from '../../../../service/permission.service';

@Component({
  selector: 'app-patient-detail',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './patient-detail.component.html',
  styleUrl: './patient-detail.component.scss',
})
export class PatientDetailComponent {
  protected store = inject(AppStore);
  protected perm = inject(PermissionService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  patientUuid = '';
  isEditing = signal(false);
  editData = signal<any>({});

  // Formulaire note rapide
  showNoteForm = signal(false);
  noteContent = signal('');

  patient = computed(() => {
    const patient = this.store.patient();
    if (!patient) return null;
    const assessments = this.store.allAssessments() ?? [];
    const assessment = assessments.find(a => a.patientUuid === patient.patientUuid);
    return {
      ...patient,
      riskLevel: assessment?.riskLevel ?? null,
      riskLevelDescription: assessment?.riskLevelDescription ?? null,
      triggerCount: assessment?.triggerCount ?? 0,
      triggersFound: assessment?.triggersFound ?? [],
      assessedAt: assessment?.assessedAt ?? null
    };
  });

  patientNotes = computed(() => this.store.notes() ?? []);

  // Le patient a-t-il des notes ?
  hasNotes = computed(() => this.patientNotes().length > 0);

  // Le patient a-t-il été évalué ?
  hasAssessment = computed(() => !!this.patient()?.riskLevel);

  ngOnInit() {
    this.patientUuid = this.route.snapshot.params['patientUuid'];
    this.store.getPatient(this.patientUuid);
    this.store.getAllAssessments();
    this.store.getNotesByPatient(this.patientUuid);
  }

  // ── ÉDITION PATIENT ──

  startEditing() {
    const p = this.patient();
    if (!p) return;
    this.editData.set({
      userUuid: p.userUuid,
      firstName: p.userInfo?.firstName ?? '',
      lastName: p.userInfo?.lastName ?? '',
      email: p.userInfo?.email ?? '',
      address: p.userInfo?.address ?? '',
      phone: p.userInfo?.phone ?? '',
      dateOfBirth: p.dateOfBirth ?? '',
      gender: p.gender ?? '',
      // phone: '',
      // address: '',
      bloodType: p.bloodType ?? '',
      heightCm: p.heightCm ?? '',
      weightKg: p.weightKg ?? '',
      allergies: p.allergies ?? '',
      chronicConditions: p.chronicConditions ?? '',
      currentMedications: p.currentMedications ?? '',
      emergencyContactName: p.emergencyContactName ?? '',
      emergencyContactPhone: p.emergencyContactPhone ?? '',
      emergencyContactRelationship: p.emergencyContactRelationship ?? '',
      insuranceProvider: p.insuranceProvider ?? '',
      insuranceNumber: p.insuranceNumber ?? '',
      insurancePolicyNumber: p.insurancePolicyNumber ?? ''
    });
    this.isEditing.set(true);
  }

  cancelEditing() {
    this.isEditing.set(false);
  }

  savePatient() {
    const data = this.editData();
    this.store.updatePatient({ patientUuid: this.patientUuid, patient: data });
    this.isEditing.set(false);
  }

  updateField(field: string, event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.editData.update(d => ({ ...d, [field]: value }));
  }

  // ── NOTE RAPIDE ──

  toggleNoteForm() {
    this.showNoteForm.update(v => !v);
    this.noteContent.set('');
  }

  saveQuickNote() {
    const content = this.noteContent().trim();
    if (!content) return;
    this.store.createNote({ patientUuid: this.patientUuid, content });
    this.noteContent.set('');
    this.showNoteForm.set(false);
    // Recharger les notes après création
    setTimeout(() => this.store.getNotesByPatient(this.patientUuid), 500);
  }

  // ── ACTIONS ──

  assessPatient() {
    this.store.assessPatient(this.patientUuid);
  }

  deletePatient() {
    if (confirm('Êtes-vous sûr de vouloir désactiver ce patient ?')) {
      this.store.deletePatient(this.patientUuid);
      this.router.navigate(['/patients']);
    }
  }

  restorePatient() {
    this.store.restorePatient(this.patientUuid);
  }

  // ── HELPERS ──

  getRiskBadgeClass(risk: string | null): string {
    switch (risk) {
      case 'NONE': return 'bg-emerald-100 text-emerald-700 border-emerald-200';
      case 'BORDERLINE': return 'bg-amber-100 text-amber-700 border-amber-200';
      case 'IN_DANGER': return 'bg-orange-100 text-orange-700 border-orange-200';
      case 'EARLY_ONSET': return 'bg-red-100 text-red-700 border-red-200';
      default: return 'bg-gray-100 text-gray-600 border-gray-200';
    }
  }

  getRiskLabel(risk: string | null): string {
    switch (risk) {
      case 'NONE': return 'Aucun risque';
      case 'BORDERLINE': return 'Borderline';
      case 'IN_DANGER': return 'En danger';
      case 'EARLY_ONSET': return 'Apparition précoce';
      default: return 'Non évalué';
    }
  }

  getRiskIconBg(risk: string | null): string {
    switch (risk) {
      case 'NONE': return 'bg-emerald-500';
      case 'BORDERLINE': return 'bg-amber-500';
      case 'IN_DANGER': return 'bg-orange-500';
      case 'EARLY_ONSET': return 'bg-red-500';
      default: return 'bg-gray-400';
    }
  }

  truncate(text: string, length: number = 80): string {
    if (!text) return '';
    return text.length > length ? text.substring(0, length) + '...' : text;
  }
}