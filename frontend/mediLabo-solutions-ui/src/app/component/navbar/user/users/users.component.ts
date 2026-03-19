import { Component, inject } from '@angular/core';
import { AppStore } from '../../../../store/app.store';
import { CommonModule, Location } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-users',
  imports: [CommonModule, RouterLink],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss',
})
export class UsersComponent {
  readonly store = inject(AppStore);
  private readonly location = inject(Location);

  ngOnInit(): void {
    this.store?.getUsers();
  }

  goBack = () => this.location.back();

}
