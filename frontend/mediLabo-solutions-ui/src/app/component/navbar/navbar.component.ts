import { CommonModule } from '@angular/common';
import { Component, computed, HostListener, inject, signal, TemplateRef, WritableSignal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { DialogService } from '@ngneat/dialog';
import { UserService } from '../../service/user.service';
import { StorageService } from '../../service/storage.service';
import { AppStore } from '../../store/app.store';
import { logoutUrl } from '../../utils/fileutils';
import { PermissionService } from '../../service/permission.service';

@Component({
  selector: 'app-navbar',
  imports: [RouterOutlet, CommonModule, RouterLink, RouterLinkActive, FormsModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
  host: { '(document:click)': 'onClick($event)' }
})
export class NavbarComponent {
  isNavOpen = signal(false);
  isMenuOpen = signal(false);
  filesToSave: File[] = [];
  files = signal<{ name: string, size: string }[]>([]);
  private dialogService = inject(DialogService);
  private userService = inject(UserService);
  private storage = inject(StorageService);
  protected store = inject(AppStore);
  protected permission = inject(PermissionService);

  //patients = computed(() => this.store.allPatients() ?? []);
  today = new Date();
  noteFilesToSave: File[] = [];
  noteFiles = signal<{ name: string, size: string }[]>([]);

  users = computed(() => this.store.users() ?? []);

  allPatients = computed(() => {
    const patients = this.store.allPatients() ?? [];
    return patients.map(patient => ({
      ...patient,
      firstName: patient.userInfo?.firstName ?? '',
      lastName: patient.userInfo?.lastName ?? ''
    }));
  });

  ngOnInit() {
    this.store?.getProfile();
    this.store?.getMessages();
    this.store?.getAllPatients();
    this.store?.getUsers(); 
  }

  onFileChange = (files: FileList) => {
    const fileArray: { name: string, size: string }[] = [];
    Array.from(files).forEach(file => {
      this.filesToSave.push(file);
    });
    this.files.set(fileArray);
  };

  onNoteFileChange = (event: Event) => {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    const fileArray: { name: string, size: string }[] = [];
    Array.from(input.files).forEach(file => {
      this.noteFilesToSave.push(file);
      fileArray.push({ name: file.name, size: `${(file.size / 1024).toFixed(1)} KB` });
    });
    this.noteFiles.set([...this.noteFiles(), ...fileArray]);
  };

  removeFile = (file: File) => {
    this.files.set([...this.files().filter(currentFile => currentFile.name !== file.name)]);
    this.filesToSave = [...this.filesToSave.filter(currentFile => currentFile.name !== file.name)];
  };

  removeNoteFile = (file: { name: string, size: string }) => {
    this.noteFiles.set([...this.noteFiles().filter(currentFile => currentFile.name !== file.name)]);
    this.noteFilesToSave = [...this.noteFilesToSave.filter(currentFile => currentFile.name !== file.name)];
  };

  savePatient = (patientForm: NgForm) => {
    const data = patientForm.value;
    if (!data.userUuid || !data.dateOfBirth || !data.gender) return;
    this.store.createPatient(data);
    patientForm.resetForm();
    this.files.set([]);
    this.filesToSave = [];
    this.closeModal();
};

  saveNote = (noteForm: NgForm) => {
    const { patientUuid, content } = noteForm.value;
    if (!patientUuid || !content) return;
    this.store.createNote({ patientUuid, content });
    noteForm.resetForm();
    this.noteFiles.set([]);
    this.noteFilesToSave = [];
    this.closeModal();
  };

  onClick = (event: MouseEvent) => {
    const target = <HTMLElement>event.target;
    const usermenu = target.closest('.usermenu');
    if (!usermenu) {
      this.isMenuOpen.set(false);
    }
    const navmenu = target.closest('.navmenu');
    if (!navmenu) {
      this.isNavOpen.set(false);
    }
  };

  openModal = (templage: TemplateRef<HTMLDivElement>) => this.dialogService.open(templage, { id: new Date().getTime().toString() });

  closeModal = () => this.dialogService.closeAll();

  toggleMenu = () => this.isMenuOpen.update(isOpen => !isOpen);

  toggleNav = () => this.isNavOpen.update(isOpen => !isOpen);

  logOut = () => {
    this.userService.logOut();
    this.storage.removeRedirectUrl();
    window.location.href = logoutUrl;
  };
}