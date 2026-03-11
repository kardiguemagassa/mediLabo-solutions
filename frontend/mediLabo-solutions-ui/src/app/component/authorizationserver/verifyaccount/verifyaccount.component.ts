import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { UserService } from '../../../service/user.service';
import { HotToastService } from '@ngxpert/hot-toast';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Observer, switchMap } from 'rxjs';
import { IResponse } from '../../../interface/response';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-verifyaccount',
  imports: [CommonModule, RouterLink],
  templateUrl: './verifyaccount.component.html',
  styleUrl: './verifyaccount.component.scss'
})
export class VerifyaccountComponent {

  state = signal<{loading: boolean, message: string, error: string | any}>({ loading: false, message: undefined, error: undefined});
  private destroyRef = inject(DestroyRef);
  private userService = inject(UserService);
  private toastService = inject(HotToastService);
  private activatedRoute = inject(ActivatedRoute);

  ngOnInit(): void {
    this.activatedRoute.queryParamMap.pipe(
      switchMap((params: ParamMap) => {
        const token = params.get('token');
        if(token) {
          this.state.set({ loading: true, message: undefined, error: undefined });
          return this.userService.verifyAccountToken$(token);
        } else {
          this.state.set({ loading: false, message: undefined, error: 'Lien invalide. Veuillez réessayer.' });
          return EMPTY;
        }
      }),
      //delay(5 * 1000),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(this.verifyAccount);
  }

  closeMessage = () => this.state.set({ loading: false, message: undefined, error: undefined });

  private verifyAccount: Observer<any> = {
    next: (response: IResponse) => {
      this.state.set({ loading: false, message: response.message, error: undefined });
      this.toastService.success('Le compte a été vérifié. Vous pouvez maintenant vous connecter.');
    },
    error: (error: string) => {
      this.state.set({ loading: false, message: undefined, error });
      this.toastService.error(error);
    },
    complete: () => {}
  };

}

