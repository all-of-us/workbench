import {Component} from '@angular/core';

import {SignedOutComponent} from '../signed-out/component';

import {InvitationVerificationRequest, ProfileService} from 'generated';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

@Component ({
  selector : 'app-invitation-key',
  styleUrls: ['./component.css',
              '../../styles/template.css'],
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
      private profileService: ProfileService,
      private signedOutComponent: SignedOutComponent
  ) {
      this.invitationKeyVerifed = false;
      this.invitationKeyReq = false;
      this.invitationKeyInvalid = false;
      this.requestSent = false;
      // This is a workaround for ExpressionChangedAfterItHasBeenCheckedError from angular
      setTimeout(() => {
        this.signedOutComponent.backgroundImgSrc = '/assets/images/invitation-female.png';
        this.signedOutComponent.smallerBackgroundImgSrc =
            '/assets/images/invitation-female-standing.png';
      }, 0);

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
