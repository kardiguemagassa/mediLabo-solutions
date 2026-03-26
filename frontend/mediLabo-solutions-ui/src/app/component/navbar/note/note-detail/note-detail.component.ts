import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AppStore } from '../../../../store/app.store';
import { NoteService } from '../../../../service/note.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { PermissionService } from '../../../../service/permission.service';

@Component({
  selector: 'app-note-detail',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './note-detail.component.html',
  styleUrl: './note-detail.component.scss',
})
export class NoteDetailComponent {
  protected store = inject(AppStore);
  private route = inject(ActivatedRoute);
  private noteService = inject(NoteService);
  private sanitizer = inject(DomSanitizer);
  private cdr = inject(ChangeDetectorRef);
  protected permissionService = inject(PermissionService);

  // Édition
  isEditing = signal(false);
  editContent = signal('');

  // Commentaire
  newComment = signal('');
  editingCommentUuid = signal<string | null>(null);
  editCommentContent = signal('');

  // Upload
  filesToUpload: File[] = [];
  pendingFiles = signal<{ name: string, size: string, type: string, previewUrl: string | null }[]>([]);

  // Preview modal
  previewFile = signal<{ fileName: string, fileUuid: string, fileType: string, url: SafeResourceUrl | null } | null>(null);
  previewLoading = signal(false);

  // Note enrichie avec patientName/patientImageUrl
  note = computed(() => {
    const note = this.store.noteDetail();
    if (!note) return null;

    // Staff : enrichir via allPatients
    if (this.permissionService.isStaff()) {
      const patients = this.store.allPatients() ?? [];
      const patient = patients.find(p => p.patientUuid === note.patientUuid);
      return {
        ...note,
        patientName: patient?.userInfo
          ? `${patient.userInfo.firstName} ${patient.userInfo.lastName}`
          : 'Patient inconnu',
        patientImageUrl: patient?.userInfo?.imageUrl || 'https://cdn-icons-png.flaticon.com/512/149/149071.png'
      };
    }

    // Patient : utiliser le profil connecté
    const profile = this.store.profile();
    return {
      ...note,
      patientName: profile ? `${profile.firstName} ${profile.lastName}` : 'Mon dossier',
      patientImageUrl: profile?.imageUrl || 'https://cdn-icons-png.flaticon.com/512/149/149071.png'
    };
  });

  noteUuid = '';

  ngOnInit() {
    this.noteUuid = this.route.snapshot.params['noteUuid'];
    this.store.getNote(this.noteUuid);
    if (this.permissionService.isStaff()) {
      this.store.getAllPatients();
    }
  }

  startEditing() {
    this.editContent.set(this.note()?.content ?? '');
    this.isEditing.set(true);
  }

  cancelEditing() {
    this.isEditing.set(false);
    this.editContent.set('');
  }

  saveNote() {
    const content = this.editContent();
    if (!content.trim()) return;
    const patientUuid = this.note()?.patientUuid;
    this.store.updateNote({ noteUuid: this.noteUuid, note: { patientUuid, content } });
    this.isEditing.set(false);
  }

