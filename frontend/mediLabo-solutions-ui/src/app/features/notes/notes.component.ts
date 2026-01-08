import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

interface Note {
  id: string;
  patientId: number;
  patientName: string;
  date: string;
  content: string;
  practitioner: string;
}

@Component({
  selector: 'app-notes',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './notes.component.html'
})
export class NotesComponent {
  searchQuery = '';

  notes = signal<Note[]>([
    { id: '1', patientId: 1, patientName: 'Jean Dupont', date: '2024-12-20', content: 'Patient en bonne santé. Aucun signe de complication. Contrôle de routine effectué.', practitioner: 'Dr. Rousseau' },
    { id: '2', patientId: 2, patientName: 'Marie Martin', date: '2024-12-18', content: 'Hémoglobine A1C légèrement élevée. Microalbumine détectée. Surveillance recommandée.', practitioner: 'Dr. Martin' },
    { id: '3', patientId: 3, patientName: 'Pierre Bernard', date: '2024-12-15', content: 'Patient Fumeur. Cholestérol élevé. Vertiges signalés.', practitioner: 'Dr. Rousseau' },
    { id: '4', patientId: 4, patientName: 'Sophie Leroy', date: '2024-12-10', content: 'Hémoglobine A1C très élevée. Anticorps détectés. Rechute constatée.', practitioner: 'Dr. Martin' },
    { id: '5', patientId: 1, patientName: 'Jean Dupont', date: '2024-11-15', content: 'Contrôle de routine. Taux de Cholestérol normal.', practitioner: 'Dr. Rousseau' }
  ]);

  get filteredNotes(): Note[] {
    if (!this.searchQuery) return this.notes();
    const query = this.searchQuery.toLowerCase();
    return this.notes().filter(note =>
      note.patientName.toLowerCase().includes(query) ||
      note.content.toLowerCase().includes(query)
    );
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
  }

  /**
   * Génère les initiales à partir d'un nom complet
   * Ex: "Jean Dupont" → "JD", "Marie" → "M"
   */
  getInitials(name: string): string {
    if (!name) return '?';
    const parts = name.trim().split(' ').filter(p => p.length > 0);
    if (parts.length === 0) return '?';
    if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
  }
}
