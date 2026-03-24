import { CommonModule, Location } from '@angular/common';
import { Component, computed, inject, TemplateRef } from '@angular/core';
import { AppStore } from '../../../../store/app.store';
import { DialogService } from '@ngneat/dialog';
import { FormsModule, NgForm } from '@angular/forms';
import { getFormData } from '../../../../utils/fileutils';
import { RouterLink } from '@angular/router';
import { MessageGroup } from '../../../../pipe/message.pipe';

@Component({
  selector: 'app-messages',
  imports: [CommonModule, FormsModule, RouterLink, MessageGroup],
  templateUrl: './messages.component.html',
  styleUrl: './messages.component.scss',
})
export class MessagesComponent {
  private readonly location = inject(Location);
  readonly store = inject(AppStore);
  private dialogService = inject(DialogService);

  protected recipients = computed(() => {
    const users = this.store.users() ?? [];
    const myEmail = this.store.profile()?.email;
    return users.filter(u => u.email !== myEmail);
  });

  ngOnInit(): void {
    this.store?.getMessages();
    this.store?.getUsers();
  }

  saveMessage = (form: NgForm) => {
    this.store.sendMessage(form.value);
    this.closeModal();
  };
  
  openModal = (templage: TemplateRef<HTMLDivElement>) => this.dialogService.open(templage, { id: new Date().getTime().toString() });
  
  closeModal = () => this.dialogService.closeAll();

}