  //  FICHIERS 

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    const fileArray: { name: string, size: string, type: string, previewUrl: string | null }[] = [];
    Array.from(input.files).forEach(file => {
      this.filesToUpload.push(file);
      const isPreviewable = file.type.startsWith('image/') || file.type === 'application/pdf';
      fileArray.push({
        name: file.name,
        size: `${(file.size / 1024).toFixed(1)} KB`,
        type: file.type,
        previewUrl: isPreviewable ? URL.createObjectURL(file) : null
      });
    });
    this.pendingFiles.set([...this.pendingFiles(), ...fileArray]);
    input.value = '';
  }

  removePendingFile(file: { name: string, size: string, type: string, previewUrl: string | null }) {
    if (file.previewUrl) URL.revokeObjectURL(file.previewUrl);
    this.pendingFiles.set(this.pendingFiles().filter(f => f.name !== file.name));
    this.filesToUpload = this.filesToUpload.filter(f => f.name !== file.name);
  }

  uploadFiles() {
    this.pendingFiles().forEach(f => { if (f.previewUrl) URL.revokeObjectURL(f.previewUrl); });
    this.filesToUpload.forEach(file => {
      const formData = new FormData();
      formData.append('file', file);
      this.store.uploadFile({ noteUuid: this.noteUuid, file: formData });
    });
    this.filesToUpload = [];
    this.pendingFiles.set([]);
  }

  isImage(type: string): boolean {
    return type.startsWith('image/');
  }

  getSafeUrl(url: string | null): SafeResourceUrl | null {
    if (!url) return null;
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  downloadFile(fileUuid: string, fileName: string, event?: Event) {
    event?.stopPropagation();
    this.noteService.downloadFile$(this.noteUuid, fileUuid).subscribe({
      next: (response: any) => {
        const blob = response.body;
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        a.click();
        window.URL.revokeObjectURL(url);
      }
    });
  }

  openPreview(file: any, event?: Event) {
    event?.stopPropagation();
    if (this.previewLoading()) return;

    const name = file.name || file.fileName || '';
    const ext = file.extension || (name.split('.').pop()?.toLowerCase() ?? '');
    const mimeType = file.contentType || file.fileType || '';

    const isPreviewable =
      ['pdf', 'jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext) ||
      mimeType.startsWith('image/') ||
      mimeType === 'application/pdf';

    if (!isPreviewable) {
      this.downloadFile(file.fileUuid, name);
      return;
    }

    this.previewLoading.set(true);
    this.previewFile.set({ fileName: name, fileUuid: file.fileUuid, fileType: ext, url: null });
    this.cdr.detectChanges();

    this.noteService.downloadFile$(this.noteUuid, file.fileUuid).subscribe({
      next: (response: any) => {
        const blob = response.body;
        const blobUrl = URL.createObjectURL(blob);
        this.previewFile.set({
          fileName: name,
          fileUuid: file.fileUuid,
          fileType: ext,
          url: this.sanitizer.bypassSecurityTrustResourceUrl(blobUrl)
        });
        this.previewLoading.set(false);
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.previewLoading.set(false);
        this.previewFile.set(null);
        this.cdr.detectChanges();
      }
    });
  }

  closePreview() {
    this.previewFile.set(null);
  }

  isImageFile(fileType: string): boolean {
    return ['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(fileType);
  }

  deleteFile(fileUuid: string, event?: Event) {
    event?.stopPropagation();
    if (confirm('Supprimer ce fichier ?')) {
      this.store.deleteFile({ noteUuid: this.noteUuid, fileUuid });
    }
  }

  getFileIcon(fileName: string): string {
    const ext = fileName?.split('.').pop()?.toLowerCase();
    if (['pdf'].includes(ext ?? '')) return 'text-red-500';
    if (['doc', 'docx'].includes(ext ?? '')) return 'text-blue-500';
    if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext ?? '')) return 'text-emerald-500';
    return 'text-gray-500';
  }

  //  COMMENTAIRES 

  addComment() {
    const content = this.newComment().trim();
    if (!content) return;
    this.store.addComment({ noteUuid: this.noteUuid, comment: { content } });
    this.newComment.set('');
  }

  startEditComment(commentUuid: string, content: string) {
    this.editingCommentUuid.set(commentUuid);
    this.editCommentContent.set(content);
  }

  cancelEditComment() {
    this.editingCommentUuid.set(null);
    this.editCommentContent.set('');
  }

  saveComment(commentUuid: string) {
    const content = this.editCommentContent().trim();
    if (!content) return;
    this.store.updateComment({ noteUuid: this.noteUuid, commentUuid, comment: { content } });
    this.editingCommentUuid.set(null);
    this.editCommentContent.set('');
  }

  deleteComment(commentUuid: string) {
    if (confirm('Supprimer ce commentaire ?')) {
      this.store.deleteComment({ noteUuid: this.noteUuid, commentUuid });
    }
  }
}