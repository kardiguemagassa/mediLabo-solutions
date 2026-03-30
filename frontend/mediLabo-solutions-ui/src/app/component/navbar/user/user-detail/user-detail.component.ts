import { Component, inject, input, signal } from '@angular/core';
import { AppStore } from '../../../../store/app.store';
import { CommonModule, Location } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';

@Component({
  selector: 'app-user-detail',
  imports: [CommonModule, FormsModule],
  templateUrl: './user-detail.component.html',
  styleUrl: './user-detail.component.scss',
})
export class UserDetailComponent {
  userUuid = input<string>('');
  readonly mode = signal<'view' | 'edit'>('view');
  readonly store = inject(AppStore);
  private readonly location = inject(Location);

  ngOnInit(): void {
    if (this.userUuid()) {
      this.store.getUser(this.userUuid());  
    }
  }

  onRoleChange = (event: Event) => {
    const role = (event.target as HTMLSelectElement).value;
    const userUuid = this.store.user()?.userUuid;
    if (userUuid && confirm(`Changer le rôle en ${role} ?`)) {
        this.store.updateRoleByUuid({ userUuid, role });
    }
  };

  toggleAccountLocked = () => {
    const userUuid = this.store.user()?.userUuid;
    if (userUuid) this.store.toggleAccountLockedByUuid(userUuid);
  };

  toggleAccountExpired = () => {
    const userUuid = this.store.user()?.userUuid;
    if (userUuid) this.store.toggleAccountExpiredByUuid(userUuid);
  };

  toggleAccountEnabled = () => {
    const userUuid = this.store.user()?.userUuid;
    if (userUuid) this.store.toggleAccountEnabledByUuid(userUuid);
  };

  toggleMfa = () => {
    const user = this.store.user();
    if (!user?.userUuid) return;
    if (user.mfa) {
        this.store.disableMfaByUuid(user.userUuid);
    } else {
        this.store.enableMfaByUuid(user.userUuid);
    }
  };

  goBack = () => this.location.back();

  switchMode = () => this.mode() == 'view' ? this.mode.update(_mode => 'edit') : this.mode.update(_mode => 'view');
  
  updateUser = (form: NgForm) => {
    const userUuid = this.store.user()?.userUuid;
    if (userUuid) {
        this.store.updateUser({ ...form.value, userUuid });
        this.mode.set('view');
    }
  };
}
