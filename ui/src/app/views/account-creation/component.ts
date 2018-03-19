import {Component} from '@angular/core';

import {InvitationKeyComponent} from '../invitation-key/component';
import {SignedOutAppComponent} from '../signed-out-app/component';

import {DataAccessLevel} from 'generated';
import {Profile} from 'generated';
import {ProfileService} from 'generated';
import {CreateAccountRequest} from 'generated';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

@Component({
  selector: 'app-account-creation',
  templateUrl: './component.html',
  styleUrls: ['./component.css',
              '../../styles/template.css']
})
export class AccountCreationComponent {
  containsLowerAndUpperError: boolean;
  profile: Profile = {
    username: '',
    enabledInFireCloud: false,
    dataAccessLevel: DataAccessLevel.Unregistered,
    givenName: '',
    familyName: '',
    contactEmail: ''
  };
  givenName: string;
  familyName: string;
  username: string;
  password: string;
  passwordAgain: string;
  showAllFieldsRequiredError: boolean;
  showPasswordsDoNotMatchError: boolean;
  showPasswordLengthError: boolean;
  creatingAcccount: boolean;
  accountCreated: boolean;
  conflictError = false;
  usernameCheckTimeout: NodeJS.Timer;

  constructor(
    private profileService: ProfileService,
    private invitationKeyService: InvitationKeyComponent,
    private signedOutAppComponent: SignedOutAppComponent
  ) {
    // This is a workaround for ExpressionChangedAfterItHasBeenCheckedError from angular
    setTimeout(() => {
      this.signedOutAppComponent.smallerBackgroundImgSrc =
          '/assets/images/create-account-male-standing.png';
      this.signedOutAppComponent.backgroundImgSrc = '/assets/images/create-account-male.png';
    }, 0);
  }

  createAccount(): void {
    this.containsLowerAndUpperError = false;
    this.showAllFieldsRequiredError = false;
    this.showPasswordsDoNotMatchError = false;
    this.showPasswordLengthError = false;
    const requiredFields =
        [this.profile.givenName, this.profile.familyName,
         this.profile.username, this.profile.contactEmail, this.password, this.passwordAgain];
    if (requiredFields.some(isBlank)) {
      this.showAllFieldsRequiredError = true;
      return;
    } else if (!(this.password === this.passwordAgain)) {
      this.showPasswordsDoNotMatchError = true;
      return;
    } else if (this.password.length < 8 || this.password.length > 100) {
      this.showPasswordLengthError = true;
      return;
    } else if (!(this.hasLowerCase(this.password) && this.hasUpperCase(this.password))) {
      this.containsLowerAndUpperError = true;
      return;
    }

    const request: CreateAccountRequest = {
      profile: this.profile, password: this.password,
      invitationKey: this.invitationKeyService.invitationKey
    };
    this.creatingAcccount = true;
    this.profileService.createAccount(request).subscribe(() => {
      this.creatingAcccount = false;
      this.accountCreated = true;
    }, () => {
      this.creatingAcccount = false;
    });
  }

  usernameChanged(): void {
    this.conflictError = false;
    clearTimeout(this.usernameCheckTimeout);
    this.usernameCheckTimeout = setTimeout(() => {
      this.profileService.isUsernameTaken(this.profile.username).subscribe((response) => {
        this.conflictError = response.isTaken;
      });
    }, 300);
  }

  hasLowerCase(str: string): boolean {
    return (/[a-z]/.test(str));
  }

  hasUpperCase(str: string): boolean {
    return (/[A-Z]/.test(str));
  }

}
