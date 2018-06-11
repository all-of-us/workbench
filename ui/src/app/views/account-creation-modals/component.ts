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

  resendEmail() {
    this.changingEmail = true;
    this.contactEmail = '';
  }

  send() {
    const request: UpdateContactEmailRequest = {
      username: this.username + '@' + this.gsuiteDomain,
      contactEmail: this.contactEmail
    };
    this.updateEmail.emit(this.contactEmail);
    this.profileService.updateContactEmail(request).subscribe(() => {
      this.changingEmail = false;
    });
  }
}
