import { inject, Injectable } from '@angular/core';
import { JwtHelperService } from '@auth0/angular-jwt';
import { StorageService } from './storage.service';
import { Key } from '../enum/cache.key';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, Observable, tap, throwError } from 'rxjs';
import { authorizationserver, server } from '../utils/fileutils';
import { IResponse } from '../interface/response';
import { IAuthentication } from '../interface/IAuthentication';
import { IUser } from '../interface/user';
import { UpdatePassword } from '../interface/credentials';

@Injectable()
export class UserService {
  private jwt = new JwtHelperService();
  private storage = inject(StorageService);
  private http = inject(HttpClient);

  constructor() { }

  register$ = (user: any) => <Observable<IResponse>>
    this.http.post<IResponse>
      (`${server}/api/users/register`, user)
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  profile$ = () => <Observable<IResponse>>
    this.http.get<IResponse>
      (`${server}/api/users/profile`)
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  update$ = (user: IUser) => <Observable<IResponse>>
    this.http.patch<IResponse>
      (`${server}/api/users/update`, user)
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  updatePassword$ = (form: UpdatePassword) => <Observable<IResponse>>
    this.http.patch<IResponse>
      (`${server}/api/users/updatepassword`, form)
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  verifyAccountToken$ = (token: string) => <Observable<IResponse>>
    this.http.get<IResponse>
      (`${server}/api/users/verify/account?token=${token}`)
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  verifyPasswordToken$ = (token: string) => <Observable<IResponse>>
    this.http.get<IResponse>
      (`${server}/api/users/verify/password?token=${token}`)
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  resetPassword$ = (form: FormData) => <Observable<IResponse>>
    this.http.post<IResponse>
      (`${server}/api/users/resetpassword`, form)
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  createNewPassword$ = (request: { userUuid: string, token: string, password: string, confirmPassword: string }) => <Observable<IResponse>>
    this.http.post<IResponse>
      (`${server}/api/users/resetpassword/reset`, request)
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  validateCode$ = (form: FormData) => <Observable<IAuthentication>>
    this.http.post<IAuthentication>
      (`${authorizationserver}/oauth2/token`, form)
      .pipe(
        tap((response: IAuthentication) => {
          console.log(response);
          this.storage.remove(Key.TOKEN);
          this.storage.remove(Key.REFRESH_TOKEN);
          this.storage.set(Key.TOKEN, response.access_token);
          this.storage.set(Key.REFRESH_TOKEN, response.refresh_token);
        }),
        catchError(this.handleError)
      );

  updateImage$ = (form: FormData) => <Observable<IResponse>>
    this.http.patch<IResponse>
      (`${server}/api/users/photo`, form)
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  toggleAccountLocked$ = () => <Observable<IResponse>>
    this.http.patch<IResponse>
      (`${server}/api/users/toggleaccountlocked`, {})
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  toggleAccountExpired$ = () => <Observable<IResponse>>
    this.http.patch<IResponse>
      (`${server}/api/users/toggleaccountexpired`, {})
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  toggleAccountEnabled$ = () => <Observable<IResponse>>
    this.http.patch<IResponse>
      (`${server}/api/users/toggleaccountenabled`, {})
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  enableMfa$ = () => <Observable<IResponse>>
    this.http.patch<IResponse>
      (`${server}/api/users/mfa/enable`, {})
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  disableMfa$ = () => <Observable<IResponse>>
    this.http.patch<IResponse>
      (`${server}/api/users/mfa/disable`, {})
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  updateRole$ = (role: string) => <Observable<IResponse>>
    this.http.patch<IResponse>
      (`${server}/api/users/updaterole`, { role })
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  users$ = () => <Observable<IResponse>>
    this.http.get<IResponse>
      (`${server}/api/users/list`)
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );

  user$ = (userUuid: string) => <Observable<IResponse>>
    this.http.get<IResponse>
      (`${server}/api/users/${userUuid}`)
      .pipe(
        tap(console.log),
        catchError(this.handleError)
      );
  
  /** Admin operations on other users */
  updateRoleByUuid$ = (userUuid: string, role: string) =>
    <Observable<IResponse>>this.http
        .patch<IResponse>(`${server}/api/users/${userUuid}/updaterole`, { role })
        .pipe(tap(console.log), catchError(this.handleError));

  toggleAccountLockedByUuid$ = (userUuid: string) =>
    <Observable<IResponse>>this.http
        .patch<IResponse>(`${server}/api/users/${userUuid}/toggleaccountlocked`, {})
        .pipe(tap(console.log), catchError(this.handleError));

  toggleAccountExpiredByUuid$ = (userUuid: string) =>
    <Observable<IResponse>>this.http
        .patch<IResponse>(`${server}/api/users/${userUuid}/toggleaccountexpired`, {})
        .pipe(tap(console.log), catchError(this.handleError));

  toggleAccountEnabledByUuid$ = (userUuid: string) =>
    <Observable<IResponse>>this.http
        .patch<IResponse>(`${server}/api/users/${userUuid}/toggleaccountenabled`, {})
        .pipe(tap(console.log), catchError(this.handleError));

  enableMfaByUuid$ = (userUuid: string) =>
    <Observable<IResponse>>this.http
        .patch<IResponse>(`${server}/api/users/${userUuid}/mfa/enable`, {})
        .pipe(tap(console.log), catchError(this.handleError));

  disableMfaByUuid$ = (userUuid: string) =>
    <Observable<IResponse>>this.http
        .patch<IResponse>(`${server}/api/users/${userUuid}/mfa/disable`, {})
        .pipe(tap(console.log), catchError(this.handleError));

  refreshToken$ = (form: FormData) => <Observable<IAuthentication>>
    this.http.post<IAuthentication>
      (`${authorizationserver}/oauth2/token`, form)
      .pipe(
        tap((response: IAuthentication) => {
          console.log(response);
          this.storage.remove(Key.TOKEN);
          this.storage.remove(Key.REFRESH_TOKEN);
          this.storage.set(Key.TOKEN, response.access_token);
          this.storage.set(Key.REFRESH_TOKEN, response.refresh_token);
        }),
        catchError(this.handleError)
      );

  logOut = () => {
    this.storage.remove(Key.TOKEN);
    this.storage.remove(Key.REFRESH_TOKEN);
  };

  isAuthenticated = (): boolean => this.jwt.decodeToken<string>(this.storage.get(Key.TOKEN)) ? true : false;

  isTokenExpired = (): boolean => this.jwt.isTokenExpired(this.storage.get(Key.TOKEN));

  handleError = (httpErrorResponse: HttpErrorResponse): Observable<never> => {
    console.log(httpErrorResponse);
    let error: string = 'An error occurred. Please try again.';
    if (httpErrorResponse.error instanceof ErrorEvent) {
      error = `A client error occurred - ${httpErrorResponse.error.message}`;
      return throwError(() => error);
    }
    if (httpErrorResponse.error.message) {
      error = `${httpErrorResponse.error.message}`;
      return throwError(() => error);
    }
    if (httpErrorResponse.error.error) {
      error = `Please login in again`;
      return throwError(() => httpErrorResponse.error.error);
    }
    return throwError(() => error);
  }

}
