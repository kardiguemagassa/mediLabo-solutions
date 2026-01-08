import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  specialty?: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8080/api/auth';
  private readonly TOKEN_KEY = 'medilabo_token';
  private readonly USER_KEY = 'medilabo_user';

  currentUser = signal<User | null>(null);

  constructor(private http: HttpClient, private router: Router) {
    this.loadUserFromStorage();
  }

  private loadUserFromStorage(): void {
    const userJson = localStorage.getItem(this.USER_KEY);
    if (userJson) {
      this.currentUser.set(JSON.parse(userJson));
    }
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    // TODO: Remplacer par vrai appel API quand le backend sera prêt
    // return this.http.post<AuthResponse>(`${this.API_URL}/login`, credentials)

    // Mock pour le développement → accepte N'IMPORTE QUEL email/mot de passe
    return new Observable(observer => {
      setTimeout(() => {
        const mockResponse: AuthResponse = {
          token: 'mock-jwt-token-' + Date.now(),
          user: {
            id: 1,
            email: credentials.email,
            firstName: 'Dr.',
            lastName: 'Rousseau',
            role: 'PRACTITIONER'
          }
        };
        this.setSession(mockResponse);
        observer.next(mockResponse);
        observer.complete();
      }, 500);
    });
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    // TODO: Remplacer par vrai appel API
    return new Observable(observer => {
      setTimeout(() => {
        const mockResponse: AuthResponse = {
          token: 'mock-jwt-token-' + Date.now(),
          user: {
            id: 1,
            email: data.email,
            firstName: data.firstName,
            lastName: data.lastName,
            role: 'PRACTITIONER'
          }
        };
        this.setSession(mockResponse);
        observer.next(mockResponse);
        observer.complete();
      }, 500);
    });
  }

  private setSession(authResult: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, authResult.token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(authResult.user));
    this.currentUser.set(authResult.user);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    return !!token;
  }
}
