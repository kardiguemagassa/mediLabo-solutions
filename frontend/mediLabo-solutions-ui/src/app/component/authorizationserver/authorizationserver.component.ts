import { Component } from '@angular/core';
import { RouterOutlet, RouterLinkActive, RouterLink } from '@angular/router';

@Component({
  selector: 'app-authorizationserver',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './authorizationserver.component.html',
  styleUrl: './authorizationserver.component.scss'
})
export class AuthorizationserverComponent {

}
