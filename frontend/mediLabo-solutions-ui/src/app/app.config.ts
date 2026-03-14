import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { InMemoryScrollingFeature, provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { StorageService } from './service/storage.service';
import { UserService } from './service/user.service';
import { provideHotToastConfig } from '@ngxpert/hot-toast';
import { provideDialogConfig } from '@ngneat/dialog';
import { NotificationService } from './service/notification.service';
import { AssessmentService } from './service/assessment.service';
import { PatientService } from './service/patient.service';
import { NoteService } from './service/note.service';
import { tokenInterceptor } from './interceptor/token.interceptor';
import { cacheInterceptor } from './interceptor/cache.interceptor';
import { ModalService } from './service/modal.service';
import { CacheService } from './service/cache.service';

const dialogConfig = provideDialogConfig({
  closeButton: false,
  resizable: true,
});

const inMemoryScrollingFeature: InMemoryScrollingFeature = withInMemoryScrolling({
  scrollPositionRestoration: 'enabled',
  anchorScrolling: 'enabled'
});

export const appConfig: ApplicationConfig = {
  providers: [provideZoneChangeDetection({ eventCoalescing: true }),provideRouter(routes,inMemoryScrollingFeature, withComponentInputBinding()),
    dialogConfig,
    UserService,
    StorageService,
    NotificationService,
    AssessmentService,
    NoteService,
    PatientService,
    ModalService,
    CacheService,
    provideHttpClient(withInterceptors([tokenInterceptor, cacheInterceptor])),
    provideHotToastConfig({ position: 'top-right' }),
  ],
};
