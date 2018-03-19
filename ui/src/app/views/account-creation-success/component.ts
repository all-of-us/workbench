import {Component} from '@angular/core';

import {SignInService} from '../../services/sign-in.service';
import {AccountCreationComponent} from '../account-creation/component';
import {SignedOutAppComponent} from '../signed-out-app/component';

@Component({
  selector : 'app-account-creation-success',
  styleUrls: ['../../styles/template.css'],
  templateUrl: './component.html'
})
export class AccountCreationSuccessComponent {
  email: string;

  constructor(
      private signedOutAppComponent: SignedOutAppComponent,
      private account: AccountCreationComponent,
      private signInService: SignInService) {
    // This is a workaround for ExpressionChangedAfterItHasBeenCheckedError from angular
    setTimeout(() => {
      signedOutAppComponent.smallerBackgroundImgSrc = '/assets/images/congrats-female-standing.png';
      signedOutAppComponent.backgroundImgSrc = '/assets/images/congrats-female.png';
    }, 0);
    this.email = account.profile.username;
  }

  signIn(): void {
    this.signInService.signIn();
  }
}
