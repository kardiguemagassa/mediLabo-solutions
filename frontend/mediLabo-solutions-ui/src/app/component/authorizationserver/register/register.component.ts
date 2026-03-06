import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { StorageService } from '../../../service/storage.service';
import { UserService } from '../../../service/user.service';


@Component({
  selector: 'app-register',
  imports: [RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  state = signal<{loading: boolean, message: string, error: string | any}>({ loading: false, message: undefined, error: undefined});
  private router = inject(Router);
  private storage = inject(StorageService);
  private userService = inject(UserService);

  ngOnInit() : void {
    if (this.userService.isAuthenticated() && this.userService.isTokenExpired()) {
      this.storage.getRedirectUrl() ? this.router.navigate([this.storage.getRedirectUrl()]) : this.router.navigate(['/dashboard']);
    }
  }
}
