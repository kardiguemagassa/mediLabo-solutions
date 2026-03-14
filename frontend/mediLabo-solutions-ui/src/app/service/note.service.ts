import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, Observable, tap, throwError } from 'rxjs';
import { server } from '../utils/fileutils';
import { IResponse } from '../interface/response';

@Injectable()
export class NoteService {

  private http = inject(HttpClient);

  constructor() {}

  createNote$ = (note: any) =>
    <Observable<IResponse>>(
      this.http
        .post<IResponse>(`${server}/api/notes`, note)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  note$ = (noteUuid: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/notes/${noteUuid}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  notesByPatient$ = (patientUuid: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/notes/patient/${patientUuid}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  notesByPractitioner$ = (practitionerUuid: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/notes/practitioner/${practitionerUuid}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  myNotes$ = () =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/notes/my-notes`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  updateNote$ = (noteUuid: string, note: any) =>
    <Observable<IResponse>>(
      this.http
        .put<IResponse>(`${server}/api/notes/${noteUuid}`, note)
        .pipe(tap(console.log), catchError(this.handleError))
    );


  deleteNote$ = (noteUuid: string) =>
    <Observable<IResponse>>(
      this.http
        .delete<IResponse>(`${server}/api/notes/${noteUuid}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );


  noteCount$ = (patientUuid: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/notes/count/patient/${patientUuid}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  uploadFile$ = (noteUuid: string, file: FormData) =>
    <Observable<IResponse>>(
      this.http
        .post<IResponse>(`${server}/api/notes/${noteUuid}/files`, file)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  files$ = (noteUuid: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/notes/${noteUuid}/files`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  downloadFile$ = (noteUuid: string, fileUuid: string) =>
    this.http
      .get(`${server}/api/notes/${noteUuid}/files/${fileUuid}/download`, {
        responseType: 'blob',
        observe: 'response'
      })
      .pipe(tap(console.log), catchError(this.handleError));

  deleteFile$ = (noteUuid: string, fileUuid: string) =>
    <Observable<IResponse>>(
      this.http
        .delete<IResponse>(`${server}/api/notes/${noteUuid}/files/${fileUuid}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  addComment$ = (noteUuid: string, comment: any) =>
    <Observable<IResponse>>(
      this.http
        .post<IResponse>(`${server}/api/notes/${noteUuid}/comments`, comment)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  comments$ = (noteUuid: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/notes/${noteUuid}/comments`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  updateComment$ = (noteUuid: string, commentUuid: string, comment: any) =>
    <Observable<IResponse>>(
      this.http
        .put<IResponse>(`${server}/api/notes/${noteUuid}/comments/${commentUuid}`, comment)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  deleteComment$ = (noteUuid: string, commentUuid: string) =>
    <Observable<IResponse>>(
      this.http
        .delete<IResponse>(`${server}/api/notes/${noteUuid}/comments/${commentUuid}`)
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