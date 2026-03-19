import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { UserService } from '../../service/user.service';
import { StorageService } from '../../service/storage.service';
import { AppStore } from '../../store/app.store';
import { logoutUrl } from '../../utils/fileutils';

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
  private userService = inject(UserService);
  private storage = inject(StorageService);
  protected store = inject(AppStore);

  ngOnInit() {
    this.store?.getProfile();
    this.store?.getMessages();
    this.store?.getAllPatients();
  }

  onClick = (event: MouseEvent) => {
    const target = <HTMLElement>event.target;
    if (!target.closest('.usermenu')) this.isMenuOpen.set(false);
    if (!target.closest('.navmenu') && !target.closest('.burger-btn')) this.isNavOpen.set(false);
  };

  toggleMenu = () => this.isMenuOpen.update(isOpen => !isOpen);
  toggleNav = () => this.isNavOpen.update(isOpen => !isOpen);

  logOut = () => {
    this.userService.logOut();
    this.storage.removeRedirectUrl();
    window.location.href = logoutUrl;
  };
}