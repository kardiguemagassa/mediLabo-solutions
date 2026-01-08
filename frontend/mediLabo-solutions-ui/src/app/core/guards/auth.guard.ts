import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard de protection des routes authentifiées
 * 
 * Utilité :
 * - Protège les routes qui nécessitent une connexion (dashboard, patients, notes, etc.)
 * - Redirige vers /login si l'utilisateur n'est pas connecté
 * - Sauvegarde l'URL demandée pour rediriger après connexion (returnUrl)
 * 
 * Utilisation dans les routes :
 * {
 *   path: 'app',
 *   canActivate: [authGuard],
 *   loadChildren: () => import('./features/app/app.routes').then(r => r.APP_ROUTES)
 * }
 */
export const authGuard: CanActivateFn = (route, state) => {
  // Injection des dépendances (Angular 15+ style fonctionnel)
  const authService = inject(AuthService);
  const router = inject(Router);

  // Vérifie si l'utilisateur est authentifié (token valide en localStorage)
  if (authService.isAuthenticated()) {
    return true; // Autorise l'accès à la route
  }

  // Non authentifié : redirige vers login avec l'URL d'origine en paramètre
  // Exemple: /app/patients → /login?returnUrl=/app/patients
  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false; // Bloque l'accès à la route
};
