import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppStore } from '../../../../store/app.store';

@Component({
  selector: 'app-notes',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './notes.component.html',
  styleUrl: './notes.component.scss',
})
export class NotesComponent {
  protected store = inject(AppStore);

  searchQuery = signal('');
  pageSize = 10;

  ngOnInit() {
    this.store.getAllNotes();
    this.store.getAllPatients();
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