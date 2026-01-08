import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Interceptor d'authentification
 * 
 * Rôle :
 * - Ajoute automatiquement le token JWT à chaque requête HTTP
 * - Évite de répéter le header Authorization dans chaque service
 * 
 * Fonctionnement :
 * - Intercepte TOUTES les requêtes HTTP sortantes
 * - Si un token existe → ajoute "Authorization: Bearer <token>"
 * - Si pas de token → laisse passer la requête sans modification
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  if (token) {
    // Clone la requête et ajoute le header Authorization
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(cloned);
  }

  // Pas de token → requête non modifiée
  return next(req);
};
