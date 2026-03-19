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
      this.store.getUser(this.userUuid);
    }
  }

  goBack = () => this.location.back();

  switchMode = () => this.mode() == 'view' ? this.mode.update(_mode => 'edit') : this.mode.update(_mode => 'view');
  
  updateUser = (form: NgForm) => {
    console.log(form.value)
  };
}
