import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './landing.component.html'
})
export class LandingComponent {
  features = [
    { icon: '👥', title: 'Gestion Patients', description: 'Centralisez toutes les informations de vos patients : identité, historique, coordonnées.', color: 'from-cyan-500 to-blue-600' },
    { icon: '📝', title: 'Notes Médicales', description: 'Rédigez et consultez les notes de consultation pour chaque patient facilement.', color: 'from-violet-500 to-purple-600' },
    { icon: '⚕️', title: 'Évaluation Risque', description: 'Détectez automatiquement les risques de diabète grâce à l\'analyse intelligente.', color: 'from-amber-500 to-orange-600' },
    { icon: '🔒', title: 'Sécurité Maximale', description: 'Vos données sont chiffrées et protégées selon les normes médicales.', color: 'from-emerald-500 to-teal-600' }
  ];

  steps = [
    { icon: '📋', title: 'Créez votre compte', description: 'Inscrivez-vous gratuitement en quelques secondes.' },
    { icon: '👤', title: 'Ajoutez vos patients', description: 'Enregistrez les informations et ajoutez des notes.' },
    { icon: '📊', title: 'Évaluez les risques', description: 'Notre algorithme analyse et détecte les facteurs de risque.' }
  ];

  testimonials = [
    { name: 'Dr. Rousseau', role: 'Médecin généraliste, Paris', initials: 'DR', color: 'from-cyan-500 to-blue-600', text: 'MediLabo a transformé ma pratique. L\'évaluation automatique des risques me permet de détecter les patients à risque beaucoup plus tôt.' },
    { name: 'Dr. Martin-Leblanc', role: 'Endocrinologue, Lyon', initials: 'ML', color: 'from-violet-500 to-purple-600', text: 'Interface intuitive et sécurisée. Je recommande MediLabo à tous mes confrères.' },
    { name: 'Dr. Petit-Dubois', role: 'Diabétologue, Marseille', initials: 'PD', color: 'from-emerald-500 to-teal-600', text: 'L\'analyse des notes cliniques est bluffante. J\'ai pu identifier des cas de pré-diabète.' }
  ];

  riskLevels = [
    { icon: '✓', name: 'None', color: 'emerald', description: 'Aucun risque détecté.' },
    { icon: '!', name: 'Borderline', color: 'amber', description: 'Surveillance recommandée.' },
    { icon: '⚠', name: 'In Danger', color: 'orange', description: 'Action préventive conseillée.' },
    { icon: '⛔', name: 'Early Onset', color: 'rose', description: 'Intervention urgente requise.' }
  ];
}
