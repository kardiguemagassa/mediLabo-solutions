import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, ParamMap, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UserService } from '../../../service/user.service';
import { HotToastService } from '@ngxpert/hot-toast';

import { delay, EMPTY, Observer, switchMap } from 'rxjs';
import { IResponse } from '../../../interface/response';

@Component({
  selector: 'app-verifypassword',
  imports: [RouterLink, FormsModule],
  templateUrl: './verifypassword.component.html',
  styleUrl: './verifypassword.component.scss'
})
export class VerifypasswordComponent {

  state = signal<{ success?: boolean, token?: string, mode: 'verify' | 'reset', userUuid?: string, loading: boolean, message: string, error: string | any }>({ mode: 'verify', success: false, loading: false, message: undefined, error: undefined })
  private destroyRef = inject(DestroyRef);
  private userService = inject(UserService);
  private toastService = inject(HotToastService);
  private activatedRoute = inject(ActivatedRoute);


  ngOnInit(): void {
    this.activatedRoute.queryParamMap.pipe(
      switchMap((params: ParamMap) => {
        const token = params.get('token');
        if (token) {
          this.state.set({ token, mode: 'verify', loading: true, message: undefined, error: undefined, success: false });
          return this.userService.verifyPasswordToken$(token);
        } else {
          this.state.set({ mode: 'verify', success: false, loading: false, message: undefined, error: 'Lien invalide. Veuillez réessayer.' });
          return EMPTY;
        }
      }),
      //delay(5 * 1000),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(this.verifySubscriber);
  }

  closeMessage = () => this.state.update(state => ({ ...state, message: undefined, error: undefined }));

  createNewPassword = (form: NgForm) => {
      this.userService.createNewPassword$(form.value).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: response => {
          this.state.update(state => ({ ...state, success: true, loading: false, message: response.message, error: undefined }));
          this.toastService.success(response.message);
        },
        error: (error: string) => {
          this.state.update(state => ({ ...state, success: false, loading: false, message: undefined, error }));
          this.toastService.error(error);
        },
        complete: () => {}
      });
    };

  private verifySubscriber: Observer<any> = {
    next: (response: IResponse) => {
      this.state.update(state => ({ ...state, loading: false, mode: 'reset', userUuid: response.data.user.userUuid, message: `${response.message} pour ${response.data.user.email}`, error: undefined }));
      this.toastService.success('Lien vérifié avec succès. Vous pouvez maintenant créer un nouveau mot de passe.');
    },
    error: (error: string) => {
      this.state.set({ mode: 'verify', loading: false, message: undefined, error });
      this.toastService.error(error);
    },
    complete: () => { }
  };

}
