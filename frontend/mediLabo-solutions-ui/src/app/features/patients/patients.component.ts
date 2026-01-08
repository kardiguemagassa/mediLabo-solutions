import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

/**
 * Interface Patient
 * Représente les données d'un patient
 */
export interface Patient {
  id: number;
  firstName: string;
  lastName: string;
  birthDate: string;
  gender: 'M' | 'F';
  address?: string;
  phone?: string;
  risk?: 'None' | 'Borderline' | 'In Danger' | 'Early Onset';
}

@Component({
  selector: 'app-patients',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './patients.component.html'
})
export class PatientsComponent {
  // État du modal
  showAddModal = signal(false);
  showEditModal = signal(false);
  showDeleteModal = signal(false);
  
  // Patient sélectionné pour édition/suppression
  selectedPatient = signal<Patient | null>(null);
  
  // Filtres
  searchQuery = '';
  riskFilter = '';
  genderFilter = '';

  // Données mockées (sera remplacé par appel API)
  patients = signal<Patient[]>([
    { id: 1, firstName: 'Jean', lastName: 'Dupont', birthDate: '1985-03-15', gender: 'M', phone: '06 12 34 56 78', address: '15 Rue de la Paix, Paris', risk: 'None' },
    { id: 2, firstName: 'Marie', lastName: 'Martin', birthDate: '1972-08-22', gender: 'F', phone: '06 98 76 54 32', address: '8 Avenue des Champs, Lyon', risk: 'Borderline' },
    { id: 3, firstName: 'Pierre', lastName: 'Bernard', birthDate: '1990-01-10', gender: 'M', phone: '06 11 22 33 44', address: '22 Boulevard Victor Hugo, Marseille', risk: 'In Danger' },
    { id: 4, firstName: 'Sophie', lastName: 'Leroy', birthDate: '1968-12-05', gender: 'F', phone: '06 55 66 77 88', address: '5 Rue du Commerce, Bordeaux', risk: 'Early Onset' },
    { id: 5, firstName: 'Lucas', lastName: 'Moreau', birthDate: '1995-06-18', gender: 'M', phone: '06 99 88 77 66', address: '12 Place de la République, Toulouse', risk: 'None' }
  ]);

  // Nouveau patient (formulaire)
  newPatient: Partial<Patient> = {
    firstName: '',
    lastName: '',
    birthDate: '',
    gender: 'M',
    phone: '',
    address: ''
  };

  /**
   * Retourne les patients filtrés
   */
  get filteredPatients(): Patient[] {
    return this.patients().filter(patient => {
      const matchesSearch = this.searchQuery === '' || 
        `${patient.firstName} ${patient.lastName}`.toLowerCase().includes(this.searchQuery.toLowerCase());
      const matchesRisk = this.riskFilter === '' || patient.risk === this.riskFilter;
      const matchesGender = this.genderFilter === '' || patient.gender === this.genderFilter;
      return matchesSearch && matchesRisk && matchesGender;
    });
  }

  /**
   * Calcule l'âge à partir de la date de naissance
   */
  calculateAge(birthDate: string): number {
    const today = new Date();
    const birth = new Date(birthDate);
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  }

  /**
   * Formate la date en français
   */
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR');
  }

  /**
   * Classes CSS pour le badge de risque
   */
  getRiskClass(risk: string | undefined): string {
    const classes: Record<string, string> = {
      'None': 'bg-emerald-100 text-emerald-800 border-emerald-200',
      'Borderline': 'bg-amber-100 text-amber-800 border-amber-200',
      'In Danger': 'bg-orange-100 text-orange-800 border-orange-200',
      'Early Onset': 'bg-rose-100 text-rose-800 border-rose-200'
    };
    return classes[risk || 'None'] || classes['None'];
  }

  /**
   * Classes CSS pour le badge de genre
   */
  getGenderClass(gender: string): string {
    return gender === 'M' ? 'bg-blue-100 text-blue-700' : 'bg-pink-100 text-pink-700';
  }

  /**
   * Classes CSS pour l'avatar
   */
  getAvatarClass(gender: string): string {
    return gender === 'M' ? 'from-cyan-500 to-blue-600' : 'from-pink-500 to-rose-600';
  }

  // --- Actions Modal ---

  openAddModal(): void {
    this.newPatient = { firstName: '', lastName: '', birthDate: '', gender: 'M', phone: '', address: '' };
    this.showAddModal.set(true);
  }

  closeAddModal(): void {
    this.showAddModal.set(false);
  }

  openEditModal(patient: Patient): void {
    this.selectedPatient.set({ ...patient });
    this.showEditModal.set(true);
  }

  closeEditModal(): void {
    this.showEditModal.set(false);
    this.selectedPatient.set(null);
  }

  openDeleteModal(patient: Patient): void {
    this.selectedPatient.set(patient);
    this.showDeleteModal.set(true);
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
    this.selectedPatient.set(null);
  }

  // --- CRUD Operations (mock) ---

  addPatient(): void {
    const newId = Math.max(...this.patients().map(p => p.id)) + 1;
    const patient: Patient = {
      id: newId,
      firstName: this.newPatient.firstName || '',
      lastName: this.newPatient.lastName || '',
      birthDate: this.newPatient.birthDate || '',
      gender: this.newPatient.gender as 'M' | 'F',
      phone: this.newPatient.phone,
      address: this.newPatient.address,
      risk: 'None'
    };
    this.patients.update(patients => [...patients, patient]);
    this.closeAddModal();
  }

  updatePatient(): void {
    const updated = this.selectedPatient();
    if (updated) {
      this.patients.update(patients => 
        patients.map(p => p.id === updated.id ? updated : p)
      );
    }
    this.closeEditModal();
  }

  deletePatient(): void {
    const toDelete = this.selectedPatient();
    if (toDelete) {
      this.patients.update(patients => patients.filter(p => p.id !== toDelete.id));
    }
    this.closeDeleteModal();
  }
}
