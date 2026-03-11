import { Inject, Injectable, InjectionToken } from '@angular/core';
import {Key} from '../enum/cache.key';

const CLIENT_STORAGE = new InjectionToken<Storage>('CLIENT_STORAGE', {factory: () => localStorage});

@Injectable()
export class StorageService {

  constructor(@Inject(CLIENT_STORAGE) private storage: Storage) { }

  get = (key: string) => this.storage.getItem(key);

  set = (key: string, value: any) => this.storage.setItem(key, value === 'string' ? value : JSON.stringify(value));
  
  remove = (key: string) => this.storage.removeItem(key);

  getRedirectUrl = () => this.storage.getItem(Key.REDIRECT_URL);

  setRedirectUrl = (redirectUrl: string) => this.storage.setItem(Key.REDIRECT_URL, redirectUrl);

  removeRedirectUrl = () => this.storage.removeItem(Key.REDIRECT_URL);
}
