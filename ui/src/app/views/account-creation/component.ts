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
  styleUrls: ['../../styles/template.css',
              './component.css']
})
export class AccountCreationComponent {
  contactEmailConflictError = false;
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
  creatingAccount: boolean;
  accountCreated: boolean;
  usernameConflictError = false;
  gsuiteDomain: string;
  usernameOffFocus: boolean;
  passwordOffFocus: boolean;
  passwordAgainOffFocus: boolean;
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
    if (this.usernameConflictError || this.contactEmailConflictError
        || this.usernameInvalidError) {
      return;
    }
    this.showAllFieldsRequiredError = false;
    const requiredFields =
        [this.profile.givenName, this.profile.familyName,
         this.profile.username, this.profile.contactEmail, this.password, this.passwordAgain];
    if (requiredFields.some(isBlank)) {
      this.showAllFieldsRequiredError = true;
      return;
    } else if (this.showPasswordsDoNotMatchError
      || this.showPasswordLengthError
      || this.containsLowerAndUpperError) {
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

  get showPasswordsDoNotMatchError() {
    // We do not want to show errors if nothing is typed yet. This is caught by the required
    // fields case.
    if (this.password === undefined || this.passwordAgain === undefined
      || this.password.length === 0 || this.passwordAgain.length === 0) {
        return false;
    }
    return !(this.password === this.passwordAgain);
  }

  get showPasswordLengthError() {
    // We do not want to show errors if nothing is typed yet. This is caught by the required
    // fields case.
    if (this.password === undefined || this.password.length === 0) {
      return false;
    }
    return (this.password.length < 8 || this.password.length > 100);
  }

  get containsLowerAndUpperError() {
    // We do not want to show errors if nothing is typed yet. This is caught by the required
    // fields case.
    if (this.password === undefined || this.password.length === 0) {
      return false;
    }
    return !(this.hasLowerCase(this.password) && this.hasUpperCase(this.password));
  }

  get usernameInvalidError(): boolean {
    const username = this.profile.username;
    if (isBlank(username)) {
      return false;
    }
    // Include alphanumeric characters, -'s, _'s, apostrophes, and single .'s in a row.
    return !(new RegExp(/^[\w-']([.]{0,1}[\w-']+)*$/).test(username));
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

  leaveFocusUsername(): void {
    this.usernameOffFocus = true;
  }

  enterFocusUsername(): void {
    this.usernameOffFocus = false;
  }

  get usernameValidationError(): boolean {
    return this.usernameOffFocus && (this.usernameConflictError || this.usernameInvalidError);
  }

  get usernameValidationSuccess(): boolean {
    return this.usernameOffFocus
      && this.profile.username.trim().length >= 1
      && !this.usernameConflictError
      && !this.usernameInvalidError;
  }

  leaveFocusPassword(): void {
    this.passwordOffFocus = true;
  }

  enterFocusPassword(): void {
    this.passwordOffFocus = false;
  }

  get passwordValidationError(): boolean {
    return this.passwordOffFocus &&
      (this.showPasswordLengthError ||
      this.containsLowerAndUpperError);
  }

  get passwordValidationSuccess(): boolean {
    return this.passwordOffFocus &&
      !this.showPasswordLengthError &&
      !this.containsLowerAndUpperError;
  }

  leaveFocusPasswordAgain(): void {
    this.passwordAgainOffFocus = true;
  }

  enterFocusPasswordAgain(): void {
    this.passwordAgainOffFocus = false;
  }

  get passwordAgainValidationError(): boolean {
    return this.passwordAgainOffFocus && this.showPasswordsDoNotMatchError;
  }

  get passwordAgainValidationSuccess(): boolean {
    return this.passwordAgainOffFocus && !this.showPasswordsDoNotMatchError;
  }

}
