import { Observable, of, tap } from "rxjs";
import { HttpEvent, HttpHandlerFn, HttpInterceptorFn, HttpRequest, HttpResponse } from "@angular/common/http";
import { inject } from "@angular/core";
import { CacheService } from "../service/cache.service";

export const cacheInterceptor = (request: HttpRequest<unknown>, next: HttpHandlerFn, cache = inject(CacheService)): Observable<HttpEvent<unknown>> => {
    if (request.url.includes('resetpassword')) {
        return next(request);
    }
    if (request.url.includes('register')) {
        return next(request);
    }
    if (request.url.includes('login')) {
        return next(request);
    }
    if (request.url.includes('refresh')) {
        return next(request);
    }
    if (request.url.includes('messages')) {
        return next(request);
    }
    if (request.url.includes('download')) {
        return next(request);
    }
    if (request.method === 'POST' || request.method === 'PUT' || request.method === 'PATCH' || request.method === 'DELETE' || request.method === 'HEAD') {
        cache.evictAll();
        return next(request);
    }
    const cachedResponse = cache.get(request.url);
    if (cachedResponse) {
        console.log('Found response in cache', cachedResponse);
        return of(cachedResponse);
    }
    return handleRequest(request, next, cache);
};

const handleRequest = (request: HttpRequest<unknown>, next: HttpHandlerFn, cache: CacheService) => {
    return next(request).pipe(
        tap(response => {
            if (response instanceof HttpResponse && request.method === 'GET') {
                console.log('Caching get request resopnse', response);
                cache.put(request.url, response);
            }
        }))
};