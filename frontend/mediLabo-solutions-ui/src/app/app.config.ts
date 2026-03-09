import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { StorageService } from './service/storage.service';
import { UserService } from './service/user.service';
import { provideHotToastConfig } from '@ngxpert/hot-toast';


export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    UserService, StorageService,
    provideHttpClient(),
    provideHotToastConfig({ position: 'top-right' })
  ]
};
