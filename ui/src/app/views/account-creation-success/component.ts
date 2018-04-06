import {Component} from '@angular/core';
import {Router} from '@angular/router';

import {ServerConfigService} from '../../services/server-config.service';
import {SignInService} from '../../services/sign-in.service';
import {AccountCreationComponent} from '../account-creation/component';
import {LoginComponent} from '../login/component';

@Component({
  selector : 'app-account-creation-success',
  styleUrls: ['../../styles/template.css'],
  templateUrl: './component.html'
})
export class AccountCreationSuccessComponent {
  email: string;
  gsuiteDomain: string;

  constructor(
    private loginComponent: LoginComponent,
    private account: AccountCreationComponent,
    private router: Router,
    private signInService: SignInService,
    serverConfigService: ServerConfigService
  ) {
    serverConfigService.getConfig().subscribe((config) => {
      this.gsuiteDomain = config.gsuiteDomain;
    });
    // This is a workaround for ExpressionChangedAfterItHasBeenCheckedError from angular
    setTimeout(() => {
      loginComponent.smallerBackgroundImgSrc = '/assets/images/congrats-female-standing.png';
      loginComponent.backgroundImgSrc = '/assets/images/congrats-female.png';
    }, 0);
    this.email = account.profile.username;
  }

  signIn(): void {
    this.signInService.signIn();
  }
}
