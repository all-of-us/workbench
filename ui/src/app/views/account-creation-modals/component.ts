import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';

import {ProfileService} from 'generated';
import {ResendWelcomeEmailRequest, UpdateContactEmailRequest} from 'generated';
import {ServerConfigService} from '../../services/server-config.service';

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
  @Input('nonceCode') nonceCode: string;

  @Output() updateEmail = new EventEmitter<string>();

  constructor(
    private profileService: ProfileService,
    serverConfigService: ServerConfigService
  ) {
    serverConfigService.getConfig().subscribe((config) => {
      this.gsuiteDomain = config.gsuiteDomain;
    });
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
      contactEmail: this.contactEmail,
      nonce: parseInt(this.nonceCode)
    };
    this.updateEmail.emit(this.contactEmail);
    this.profileService.updateContactEmail(request).subscribe(() => {
      this.changingEmail = false;
    });
    this.send();
  }

  send() {
    const request: ResendWelcomeEmailRequest = {
      username: this.username + '@' + this.gsuiteDomain,
      nonce: parseInt(this.nonceCode)
    };
    this.profileService.resendWelcomeEmail(request).subscribe(() => {
      this.resendingEmail = false;
    });
  }
}
