import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { LoadingService } from '../services/loading.service';

/**
 * Interceptor de chargement
 * 
 * Rôle :
 * - Affiche automatiquement un spinner pendant les requêtes HTTP
 * - Masque le spinner quand la requête est terminée
 * 
 * Avantages :
 * - Pas besoin de gérer isLoading dans chaque composant
 * - UX cohérente dans toute l'application
 */
export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const loadingService = inject(LoadingService);
  
  // Démarre le chargement
  loadingService.show();

  return next(req).pipe(
    finalize(() => {
      // Arrête le chargement (succès ou erreur)
      loadingService.hide();
    })
  );
};
