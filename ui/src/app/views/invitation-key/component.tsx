import {Component, DoCheck, OnInit, ViewChildren} from '@angular/core';

import {LoginComponent} from '../login/component';

import {InvitationVerificationRequest, ProfileService} from 'generated';
import * as React from 'react';
import * as ReactDOM from 'react-dom';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

class InvitationReact extends React.Component<any, any> {
  render() {
    return <><div>ne invitation key</div></>;
  }
}

@Component ({
  selector : 'app-invitation-key',
  styleUrls: ['./component.css',
              '../../styles/template.css'],
  templateUrl: './component.html'
})

export class InvitationKeyComponent implements DoCheck, OnInit {

  invitationKey: string;
  invitationKeyVerified: boolean;
  invitationKeyReq: boolean;
  invitationKeyInvalid: boolean;
  invitationKeyRequestEmail: string;
  requestSent: boolean;
  @ViewChildren('invitationInput') invitationInput;

  constructor(
      private profileService: ProfileService,
      private loginComponent: LoginComponent
  ) {
      this.invitationKeyVerified = false;
      this.invitationKeyReq = false;
      this.invitationKeyInvalid = false;
      this.requestSent = false;
      // This is a workaround for ExpressionChangedAfterItHasBeenCheckedError from angular
      setTimeout(() => {
        this.loginComponent.backgroundImgSrc = '/assets/images/invitation-female.png';
        this.loginComponent.smallerBackgroundImgSrc =
            '/assets/images/invitation-female-standing.png';
      }, 0);

    }

  ngOnInit(): void {
    ReactDOM.render(React.createElement(InvitationReact),
        document.getElementById('in'));

  }
  ngDoCheck(): void {
    ReactDOM.render(React.createElement(InvitationReact),
        document.getElementById('in'));
  }

  next(): void {
    if (isBlank(this.invitationKey)) {
      this.invitationKeyReq = true;
      this.invitationKeyInvalid = false;
      this.invitationInput.first.nativeElement.focus();
      return;
    }
    this.invitationKeyReq = false;
    this.invitationKeyInvalid = false;
    const request: InvitationVerificationRequest = {
      invitationKey: this.invitationKey
    };
    this.profileService.invitationKeyVerification(request).subscribe(() => {
      this.invitationKeyVerified = true;
    }, () => {
      this.invitationKeyInvalid = true;
      this.invitationInput.first.nativeElement.focus();
    });
  }
}
