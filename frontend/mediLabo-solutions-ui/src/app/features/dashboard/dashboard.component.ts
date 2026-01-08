import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent {
  stats = [
    { label: 'Total Patients', value: '1,247', change: '+12%', positive: true, icon: '👥', color: 'from-cyan-500 to-blue-600' },
    { label: 'Consultations', value: '384', change: '+8%', positive: true, icon: '📅', color: 'from-violet-500 to-purple-600' },
    { label: 'Risque élevé', value: '23', change: '-5%', positive: false, icon: '⚠️', color: 'from-amber-500 to-orange-600' },
    { label: 'Notes ajoutées', value: '156', change: '+24%', positive: true, icon: '📝', color: 'from-emerald-500 to-teal-600' }
  ];

  riskDistribution = [
    { label: 'None', percent: 65, color: 'bg-emerald-500' },
    { label: 'Borderline', percent: 20, color: 'bg-amber-500' },
    { label: 'In Danger', percent: 10, color: 'bg-orange-500' },
    { label: 'Early Onset', percent: 5, color: 'bg-rose-500' }
  ];

  recentActivity = [
    { action: 'Nouvelle note ajoutée', patient: 'Jean Dupont', time: 'Il y a 5 min', icon: '📝' },
    { action: 'Évaluation effectuée', patient: 'Marie Martin', time: 'Il y a 1h', icon: '⚕️' },
    { action: 'Patient enregistré', patient: 'Lucas Moreau', time: 'Il y a 2h', icon: '👤' },
    { action: 'Risque mis à jour', patient: 'Sophie Leroy', time: 'Il y a 3h', icon: '⚠️' }
  ];
}
