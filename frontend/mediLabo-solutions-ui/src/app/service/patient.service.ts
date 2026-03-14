import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, Observable, tap, throwError } from 'rxjs';
import { server } from '../utils/fileutils';
import { IResponse } from '../interface/response';

@Injectable()
export class PatientService {

  private http = inject(HttpClient);

  constructor() {}

  createPatient$ = (patient: any) =>
    <Observable<IResponse>>(
      this.http
        .post<IResponse>(`${server}/api/patients`, patient)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  allPatients$ = () =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/patients`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  patient$ = (patientUuid: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/patients/${patientUuid}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  patientByUserUuid$ = (userUuid: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/patients/user/${userUuid}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  patientByEmail$ = (email: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/patients/email/${email}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  myPatientRecord$ = () =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/patients/me`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  patientByMedicalRecord$ = (medicalRecordNumber: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/patients/medical-record/${medicalRecordNumber}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  patientsByBloodType$ = (bloodType: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/patients/blood-type/${bloodType}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  updatePatient$ = (patientUuid: string, patient: any) =>
    <Observable<IResponse>>(
      this.http
        .put<IResponse>(`${server}/api/patients/${patientUuid}`, patient)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  deletePatient$ = (patientUuid: string) =>
    <Observable<IResponse>>(
      this.http
        .delete<IResponse>(`${server}/api/patients/${patientUuid}`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  restorePatient$ = (patientUuid: string) =>
    <Observable<IResponse>>(
      this.http
        .patch<IResponse>(`${server}/api/patients/${patientUuid}/restore`, {})
        .pipe(tap(console.log), catchError(this.handleError))
    );

  patientCount$ = () =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/patients/stats/count`)
        .pipe(tap(console.log), catchError(this.handleError))
    );

  patientExists$ = (userUuid: string) =>
    <Observable<IResponse>>(
      this.http
        .get<IResponse>(`${server}/api/patients/exists/user/${userUuid}`)
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