import {Component} from '@angular/core';

import {ServerConfigService} from '../../services/server-config.service';
import {InvitationKeyComponent} from '../invitation-key/component';
import {LoginComponent} from '../login/component';

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
  contactEmailConflictError = false;
  containsLowerAndUpperError: boolean;
  profile: Profile = {
    username: '',
    enabledInFireCloud: false,
    dataAccessLevel: DataAccessLevel.Unregistered,
    givenName: '',
    familyName: '',
    contactEmail: ''
  };
  password: string;
  passwordAgain: string;
  showAllFieldsRequiredError: boolean;
  showPasswordsDoNotMatchError: boolean;
  showPasswordLengthError: boolean;
  creatingAccount: boolean;
  accountCreated: boolean;
  usernameConflictError = false;
  gsuiteDomain: string;
  usernameCheckTimeout: NodeJS.Timer;
  contactEmailCheckTimeout: NodeJS.Timer;

  // TODO: Injecting the parent component is a bad separation of concerns, as
  // well as injecting LoginComponent. Should look at refactoring these
  // interactions.
  constructor(
    private profileService: ProfileService,
    private invitationKeyService: InvitationKeyComponent,
    private loginComponent: LoginComponent,
    serverConfigService: ServerConfigService
  ) {
    serverConfigService.getConfig().subscribe((config) => {
      this.gsuiteDomain = config.gsuiteDomain;
    });
    // This is a workaround for ExpressionChangedAfterItHasBeenCheckedError from angular
    setTimeout(() => {
      this.loginComponent.smallerBackgroundImgSrc =
          '/assets/images/create-account-male-standing.png';
      this.loginComponent.backgroundImgSrc = '/assets/images/create-account-male.png';
    }, 0);
  }

  createAccount(): void {
    if (this.usernameConflictError || this.contactEmailConflictError) {
      return;
    }
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
    this.creatingAccount = true;
    this.profileService.createAccount(request).subscribe(() => {
      this.creatingAccount = false;
      this.accountCreated = true;
    }, () => {
      this.creatingAccount = false;
    });
  }

  usernameChanged(): void {
    this.usernameConflictError = false;
    // TODO: This should use a debounce, rather than manual setTimeout()s.
    clearTimeout(this.usernameCheckTimeout);
    this.usernameCheckTimeout = setTimeout(() => {
      if (!this.profile.username.trim()) {
        return;
      }
      this.profileService.isUsernameTaken(this.profile.username).subscribe((response) => {
        this.usernameConflictError = response.isTaken;
      });
    }, 300);
  }

  contactEmailChanged(): void {
    if (!this.profile.contactEmail) {
      return;
    }
    this.contactEmailConflictError = false;
    clearTimeout(this.contactEmailCheckTimeout);
    this.contactEmailCheckTimeout = setTimeout(() => {
      this.profileService.isContactEmailTaken(this.profile.contactEmail).subscribe((response) => {
        this.contactEmailConflictError = response.isTaken;
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
