import {Component, Input, OnInit} from '@angular/core';

import {AccountCreationService} from '../../services/account-creation.service';

import {ProfileService} from 'generated';
import {UpdateContactEmailRequest} from 'generated';

@Component({
  selector: 'app-account-creation-modals',
  templateUrl: './component.html',
  styleUrls: ['./component.css',
    '../../styles/buttons.css'],
  providers: [AccountCreationService]
})
export class AccountCreationModalsComponent implements OnInit {
  changingEmail = false;
  contactEmail: string;
  @Input('username') username: string;
  @Input('gsuiteDomain') gsuiteDomain: string;

  constructor(
    private profileService: ProfileService,
    private accountCreationService: AccountCreationService
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
    this.accountCreationService.updateEmail(request.contactEmail);
    this.profileService.updateContactEmail(request).subscribe(() => {
      this.changingEmail = false;
    });
  }
}