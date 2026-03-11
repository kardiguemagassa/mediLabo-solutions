import { inject, Injectable } from '@angular/core';
import { JwtHelperService } from '@auth0/angular-jwt';
import { StorageService } from './storage.service';
import { Key } from '../enum/cache.key';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, Observable, tap, throwError } from 'rxjs';
import { server } from '../utils/fileutils';
import { IResponse } from '../interface/response';

import { authorizationserver } from '../utils/fileutils';
import { IAuthentication } from '../interface/IAuthentication';

@Injectable()
export class UserService {
  private jwtToken = new JwtHelperService();
  private storage = inject(StorageService);
  private http = inject(HttpClient);

  constructor() {}

  register$ = (user: any) =>
    <Observable<IResponse>>(
      this.http
        .post<IResponse>(`${server}/user/register`, user)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  verifyAccountToken$ = (token: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/user/verify/account?token=${token}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  verifyPasswordToken$ = (token: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/user/verify/password?token=${token}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  resetPassword$ = (form: FormData) =>
    <Observable<IResponse>>(
      this.http
        .post<IResponse>(`${server}/user/resetpassword`, form)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  createNewPassword$ = (request: {
    userUuid: string;
    token: string;
    password: string;
    confirmPassword: string;
  }) =>
    <Observable<IResponse>>(
      this.http
        .post<IResponse>(`${server}/user/resetpassword/reset`, request)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  validateCode$ = (form: FormData) => <Observable<IAuthentication>>
    this.http.post<IAuthentication>
      (`${authorizationserver}/oauth2/token`, form)
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );




      
  isAuthenticated = (): boolean =>
    this.jwtToken.decodeToken<string>(this.storage.get(Key.TOKEN))
      ? true
      : false;

  isTokenExpired = (): boolean =>
    this.jwtToken.isTokenExpired(this.storage.get(Key.TOKEN));

  handleError = (httpErrorResponse: HttpErrorResponse): Observable<never> => {
    console.log(httpErrorResponse);
    let error: string = `Une erreur s'est produite. Veuillez réessayer.`;
    if (httpErrorResponse.error instanceof ErrorEvent) {
      error = `Une erreur client s'est produite. - ${httpErrorResponse.error.message}`;
      return throwError(() => error);
    }
    if (httpErrorResponse.error.message) {
      error = `${httpErrorResponse.error.message}`;
      return throwError(() => error);
    }
    if (httpErrorResponse.error.error) {
      error = `Veuillez vous reconnecter`;
      return throwError(() => httpErrorResponse.error.error);
    }
    return throwError(() => error);
  };
}
