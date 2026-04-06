import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppStore } from '../../../../store/app.store';
import { IQuery, defaultQuery } from '../../../../interface/query';

@Component({
  selector: 'app-notes',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './notes.component.html',
  styleUrl: './notes.component.scss',
})
export class NotesComponent {
  protected store = inject(AppStore);

  searchQuery = signal('');
 pageSize = signal(10);

  // Formulaire création note
  showCreateForm = signal(false);
  selectedPatientUuid = signal('');
  noteContent = signal('');

  ngOnInit() {
    // Pagination backend pour la liste
    this.store.getAllNotesPageable({ ...defaultQuery, size: this.pageSize() });
    // Toutes les notes pour les stats
    this.store.getAllNotes();
    this.store.getAllPatients();
  }

changePageSize(event: Event) {
    const newSize = +(event.target as HTMLSelectElement).value;
    this.pageSize.set(newSize);
    this.store.getAllNotesPageable({ ...defaultQuery, page: 0, size: newSize });
}

  // Patient sélectionné (aperçu)
  selectedPatient = computed(() => {
    const uuid = this.selectedPatientUuid();
    if (!uuid) return null;
    const patients = this.store.allPatients() ?? [];
    return patients.find(p => p.patientUuid === uuid) ?? null;
  });

  toggleCreateForm() {
    this.showCreateForm.update(v => !v);
    this.selectedPatientUuid.set('');
    this.noteContent.set('');
  }

  onPatientSelected(event: Event) {
    this.selectedPatientUuid.set((event.target as HTMLSelectElement).value);
  }

  createNote() {
    const patientUuid = this.selectedPatientUuid();
    const content = this.noteContent().trim();
    if (!patientUuid || !content) return;
    this.store.createNote({ patientUuid, content });
    this.toggleCreateForm();
    // Recharger la page courante
    setTimeout(() => this.reloadCurrentPage(), 500);
  }

  // Notes enrichies depuis la page backend
  enrichedNotes = computed(() => {
    const notes = this.store.notePage()?.content ?? [];
    const patients = this.store.allPatients() ?? [];
    return notes.map(note => {
      const patient = patients.find(p => p.patientUuid === note.patientUuid);
      return {
        ...note,
        patientName: patient?.userInfo
          ? `${patient.userInfo.firstName} ${patient.userInfo.lastName}`
          : 'Patient inconnu',
        patientImageUrl: patient?.userInfo?.imageUrl || 'https://cdn-icons-png.flaticon.com/512/149/149071.png'
      };
    });
  });

  // Filtrage côté client sur la page courante
  filteredNotes = computed(() => {
    let notes = this.enrichedNotes();
    const query = this.searchQuery().toLowerCase();
    if (query) {
      notes = notes.filter(n =>
        n.content?.toLowerCase().includes(query) ||
        n.practitionerName?.toLowerCase().includes(query) ||
        n.patientName?.toLowerCase().includes(query)
      );
    }
    return notes;
  });

  // Pas de pagination côté client — la page vient du backend
  paginatedNotes = this.filteredNotes;

  // Pagination info depuis le backend
  currentPage = computed(() => this.store.notePage()?.currentPage ?? 0);
  totalPages = computed(() => this.store.notePage()?.totalPages ?? 0);
  totalElements = computed(() => this.store.notePage()?.totalElements ?? 0);

  // Stats basées sur allNotes (chargement complet séparé)
  allEnrichedNotes = computed(() => {
    const notes = this.store.allNotes() ?? [];
    const patients = this.store.allPatients() ?? [];
    return notes.map(note => {
      const patient = patients.find(p => p.patientUuid === note.patientUuid);
      return { ...note, patientName: patient?.userInfo ? `${patient.userInfo.firstName} ${patient.userInfo.lastName}` : '' };
    });
  });

  totalNotes = computed(() => this.allEnrichedNotes().length);
  notesWithFiles = computed(() => this.allEnrichedNotes().filter(n => n.fileCount > 0).length);
  notesWithComments = computed(() => this.allEnrichedNotes().filter(n => n.commentCount > 0).length);
  recentNotes = computed(() => {
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
    return this.allEnrichedNotes().filter(n => new Date(n.createdAt) >= sevenDaysAgo).length;
  });

  // Navigation
  goToPage(page: number) {
    this.store.getAllNotesPageable({ ...defaultQuery, page, size: this.pageSize() });
  }

  previousPage() {
    const c = this.currentPage();
    if (c > 0) this.goToPage(c - 1);
  }

  nextPage() {
    const c = this.currentPage();
    if (c < this.totalPages() - 1) this.goToPage(c + 1);
  }

  onSearch(event: Event) {
    this.searchQuery.set((event.target as HTMLInputElement).value);
  }

  clearSearch() {
    this.searchQuery.set('');
  }

  deleteNote(noteUuid: string) {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette note ?')) {
      this.store.deleteNote(noteUuid);
      setTimeout(() => this.reloadCurrentPage(), 500);
    }
  }

  truncate(text: string, length: number = 80): string {
    if (!text) return '';
    return text.length > length ? text.substring(0, length) + '...' : text;
  }

  private reloadCurrentPage() {
    this.store.getAllNotesPageable({ ...defaultQuery, page: this.currentPage(), size: this.pageSize() });
    this.store.getAllNotes();
}
}