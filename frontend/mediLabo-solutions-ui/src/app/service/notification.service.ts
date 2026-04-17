import { inject, Injectable } from '@angular/core';
import {HttpClient,HttpErrorResponse,HttpHeaders,} from '@angular/common/http';
import { catchError, Observable, tap, throwError } from 'rxjs';
import { server } from '../utils/fileutils';
import { IResponse } from '../interface/response';

@Injectable()
export class NotificationService {
  private http = inject(HttpClient);

  constructor() {}

  messages$ = () =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/notifications/messages`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  sendMessages$ = (message: any) =>
    <Observable<IResponse>>(
      this.http
        .post<IResponse>(`${server}/api/notifications/reply`, message)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  replyToMessage$ = (message: any) =>
    <Observable<IResponse>>(
      this.http
        .post<IResponse>(`${server}/api/notifications/reply`, message)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  getConversation$ = (conversationId: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/notifications/messages/${conversationId}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  sendMessage$ = (message: any) =>
    <Observable<IResponse>>(
      this.http
        .post<IResponse>(`${server}/api/notifications/messages`, message)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  conversation$ = (conversationId: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/notifications/messages/${conversationId}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

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
