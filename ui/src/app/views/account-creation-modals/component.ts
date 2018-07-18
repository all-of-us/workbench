import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';

import {ProfileService} from 'generated';
import {ResendWelcomeEmailRequest, UpdateContactEmailRequest} from 'generated';
import {ServerConfigService} from '../../services/server-config.service';

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
  @Input('gsuiteDomain') gsuiteDomain: string;

  @Output() updateEmail = new EventEmitter<string>();

  constructor(
    private profileService: ProfileService,
    serverConfigService: ServerConfigService
  ) {
    serverConfigService.getConfig().subscribe((config) => {
      this.gsuiteDomain = config.gsuiteDomain;
    });
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
    const request: UpdateContactEmailRequest = {
      username: this.username + '@' + this.gsuiteDomain,
      contactEmail: this.contactEmail
    };
    this.updateEmail.emit(this.contactEmail);
    this.profileService.updateContactEmail(request).subscribe(() => {
      this.resendingEmail = false;
      this.waiting = false;
      this.changingEmail = false;
    });
  }

  send() {
    this.waiting = true;
    const request: ResendWelcomeEmailRequest = {
      username: this.username + '@' + this.gsuiteDomain
    };
    this.profileService.resendWelcomeEmail(request).subscribe(() => {
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
