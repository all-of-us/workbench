import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';

import {ProfileService} from 'generated';
import {UpdateContactEmailRequest} from 'generated';

@Component({
  selector: 'app-account-creation-modals',
  templateUrl: './component.html',
  styleUrls: ['./component.css',
    '../../styles/buttons.css'],
})
export class AccountCreationModalsComponent implements OnInit {
  changingEmail = false;
  resendingEmail = false;
  contactEmail: string;
  @Input('username') username: string;
  @Input('gsuiteDomain') gsuiteDomain: string;

  @Output() updateEmail = new EventEmitter<string>();

  constructor(
    private profileService: ProfileService,
  ) {
    this.contactEmail = '';
  }

  ngOnInit() {
  }

  updateAndSendEmail() {
    this.changingEmail = true;
    this.contactEmail = '';
  }

  resendInstructions() {
    this.resendingEmail = true;
  }

  sendAndUpdate() {
    const request: UpdateContactEmailRequest = {
      username: this.username + '@' + this.gsuiteDomain,
      contactEmail: this.contactEmail
    };
    this.updateEmail.emit(this.contactEmail);
    this.profileService.updateContactEmail(request).subscribe(() => {
      this.changingEmail = false;
    });
    this.send();
  }

  send() {
    console.log(this.contactEmail);
    this.profileService.resendWelcomeEmail(this.contactEmail).subscribe(() => {});
    // resend email. Add in once Mandrill API is in.
  }
}
