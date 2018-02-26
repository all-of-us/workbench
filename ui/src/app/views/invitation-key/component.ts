import {Component} from '@angular/core';
import {InvitationVerificationRequest, ProfileService} from 'generated';


function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

@Component ({
  selector : 'app-invitation-key',
  styleUrls: ['./component.css',
              '../../styles/buttons.css',
              '../../styles/inputs.css',
              '../../styles/headers.css'],
  templateUrl: './component.html'
})

export class InvitationKeyComponent {
  invitationKey: string;
  invitationKeyVerifed: boolean;
  invitationKeyReq: boolean;
  invitationKeyInvalid: boolean;
  invitationKeyRequestEmail: string;
  requestSent: boolean;
  constructor(
    private profileService: ProfileService
  ) {
      this.invitationKeyVerifed = false;
      this.invitationKeyReq = false;
      this.invitationKeyInvalid = false;
      this.requestSent = false;
    }

  next(): void {
    if (isBlank(this.invitationKey)) {
      this.invitationKeyReq = true;
      this.invitationKeyInvalid = false;
      return;
    }
    this.invitationKeyReq = false;
    this.invitationKeyInvalid = false;

    const request: InvitationVerificationRequest = {
      invitationKey: this.invitationKey
    };
    this.profileService.invitationKeyVerification(request).subscribe(() => {
      this.invitationKeyVerifed = true;
    }, () => {
      this.invitationKeyInvalid = true;
    });
  }

  requestKey(): void {
    this.profileService.requestInvitationKey(this.invitationKeyRequestEmail).subscribe(() => {
      this.requestSent = true;
    });
  }

}
