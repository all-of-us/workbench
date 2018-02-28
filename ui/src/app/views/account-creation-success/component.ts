import {Component} from '@angular/core';

import {SignInService} from '../../services/sign-in.service';
import {AccountCreationComponent} from '../account-creation/component';
import {AppComponent} from '../app/component';

@Component({
  selector : 'app-account-creation-success',
  styleUrls: ['../../styles/template.css'],
  templateUrl: './component.html'
})
export class AccountCreationSuccessComponent {
  email: string;

  constructor(
      private app: AppComponent,
      private account: AccountCreationComponent,
      private signInService: SignInService) {
    app.backgroundImgSrc = '/assets/images/congrats-female@2x.jpg';
    this.email = account.profile.username;
  }

  signIn(): void {
    this.signInService.signIn();
  }
}
