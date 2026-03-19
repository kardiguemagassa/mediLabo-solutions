import { Component, DestroyRef, inject, signal, ViewContainerRef } from '@angular/core';
import { AppStore } from '../../../store/app.store';
import { CommonModule, Location } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { HotToastService } from '@ngxpert/hot-toast';
import { ModalService } from '../../../service/modal.service';
import { EMPTY, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Settings } from '../../../enum/settings.enum';
import { getFileFormData } from '../../../utils/fileutils';

@Component({
  selector: 'app-profile',
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent {
  Settings = Settings;
  readonly mode = signal<'view' | 'edit'>('view');
  readonly store = inject(AppStore);
  private destroyRef = inject(DestroyRef);
  private readonly location = inject(Location);
  private toastService = inject(HotToastService);
  private viewRef = inject(ViewContainerRef);
  private modalService = inject(ModalService);

  showCurrentPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;

  ngOnInit(): void {
    if (!this.store.profile) {
      this.store.getProfile();
    }
  }

  goBack = () => this.location.back();

  switchMode = () => this.mode() == 'view' ? this.mode.update(_mode => 'edit') : this.mode.update(_mode => 'view');

  updatePhoto = (file: File) => this.store.updatePhoto(getFileFormData(file))

  toggleMfa = (mode: 'enable' | 'disable') => mode === 'enable' ? this.store.enableMfa() : this.store.disableMfa();

  updateRole = (role: string) => this.store.updateRole(role);

  updateUser = (form: NgForm) => {
    this.store.updateUser(form.value);
    this.mode.update(_mode => 'view');
  };

  updatePassword = (form: NgForm) => {
    this.store.updatePassword(form.value);
    form.reset();
  };

  toggleSettings = (settings: Settings) => {
    if (this.store.profile().role === 'ADMIN' || this.store.profile().role === 'SUPER_ADMIN') {
      switch (settings) {
        case Settings.EXPIRED:
          this.toggleAccountExpired();
          break;
        case Settings.LOCKED:
          this.toggleAccountLocked();
          break;
        case Settings.ENABLED:
          this.toggleAccountEnabled();
          break;
      }
    } else {
      this.toastService.error(`Vous n'êtes pas autorisé à modifier les paramètres du compte.`);
    }
  };

  togglePasswordVisibility(field: 'current' | 'new' | 'confirm') {
    switch(field) {
      case 'current':
        this.showCurrentPassword = !this.showCurrentPassword;
        break;
      case 'new':
        this.showNewPassword = !this.showNewPassword;
        break;
      case 'confirm':
        this.showConfirmPassword = !this.showConfirmPassword;
        break;
    }
  }

  private toggleAccountExpired = () => {
      this.modalService
        .open(this.viewRef, { message: `Êtes-vous sûr de vouloir modifier le compte expiré ?`, type: 'warning', subtitle: `La mise à jour des paramètres du compte peut modifier l'accès utilisateur.` })
        .pipe(
          switchMap(() => {
            this.store.toggleAccountExpired();
            return EMPTY;
          }), takeUntilDestroyed(this.destroyRef)).subscribe();
    };

    private toggleAccountLocked = () => {
      this.modalService
        .open(this.viewRef, { message: `Êtes-vous sûr de vouloir modifier le compte verrouillé ?`, type: 'warning', subtitle: `La mise à jour des paramètres du compte peut modifier l'accès des utilisateurs.` })
        .pipe(
          switchMap(() => {
            this.store.toggleAccountLocked();
            return EMPTY;
          }), takeUntilDestroyed(this.destroyRef)).subscribe();
    };

    private toggleAccountEnabled = () => {
      this.modalService
        .open(this.viewRef, { message: `Êtes-vous sûr de vouloir modifier le compte activé ?`, type: 'warning', subtitle: `La mise à jour des paramètres du compte peut modifier l'accès des utilisateurs.` })
        .pipe(
          switchMap(() => {
            this.store.toggleAccountEnabled();
            return EMPTY;
          }), takeUntilDestroyed(this.destroyRef)).subscribe();
    };

}