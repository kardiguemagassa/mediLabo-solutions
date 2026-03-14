import { BehaviorSubject, catchError, Observable, switchMap, throwError } from "rxjs";
import { IResponse } from "../interface/response";
import { HttpErrorResponse, HttpEvent, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from "@angular/common/http";
import { inject } from "@angular/core";
import { UserService } from "../service/user.service";
import { StorageService } from "../service/storage.service";
import { Key } from "../enum/cache.key";
import { getFormData } from "../utils/fileutils";

let isTokenRefreshing: boolean = false;
let refreshTokenSubject: BehaviorSubject<IResponse> = new BehaviorSubject(null);

export const tokenInterceptor: HttpInterceptorFn = (request: HttpRequest<unknown>, next: HttpHandlerFn, userService = inject(UserService), storage = inject(StorageService)): Observable<HttpEvent<unknown>> => {
    if (request.url.includes('verify') || request.url.includes('home') || request.url.includes('resetpassword') || request.url.includes('register') || request.url.includes('login') || request.url.includes('oauth2')) {
        return next(request);
    }
    return next(addAuthorizationTokenHeader(request, storage.get(Key.TOKEN)))
        .pipe(
            catchError(error => {
                if (error instanceof HttpErrorResponse && error.error?.code === 401 && error.error?.message === 'Votre session a expiré. Veuillez vous reconnecter.') {
                    console.log('REFRESHING TOKEN');
                    return handleRefreshToken(request, next, userService, storage);
                } else {
                    return throwError(() => error);
                }
            })
        );

};

const addAuthorizationTokenHeader = (request: HttpRequest<unknown>, token: string): HttpRequest<unknown> => {
    return request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
};

const handleRefreshToken = (request: HttpRequest<unknown>, next: HttpHandlerFn, userService: UserService, storage: StorageService): Observable<HttpEvent<unknown>> => {
    if (!isTokenRefreshing) {
        console.log('Refreshing token');
        isTokenRefreshing = true;
        refreshTokenSubject.next(null);
        return userService.refreshToken$(formData(storage.get(Key.REFRESH_TOKEN))).pipe(
            switchMap((response: any) => {
                console.log('Token refresh response', response);
                isTokenRefreshing = false;
                refreshTokenSubject.next(response);
                console.log('Sending original request', request);
                return next(addAuthorizationTokenHeader(request, response.access_token));
            }),
            catchError(error => {
                userService.logOut();
                return throwError(() => error);
            })
        )

    } else {
        return refreshTokenSubject.pipe(
            switchMap((response: any) => {
                console.log('Already refreshed');
                return next(addAuthorizationTokenHeader(request, response.access_token));
            })
        );
    }
};

const formData = (refresh_token: string) => getFormData({ refresh_token, client_id: 'client', code_verifier: 'IXC0xF1i9LDClkUlrD58mkzI4lVw_uylG21z43xVct3Ro2GCJKV5iGJnN97CNbpmAoOCK94tc4MfvJ24q5ucCiKty3dBFMLbwqPE-vqOJ-s1axq86F0gev0j-Zv4cOSq', grant_type: 'refresh_token', redirect_url: 'http://localhost:4200' }, null);