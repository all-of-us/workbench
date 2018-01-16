import {Component} from '@angular/core';
import {SignInService} from 'app/services/sign-in.service';
import {DataAccessLevel} from 'generated';
import {Profile} from 'generated';
import {ProfileService} from 'generated';
import {CreateAccountRequest} from 'generated';
import {InvitationKeyComponent} from '../invitation-key/component';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

@Component({
  selector: 'app-account-creation',
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class AccountCreationComponent {
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
  contactEmail: string;
  showAllFieldsRequiredError: boolean;
  showPasswordsDoNotMatchError: boolean;
  creatingAcccount: boolean;
  accountCreated: boolean;
  conflictError = false;
  showPasswordLengthError = false;
  usernameCheckTimeout: NodeJS.Timer;

  constructor(
    private profileService: ProfileService,
    private signInService: SignInService,
    private invitationKeyService: InvitationKeyComponent
  ) {}

  createAccount(): void {
    this.showAllFieldsRequiredError = false;
    this.showPasswordsDoNotMatchError = false;
    const requiredFields =
        [this.profile.givenName, this.profile.familyName,
          this.profile.username, this.password, this.passwordAgain];
    if (requiredFields.some(isBlank)) {
      this.showAllFieldsRequiredError = true;
    }
    if (!(this.password === this.passwordAgain)) {
      this.showPasswordsDoNotMatchError = true;
    }
    // check password length (see https://developers.google.com/admin-sdk/directory/v1/reference/users#password)
    else if (this.password.length < 8 || this.password.length > 100) {
      this.showPasswordLengthError = true;
    }
    if (this.showAllFieldsRequiredError || this.showPasswordsDoNotMatchError || this.showPasswordLengthError) {
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

  signIn(): void {
    this.signInService.signIn();
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

}
