import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';

import {ProfileService} from 'generated';
import {ResendWelcomeEmailRequest, UpdateContactEmailRequest} from 'generated';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

@Component({
  selector: 'app-account-creation-modals',
  templateUrl: './component.html',
  styleUrls: ['./component.css',
    '../../styles/buttons.css'],
})
export class AccountCreationModalsComponent {
  changingEmail = false;
  resendingEmail = false;
  contactEmail: string;
  emailOffFocus = true;
  waiting = false;
  @Input('username') username: string;
  @Input('creationNonce') creationNonce: string;

  @Output() updateEmail = new EventEmitter<string>();

  constructor(
    private profileService: ProfileService
  ) {
    this.contactEmail = '';
    this.emailOffFocus = false;
  }

  updateAndSendEmail() {
    this.changingEmail = true;
    this.contactEmail = '';
  }

  resendInstructions() {
    this.resendingEmail = true;
  }

  updateAndSend() {
    this.waiting = true;
    if (this.contactEmailInvalidError) {
      return;
    }
    this.updateEmail.emit(this.contactEmail);
    this.profileService.updateContactEmail({
      username: this.username,
      contactEmail: this.contactEmail,
      creationNonce: this.creationNonce
    }).subscribe(() => {
      this.resendingEmail = false;
      this.waiting = false;
      this.changingEmail = false;
    });
  }

  send() {
    this.waiting = true;
    this.profileService.resendWelcomeEmail({
      username: this.username,
      creationNonce: this.creationNonce
    }).subscribe(() => {
      this.resendingEmail = false;
      this.waiting = false;
    });
  }

  leaveFocusEmail(): void {
    this.emailOffFocus = true;
  }

  enterFocusEmail(): void {
    this.emailOffFocus = false;
  }

  get contactEmailInvalidError(): boolean {
    const contactEmail = this.contactEmail;
    return !(new RegExp(/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/).test(contactEmail));
  }

  get showEmailValidationError(): boolean {
    if (isBlank(this.contactEmail) || !this.emailOffFocus) {
      return false;
    }
    return this.contactEmailInvalidError;
  }
}
