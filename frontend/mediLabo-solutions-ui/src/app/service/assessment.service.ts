import { inject, Injectable } from '@angular/core';
import {HttpClient,HttpErrorResponse,HttpResponse,} from '@angular/common/http';
import { catchError, Observable, tap, throwError } from 'rxjs';
import { server } from '../utils/fileutils';
import { IResponse } from '../interface/response';
import { IQuery } from '../interface/query';

@Injectable()
export class AssessmentService {
  private http = inject(HttpClient);

  constructor() {}

  // Lecture du cache des assessments
  allAssessments$ = () =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/assessments`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  // Calcul pour un patient spécifique existant
  assessPatient$ = (patientUuid: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/assessments/diabetes/${patientUuid}`)
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
