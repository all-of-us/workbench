import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {Router} from '@angular/router';

import {SignInService} from '../../services/sign-in.service';
import {AccountCreationModalsComponent} from '../account-creation-modals/component';
import {AccountCreationComponent} from '../account-creation/component';

import {LoginComponent} from '../login/component';

@Component({
  selector : 'app-account-creation-success',
  styleUrls: ['../../styles/template.css',
              './component.css'],
  templateUrl: './component.html'
})
export class AccountCreationSuccessComponent {
  username: string;
  @Input('contactEmail')
  contactEmail: string;
  creationNonce: string;
  updateModal: boolean;
  resendModal: boolean;

  @ViewChild(AccountCreationModalsComponent)
  accountCreationModalsComponent: AccountCreationModalsComponent;

  constructor(
    private loginComponent: LoginComponent,
    private account: AccountCreationComponent,
    private router: Router,
    private signInService: SignInService
  ) {
    // This is a workaround for ExpressionChangedAfterItHasBeenCheckedError from angular
    setTimeout(() => {
      loginComponent.smallerBackgroundImgSrc = '/assets/images/congrats-female-standing.png';
      loginComponent.backgroundImgSrc = '/assets/images/congrats-female.png';
    }, 0);
    this.username = account.profile.username;
    this.creationNonce = account.profile.creationNonce;
    // create bound methods to use as callbacks
    this.closeModals = this.closeModals.bind(this);
    this.updateEmail = this.updateEmail.bind(this);
  }

  signIn(): void {
    this.signInService.signIn();
  }

  updateEmail(newEmail: string): void {
    this.contactEmail = newEmail;
  }

  public closeModals(): void {
    this.resendModal = false;
    this.updateModal = false;
  }

  public getEmail(contactemail: string) {
    this.contactEmail = contactemail;
  }
}
