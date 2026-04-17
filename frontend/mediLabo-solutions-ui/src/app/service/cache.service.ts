import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';

@Injectable()
export class CacheService {
    private httpResponse: { [key: string]: HttpResponse<any> } = {};

    put = (key: string, httpResponse: HttpResponse<any>) => {
        console.log('caching response', httpResponse);
        this.httpResponse[key] = httpResponse;
    };

    get = (key: string) => this.httpResponse[key];

    evict = (key: string) => delete this.httpResponse[key];

    evictAll = () => {
        console.log('clearing the cache');
        this.httpResponse = {};
    };
}