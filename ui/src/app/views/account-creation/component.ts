import {AfterViewInit, Component, ElementRef, ViewChild} from '@angular/core';


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
export class AccountCreationComponent implements AfterViewInit {
  profile: Profile = {
    // Note: We abuse the "username" field here by omitting "@domain.org". After
    // profile creation, this field is populated with the full email address.
    username: '',
    dataAccessLevel: DataAccessLevel.Unregistered,
    givenName: '',
    familyName: '',
    contactEmail: '',
    currentPosition: '',
    organization: '',
    areaOfResearch: ''
  };
  showAllFieldsRequiredError: boolean;
  creatingAccount: boolean;
  accountCreated: boolean;
  usernameConflictError = false;
  usernameFocused = false;
  usernameCheckTimeout: NodeJS.Timer;
  usernameCheckInProgress = false;

  @ViewChild('infoIcon') infoIcon: ElementRef;

  // TODO: Injecting the parent component is a bad separation of concerns, as
  // well as injecting LoginComponent. Should look at refactoring these
  // interactions.
  constructor(
    private profileService: ProfileService
  ) {}

  ngAfterViewInit(): void {
    // This is necessary to avoid clarity, because clarity's tooltip library
    // automatically sets tabindex to 0.
    this.infoIcon.nativeElement.tabIndex = -1;
  }

  createAccount(): void {
    if (this.usernameConflictError || this.usernameInvalidError) {
      return;
    }
    this.showAllFieldsRequiredError = false;
    const requiredFields =
        [this.profile.givenName, this.profile.familyName,
         this.profile.username, this.profile.contactEmail,
         this.profile.currentPosition, this.profile.organization,
         this.profile.areaOfResearch];
    if (requiredFields.some(isBlank)) {
      this.showAllFieldsRequiredError = true;
      return;
    } else if (this.isUsernameValidationError) {
      return;
    }

    const request: CreateAccountRequest = {
      profile: this.profile,
      invitationKey: ''
    };
    this.creatingAccount = true;
    this.profileService.createAccount(request).subscribe((profile) => {
      this.profile = profile;
      this.creatingAccount = false;
      this.accountCreated = true;
    }, () => {
      this.creatingAccount = false;
    });
  }

  get usernameInvalidError(): boolean {
    const username = this.profile.username;
    if (isBlank(username)) {
      return false;
    }
    if (username.trim().length > 64) {
      return true;
    }
    // Include alphanumeric characters, -'s, _'s, apostrophes, and single .'s in a row.
    if (username.includes('..') || username.endsWith('.')) {
      return true;
    }
    return !(new RegExp(/^[\w'-][\w.'-]*$/).test(username));
  }

  usernameChanged(): void {
    this.usernameConflictError = false;
    // TODO: This should use a debounce, rather than manual setTimeout()s.
    clearTimeout(this.usernameCheckTimeout);
    this.usernameCheckInProgress = true;
    this.usernameCheckTimeout = setTimeout(() => {
      if (!this.profile.username.trim()) {
        this.usernameCheckInProgress = false;
        return;
      }
      this.profileService.isUsernameTaken(this.profile.username).subscribe((response) => {
        this.usernameCheckInProgress = false;
        this.usernameConflictError = response.isTaken;
      }, () => {
        this.usernameCheckInProgress = false;
      });
    }, 300);
  }

  leaveFocusUsername(): void {
    this.usernameFocused = false;
  }

  enterFocusUsername(): void {
    this.usernameFocused = true;
  }

  get isUsernameValidationError(): boolean {
    return this.usernameConflictError || this.usernameInvalidError;
  }

  get showUsernameValidationError(): boolean {
    if (isBlank(this.profile.username) || this.usernameCheckInProgress) {
      return false;
    }
    return this.isUsernameValidationError;
  }

  get showUsernameValidationSuccess(): boolean {
    if (isBlank(this.profile.username) || this.usernameFocused || this.usernameCheckInProgress) {
      return false;
    }
    return !this.isUsernameValidationError;
  }

  get showFirstNameValidationSuccess(): boolean {
    return this.profile.givenName.length <= 80;
  }

  get showLastNameValidationSuccess(): boolean {
    return this.profile.familyName.length <= 80;
  }

  get showCurrentPositionValidationSuccess(): boolean {
    return this.profile.currentPosition.length <= 255;
  }

  get showOrganizationValidationSuccess(): boolean {
    return this.profile.organization.length <= 255;
  }

}
