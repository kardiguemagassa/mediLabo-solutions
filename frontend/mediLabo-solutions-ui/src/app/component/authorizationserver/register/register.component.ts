import { Component, DestroyRef, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { StorageService } from '../../../service/storage.service';
import { UserService } from '../../../service/user.service';
import { FormsModule, NgForm } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HotToastService } from '@ngxpert/hot-toast';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-register',
  imports: [RouterLink, FormsModule, CommonModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  state = signal<{loading: boolean, message: string, error: string | any}>({ loading: false, message: undefined, error: undefined});
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private storage = inject(StorageService);
  private userService = inject(UserService);
  private toastService = inject(HotToastService);

  ngOnInit() : void {
    if (this.userService.isAuthenticated() && this.userService.isTokenExpired()) {
      this.storage.getRedirectUrl() ? this.router.navigate([this.storage.getRedirectUrl()]) : this.router.navigate(['/dashboard']);
    }
  }

  closeMessage = () => this.state.set({loading: false, message: undefined, error: undefined});

  register = (form: NgForm) => {
    this.state.set({loading: true, message: undefined, error: undefined});
    this.userService.register$(form.value).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (response) => {
        this.state.set({loading: false, message: response.message, error: undefined});
        this.toastService.success(response.message);
      },
      error: (error: string) => {
        this.state.set({loading: false, message: undefined, error});
        this.toastService.error(error);
      },
      complete: () => form.reset()
    });
  }
}
