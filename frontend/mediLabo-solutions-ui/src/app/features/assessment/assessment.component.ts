import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

interface Patient {
  id: number;
  firstName: string;
  lastName: string;
  birthDate: string;
  gender: 'M' | 'F';
}

interface AssessmentResult {
  patientName: string;
  age: number;
  gender: string;
  triggersCount: number;
  triggers: string[];
  risk: 'None' | 'Borderline' | 'In Danger' | 'Early Onset';
}

@Component({
  selector: 'app-assessment',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './assessment.component.html'
})
export class AssessmentComponent {
  selectedPatientId = '';
  isLoading = signal(false);
  result = signal<AssessmentResult | null>(null);

  patients: Patient[] = [
    { id: 1, firstName: 'Jean', lastName: 'Dupont', birthDate: '1985-03-15', gender: 'M' },
    { id: 2, firstName: 'Marie', lastName: 'Martin', birthDate: '1972-08-22', gender: 'F' },
    { id: 3, firstName: 'Pierre', lastName: 'Bernard', birthDate: '1990-01-10', gender: 'M' },
    { id: 4, firstName: 'Sophie', lastName: 'Leroy', birthDate: '1968-12-05', gender: 'F' },
    { id: 5, firstName: 'Lucas', lastName: 'Moreau', birthDate: '1995-06-18', gender: 'M' }
  ];

  // Triggers mockés par patient
  mockTriggers: Record<number, string[]> = {
    1: ['Taille', 'Poids'],
    2: ['Hémoglobine A1C', 'Microalbumine'],
    3: ['Fumeur', 'Cholestérol', 'Vertiges', 'Poids', 'Anormal'],
    4: ['Hémoglobine A1C', 'Anticorps', 'Rechute', 'Fumeuse', 'Taille', 'Poids'],
    5: []
  };

  riskLevels = [
    { name: 'None', icon: '✓', color: 'emerald', description: 'Aucun risque détecté' },
    { name: 'Borderline', icon: '!', color: 'amber', description: 'Risque limite, surveillance conseillée' },
    { name: 'In Danger', icon: '⚠', color: 'orange', description: 'Risque élevé, action recommandée' },
    { name: 'Early Onset', icon: '⛔', color: 'rose', description: 'Risque critique, intervention urgente' }
  ];

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

  runAssessment(): void {
    if (!this.selectedPatientId) return;

    this.isLoading.set(true);
    this.result.set(null);

    // Simulation d'appel API
    setTimeout(() => {
      const patient = this.patients.find(p => p.id === Number(this.selectedPatientId));
      if (!patient) return;

      const triggers = this.mockTriggers[patient.id] || [];
      const age = this.calculateAge(patient.birthDate);
      const triggersCount = triggers.length;

      // Calcul du risque
      let risk: 'None' | 'Borderline' | 'In Danger' | 'Early Onset' = 'None';

      if (age > 30) {
        if (triggersCount >= 6) risk = 'Early Onset';
        else if (triggersCount >= 4) risk = 'In Danger';
        else if (triggersCount >= 2) risk = 'Borderline';
      } else {
        if (patient.gender === 'M') {
          if (triggersCount >= 5) risk = 'Early Onset';
          else if (triggersCount >= 3) risk = 'In Danger';
        } else {
          if (triggersCount >= 7) risk = 'Early Onset';
          else if (triggersCount >= 4) risk = 'In Danger';
        }
      }

      this.result.set({
        patientName: `${patient.firstName} ${patient.lastName}`,
        age,
        gender: patient.gender === 'M' ? 'Masculin' : 'Féminin',
        triggersCount,
        triggers,
        risk
      });

      this.isLoading.set(false);
    }, 1000);
  }

  getRiskClass(risk: string): string {
    const classes: Record<string, string> = {
      'None': 'bg-emerald-100 text-emerald-800 border-emerald-200',
      'Borderline': 'bg-amber-100 text-amber-800 border-amber-200',
      'In Danger': 'bg-orange-100 text-orange-800 border-orange-200',
      'Early Onset': 'bg-rose-100 text-rose-800 border-rose-200'
    };
    return classes[risk] || classes['None'];
  }

  getRiskBg(risk: string): string {
    const classes: Record<string, string> = {
      'None': 'from-emerald-500 to-teal-600',
      'Borderline': 'from-amber-500 to-orange-500',
      'In Danger': 'from-orange-500 to-red-500',
      'Early Onset': 'from-rose-500 to-red-600'
    };
    return classes[risk] || classes['None'];
  }
}
