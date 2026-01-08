import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';

/**
 * Interfaces
 */
interface Patient {
  id: number;
  firstName: string;
  lastName: string;
  birthDate: string;
  gender: 'M' | 'F';
  address?: string;
  phone?: string;
  risk?: 'None' | 'Borderline' | 'In Danger' | 'Early Onset';
}

interface Note {
  id: string;
  patientId: number;
  date: string;
  content: string;
  practitioner: string;
}

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './patient-detail.component.html'
})
export class PatientDetailComponent implements OnInit {
  // Patient actuel
  patient = signal<Patient | null>(null);
  
  // Notes du patient
  notes = signal<Note[]>([]);
  
  // Triggers détectés (pour l'évaluation du risque)
  triggers = signal<string[]>([]);
  
  // Modal ajout note
  showAddNoteModal = signal(false);
  newNoteContent = '';
  
  // Liste des triggers possibles pour le diabète
  allTriggers = [
    'Hémoglobine A1C',
    'Microalbumine',
    'Taille',
    'Poids',
    'Fumeur',
    'Fumeuse',
    'Anormal',
    'Cholestérol',
    'Vertiges',
    'Rechute',
    'Réaction',
    'Anticorps'
  ];

  // Données mockées
  private mockPatients: Patient[] = [
    { id: 1, firstName: 'Jean', lastName: 'Dupont', birthDate: '1985-03-15', gender: 'M', phone: '06 12 34 56 78', address: '15 Rue de la Paix, Paris', risk: 'None' },
    { id: 2, firstName: 'Marie', lastName: 'Martin', birthDate: '1972-08-22', gender: 'F', phone: '06 98 76 54 32', address: '8 Avenue des Champs, Lyon', risk: 'Borderline' },
    { id: 3, firstName: 'Pierre', lastName: 'Bernard', birthDate: '1990-01-10', gender: 'M', phone: '06 11 22 33 44', address: '22 Boulevard Victor Hugo, Marseille', risk: 'In Danger' },
    { id: 4, firstName: 'Sophie', lastName: 'Leroy', birthDate: '1968-12-05', gender: 'F', phone: '06 55 66 77 88', address: '5 Rue du Commerce, Bordeaux', risk: 'Early Onset' },
    { id: 5, firstName: 'Lucas', lastName: 'Moreau', birthDate: '1995-06-18', gender: 'M', phone: '06 99 88 77 66', address: '12 Place de la République, Toulouse', risk: 'None' }
  ];

  private mockNotes: Note[] = [
    { id: '1', patientId: 1, date: '2024-12-20', content: 'Patient en bonne santé. Aucun signe de complication. Contrôle de routine effectué. Taille et Poids normaux.', practitioner: 'Dr. Rousseau' },
    { id: '2', patientId: 1, date: '2024-11-15', content: 'Contrôle de routine. Taux de Cholestérol normal. Recommandation: maintenir l\'activité physique régulière.', practitioner: 'Dr. Rousseau' },
    { id: '3', patientId: 2, date: '2024-12-18', content: 'Hémoglobine A1C légèrement élevée. Microalbumine détectée. Surveillance recommandée.', practitioner: 'Dr. Martin' },
    { id: '4', patientId: 3, date: '2024-12-15', content: 'Patient Fumeur. Cholestérol élevé. Vertiges signalés. Poids au-dessus de la normale. Réaction anormale aux tests.', practitioner: 'Dr. Rousseau' },
    { id: '5', patientId: 4, date: '2024-12-10', content: 'Hémoglobine A1C très élevée. Anticorps détectés. Rechute constatée. Fumeuse depuis 20 ans. Taille stable mais Poids en augmentation.', practitioner: 'Dr. Martin' }
  ];

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    const patientId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadPatient(patientId);
    this.loadNotes(patientId);
  }

  /**
   * Charge les données du patient
   */
  loadPatient(id: number): void {
    const found = this.mockPatients.find(p => p.id === id);
    this.patient.set(found || null);
  }

  /**
   * Charge les notes du patient
   */
  loadNotes(patientId: number): void {
    const patientNotes = this.mockNotes.filter(n => n.patientId === patientId);
    this.notes.set(patientNotes);
    this.detectTriggers(patientNotes);
  }

  /**
   * Détecte les triggers dans les notes
   */
  detectTriggers(notes: Note[]): void {
    const allContent = notes.map(n => n.content.toLowerCase()).join(' ');
    const detected = this.allTriggers.filter(trigger => 
      allContent.includes(trigger.toLowerCase())
    );
    this.triggers.set(detected);
  }

  /**
   * Calcule l'âge
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
   * Formate la date
   */
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
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
   * Icône pour le niveau de risque
   */
  getRiskIcon(risk: string | undefined): string {
    const icons: Record<string, string> = {
      'None': '✓',
      'Borderline': '!',
      'In Danger': '⚠',
      'Early Onset': '⛔'
    };
    return icons[risk || 'None'] || icons['None'];
  }

  /**
   * Couleur de fond pour l'icône de risque
   */
  getRiskIconBg(risk: string | undefined): string {
    const classes: Record<string, string> = {
      'None': 'bg-emerald-100 text-emerald-600',
      'Borderline': 'bg-amber-100 text-amber-600',
      'In Danger': 'bg-orange-100 text-orange-600',
      'Early Onset': 'bg-rose-100 text-rose-600'
    };
    return classes[risk || 'None'] || classes['None'];
  }

  /**
   * Avatar classe
   */
  getAvatarClass(gender: string): string {
    return gender === 'M' ? 'from-cyan-500 to-blue-600' : 'from-pink-500 to-rose-600';
  }

  // --- Modal Note ---

  openAddNoteModal(): void {
    this.newNoteContent = '';
    this.showAddNoteModal.set(true);
  }

  closeAddNoteModal(): void {
    this.showAddNoteModal.set(false);
  }

  addNote(): void {
    if (!this.newNoteContent.trim() || !this.patient()) return;

    const newNote: Note = {
      id: Date.now().toString(),
      patientId: this.patient()!.id,
      date: new Date().toISOString().split('T')[0],
      content: this.newNoteContent,
      practitioner: 'Dr. Rousseau'
    };

    this.notes.update(notes => [newNote, ...notes]);
    this.detectTriggers([...this.notes()]);
    this.closeAddNoteModal();
  }

  /**
   * Recalcule le risque (simulation)
   */
  recalculateRisk(): void {
    const triggersCount = this.triggers().length;
    const age = this.calculateAge(this.patient()!.birthDate);
    const gender = this.patient()!.gender;
    
    let newRisk: 'None' | 'Borderline' | 'In Danger' | 'Early Onset' = 'None';

    // Logique simplifiée de calcul du risque
    if (age > 30) {
      if (triggersCount >= 6) newRisk = 'Early Onset';
      else if (triggersCount >= 4) newRisk = 'In Danger';
      else if (triggersCount >= 2) newRisk = 'Borderline';
    } else {
      if (gender === 'M') {
        if (triggersCount >= 5) newRisk = 'Early Onset';
        else if (triggersCount >= 3) newRisk = 'In Danger';
      } else {
        if (triggersCount >= 7) newRisk = 'Early Onset';
        else if (triggersCount >= 4) newRisk = 'In Danger';
      }
    }

    this.patient.update(p => p ? { ...p, risk: newRisk } : null);
    alert(`Risque recalculé : ${newRisk}`);
  }
}
