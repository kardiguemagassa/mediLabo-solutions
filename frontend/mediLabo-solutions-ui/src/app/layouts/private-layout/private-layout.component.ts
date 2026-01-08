import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-private-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './private-layout.component.html'
})
export class PrivateLayoutComponent {
  sidebarCollapsed = signal(false);
  
  navItems = [
    { path: '/app/dashboard', icon: '📊', label: 'Tableau de bord' },
    { path: '/app/patients', icon: '👥', label: 'Patients' },
    { path: '/app/notes', icon: '📝', label: 'Notes médicales' },
    { path: '/app/assessment', icon: '⚕️', label: 'Évaluation risque' },
    { path: '/app/settings', icon: '⚙️', label: 'Paramètres' }
  ];

  constructor(public authService: AuthService, private router: Router) {}

  toggleSidebar(): void {
    this.sidebarCollapsed.update(v => !v);
  }

  logout(): void {
    this.authService.logout();
  }

  getPageTitle(): string {
    const url = this.router.url;
    if (url.includes('dashboard')) return 'Tableau de bord';
    if (url.includes('patients')) return 'Gestion des patients';
    if (url.includes('notes')) return 'Notes médicales';
    if (url.includes('assessment')) return 'Évaluation des risques';
    if (url.includes('settings')) return 'Paramètres';
    return 'MediLabo';
  }
}
