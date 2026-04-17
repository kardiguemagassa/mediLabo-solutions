import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { AppStore } from '../../../../store/app.store';
import { Location } from '@angular/common';
import { RouterLink } from '@angular/router';
import { defaultQuery } from '../../../../interface/query';

@Component({
  selector: 'app-users',
  imports: [CommonModule, RouterLink],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss',
})
export class UsersComponent {
  readonly store = inject(AppStore);
  private readonly location = inject(Location);

  pageSize = signal(10);

  ngOnInit(): void {
    this.store.getUsersPageable({ ...defaultQuery, size: this.pageSize() });
    this.store.getUsers(); // pour les stats
  }

  // Pagination depuis le backend
  users = computed(() => this.store.userPage()?.content ?? []);
  currentPage = computed(() => this.store.userPage()?.currentPage ?? 0);
  totalPages = computed(() => this.store.userPage()?.totalPages ?? 0);
  totalElements = computed(() => this.store.userPage()?.totalElements ?? 0);

  goToPage(page: number) {
    this.store.getUsersPageable({ ...defaultQuery, page, size: this.pageSize() });
  }

  previousPage() {
    if (this.currentPage() > 0) this.goToPage(this.currentPage() - 1);
  }

  nextPage() {
    if (this.currentPage() < this.totalPages() - 1) this.goToPage(this.currentPage() + 1);
  }

  changePageSize(event: Event) {
    const newSize = +(event.target as HTMLSelectElement).value;
    this.pageSize.set(newSize);
    this.store.getUsersPageable({ ...defaultQuery, page: 0, size: newSize });
  }

  goBack = () => this.location.back();
}