import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { StorageService } from '../../../service/storage.service';
import { UserService } from '../../../service/user.service';
import { HotToastService } from '@ngxpert/hot-toast';
import { CommonModule } from '@angular/common';
import { EMPTY, Observer, switchMap } from 'rxjs';

import { Key } from '../../../enum/cache.key';
import { getFormData } from '../../../utils/fileutils';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { IAuthentication } from '../../../interface/IAuthentication';

@Component({
  selector: 'app-home',
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {

  // https://www.npmjs.com/package/angular-oauth2-oidc/v/8.0.2
  loading = signal<boolean>(true);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private storage = inject(StorageService);
  private userService = inject(UserService);
  private toastService = inject(HotToastService);
  private activatedRoute = inject(ActivatedRoute);

  ngOnInit(): void {
    if (this.userService.isAuthenticated() && !this.userService.isTokenExpired()) {
      this.storage.getRedirectUrl() ? this.router.navigate([this.storage.getRedirectUrl()]) : this.router.navigate(['/dashboard']);
      return;
    } else {
      this.activatedRoute.queryParamMap.pipe(
        switchMap((params: ParamMap) => {
          const code = params.get('code');
          if (code) {
            this.loading.set(true);
            return this.userService.validateCode$(this.formData(code));
          } else {
            this.loading.set(false);
            return EMPTY;
          }
        }),
        //delay(5 * 1000),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(this.verifyCode);
    }
  }

  private verifyCode: Observer<any> = {
    next: (response: IAuthentication) => {
      this.saveToken(response);
      this.storage.getRedirectUrl() ? this.router.navigate([this.storage.getRedirectUrl()]) : this.router.navigate(['/dashboard']);
    },
    error: (error: string) => {
      this.loading.set(false);
      this.toastService.error(error);
    },
    complete: () => {}
  };

  private formData = (code: string) => getFormData({ code, client_id: 'client', grant_type: 'authorization_code', redirect_uri: 'http://localhost:4200', code_verifier: 'IXC0xF1i9LDClkUlrD58mkzI4lVw_uylG21z43xVct3Ro2GCJKV5iGJnN97CNbpmAoOCK94tc4MfvJ24q5ucCiKty3dBFMLbwqPE-vqOJ-s1axq86F0gev0j-Zv4cOSq' }, null);

  private saveToken = (response: IAuthentication) => {
    this.storage.set(Key.TOKEN, response.access_token);
    this.storage.set(Key.REFRESH_TOKEN, response.refresh_token);
  };

}
