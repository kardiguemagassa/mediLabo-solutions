import { Injectable, signal } from '@angular/core';

/**
 * Service de gestion du chargement global
 * 
 * Utilisé par loadingInterceptor pour afficher/masquer un spinner
 */
@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  // Signal réactif pour l'état de chargement
  isLoading = signal(false);
  
  private requestCount = 0;

  show(): void {
    this.requestCount++;
    this.isLoading.set(true);
  }

  hide(): void {
    this.requestCount--;
    if (this.requestCount <= 0) {
      this.requestCount = 0;
      this.isLoading.set(false);
    }
  }
}
