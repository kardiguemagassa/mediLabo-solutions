import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Interceptor de gestion des erreurs HTTP
 * 
 * Rôle :
 * - Centralise la gestion des erreurs HTTP
 * - Gère automatiquement les erreurs 401 (non autorisé) → déconnexion
 * - Gère les erreurs 403 (interdit) → redirection
 * - Gère les erreurs 500 (serveur) → message d'erreur
 * 
 * Avantages :
 * - Évite de répéter le même code d'erreur dans chaque service
 * - Comportement cohérent dans toute l'application
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      
      switch (error.status) {
        case 401:
          // Non autorisé → token expiré ou invalide
          // Déconnecte l'utilisateur et redirige vers login
          authService.logout();
          router.navigate(['/login'], { 
            queryParams: { error: 'session_expired' } 
          });
          break;
          
        case 403:
          // Interdit → pas les droits nécessaires
          router.navigate(['/app/dashboard'], { 
            queryParams: { error: 'access_denied' } 
          });
          break;
          
        case 404:
          // Ressource non trouvée
          console.error('Ressource non trouvée:', req.url);
          break;
          
        case 500:
          // Erreur serveur
          console.error('Erreur serveur:', error.message);
          // Pourrait afficher une notification toast ici
          break;
          
        default:
          console.error('Erreur HTTP:', error.status, error.message);
      }

      // Propage l'erreur pour que le composant puisse aussi la gérer
      return throwError(() => error);
    })
  );
};
