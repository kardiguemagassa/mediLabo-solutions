import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html'
})
export class SettingsComponent {
  // Onglet actif
  activeTab = signal<'profile' | 'security' | 'preferences' | 'about'>('profile');

  // Formulaire profil
  profile = {
    firstName: '',
    lastName: '',
    email: '',
    specialty: ''
  };

  // Formulaire mot de passe
  passwordForm = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  };
  showCurrentPassword = signal(false);
  showNewPassword = signal(false);
  showConfirmPassword = signal(false);

  // Préférences
  preferences = {
    theme: 'light',
    language: 'fr',
    emailNotifications: true,
    riskAlerts: true
  };

  // États
  isSaving = signal(false);
  successMessage = signal('');

  specialties = [
    { value: 'generaliste', label: 'Médecin généraliste' },
    { value: 'endocrinologue', label: 'Endocrinologue' },
    { value: 'diabetologue', label: 'Diabétologue' },
    { value: 'cardiologue', label: 'Cardiologue' },
    { value: 'autre', label: 'Autre' }
  ];

  constructor(public authService: AuthService) {
    // Charger les données utilisateur
    const user = this.authService.currentUser();
    if (user) {
      this.profile.firstName = user.firstName;
      this.profile.lastName = user.lastName;
      this.profile.email = user.email;
      this.profile.specialty = 'generaliste';
    }
  }

  setActiveTab(tab: 'profile' | 'security' | 'preferences' | 'about'): void {
    this.activeTab.set(tab);
    this.successMessage.set('');
  }

  saveProfile(): void {
    this.isSaving.set(true);
    // Simulation sauvegarde
    setTimeout(() => {
      this.isSaving.set(false);
      this.successMessage.set('Profil mis à jour avec succès !');
      setTimeout(() => this.successMessage.set(''), 3000);
    }, 1000);
  }

  changePassword(): void {
    if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
      alert('Les mots de passe ne correspondent pas');
      return;
    }
    this.isSaving.set(true);
    // Simulation changement mot de passe
    setTimeout(() => {
      this.isSaving.set(false);
      this.successMessage.set('Mot de passe modifié avec succès !');
      this.passwordForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
      setTimeout(() => this.successMessage.set(''), 3000);
    }, 1000);
  }

  savePreferences(): void {
    this.isSaving.set(true);
    // Simulation sauvegarde
    setTimeout(() => {
      this.isSaving.set(false);
      this.successMessage.set('Préférences enregistrées !');
      setTimeout(() => this.successMessage.set(''), 3000);
    }, 1000);
  }
}
