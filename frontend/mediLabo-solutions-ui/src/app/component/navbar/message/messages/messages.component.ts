import { CommonModule, Location } from '@angular/common';
import { Component, computed, inject, TemplateRef } from '@angular/core';
import { AppStore } from '../../../../store/app.store';
import { DialogService } from '@ngneat/dialog';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MessageGroup } from '../../../../pipe/message.pipe';
import { PermissionService } from '../../../../service/permission.service';

@Component({
  selector: 'app-messages',
  imports: [CommonModule, FormsModule, RouterLink, MessageGroup],
  templateUrl: './messages.component.html',
  styleUrl: './messages.component.scss',
})
export class MessagesComponent {
  private readonly location = inject(Location);
  readonly store = inject(AppStore);
  readonly perm = inject(PermissionService);
  private dialogService = inject(DialogService);

  /**
   * Liste des destinataires disponibles :
   * - Staff/Admin : tous les users sauf soi-même
   * - USER (patient) : extraite des conversations existantes (personnes qui lui ont déjà écrit)
   */
  protected recipients = computed(() => {
    if (this.perm.canViewUsers()) {
      const users = this.store.users() ?? [];
      const myEmail = this.store.profile()?.email;
      return users.filter(u => u.email !== myEmail);
    }
    // Pour les patients : extraire les contacts depuis les conversations existantes
    return this.extractContactsFromMessages();
  });

  ngOnInit(): void {
    this.store.getMessages();
    // Charger la liste des users uniquement si permission
    if (this.perm.canViewUsers()) {
      this.store.getUsers();
    }
  }

  saveMessage = (form: NgForm) => {
    this.store.sendMessage(form.value);
    this.closeModal();
  };

  openModal = (template: TemplateRef<HTMLDivElement>) =>
    this.dialogService.open(template, { id: new Date().getTime().toString() });

  closeModal = () => this.dialogService.closeAll();

  /**
   * Extrait les contacts uniques depuis les messages existants.
   * Permet aux patients d'écrire aux personnes qui les ont déjà contactés.
   */
  private extractContactsFromMessages(): any[] {
    const messages = this.store.messages() ?? [];
    const myEmail = this.store.profile()?.email;
    const contactsMap = new Map<string, any>();

    for (const msg of messages) {
      const other = msg.sender?.email !== myEmail ? msg.sender : msg.receiver;
      if (other?.email && !contactsMap.has(other.email)) {
        contactsMap.set(other.email, {
          userUuid: other.userUuid,
          firstName: other.name?.split(' ')[0] ?? '',
          lastName: other.name?.split(' ').slice(1).join(' ') ?? '',
          email: other.email,
          role: other.role ?? ''
        });
      }
    }
    return [...contactsMap.values()];
  }
}