import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppStore } from '../../../../store/app.store';
//import { PermissionService } from '../../../../service/permission.service';

@Component({
  selector: 'app-notes',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './notes.component.html',
  styleUrl: './notes.component.scss',
})
export class NotesComponent {
  protected store = inject(AppStore);
  //protected permission = inject(PermissionService);

  searchQuery = signal('');
  pageSize = 10;

  // Formulaire création note
  showCreateForm = signal(false);
  selectedPatientUuid = signal('');
  noteContent = signal('');

  ngOnInit() {
    this.store.getAllNotes();
    this.store.getAllPatients();
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
  }

  // Enrichir notes avec le nom du patient
  enrichedNotes = computed(() => {
    const notes = this.store.allNotes() ?? [];
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

  // Filtrage
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

  // Pagination
  paginatedNotes = computed(() => {
    const start = (this.store.currentPage() ?? 0) * this.pageSize;
    return this.filteredNotes().slice(start, start + this.pageSize);
  });

  totalPages = computed(() => Math.ceil(this.filteredNotes().length / this.pageSize));

  // Stats
  totalNotes = computed(() => this.enrichedNotes().length);
  notesWithFiles = computed(() => this.enrichedNotes().filter(n => n.fileCount > 0).length);
  notesWithComments = computed(() => this.enrichedNotes().filter(n => n.commentCount > 0).length);
  recentNotes = computed(() => {
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
    return this.enrichedNotes().filter(n => new Date(n.createdAt) >= sevenDaysAgo).length;
  });

  onSearch(event: Event) {
    this.searchQuery.set((event.target as HTMLInputElement).value);
    this.store.setCurrentPage(0);
  }

  goToPage(page: number) { this.store.setCurrentPage(page); }
  previousPage() { const c = this.store.currentPage() ?? 0; if (c > 0) this.store.setCurrentPage(c - 1); }
  nextPage() { const c = this.store.currentPage() ?? 0; if (c < this.totalPages() - 1) this.store.setCurrentPage(c + 1); }

  deleteNote(noteUuid: string) {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette note ?')) {
      this.store.deleteNote(noteUuid);
    }
  }

  truncate(text: string, length: number = 80): string {
    if (!text) return '';
    return text.length > length ? text.substring(0, length) + '...' : text;
  }

  clearSearch() {
    this.searchQuery.set('');
    this.store.setCurrentPage(0);
  }
}
