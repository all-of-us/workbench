import {Component} from '@angular/core';
import {InvitationVerRequest, ProfileService} from 'generated';


function isBlank(s: string) {
    return (!s || /^\s*$/.test(s));
}
@Component ({
    selector : 'app-invitation-code',
    styleUrls: ['./component.css'],
    templateUrl: './component.html'
})

export class InvitationKeyComponent {

    invitationKey: string;
    invitationKeyVerifed: boolean;
    invitationKeyReq: boolean;
    invitationKeyInvalid: boolean;
    constructor(
        private profileService: ProfileService
    ) {
        this.invitationKeyVerifed = false;
        this.invitationKeyReq = false;
        this.invitationKeyInvalid = false;
    }

    next(): void {
        if (isBlank(this.invitationKey)) {
            this.invitationKeyReq = true;
            this.invitationKeyInvalid = false;
            return;
        }
        this.invitationKeyReq = false;
        this.invitationKeyInvalid = false;

        const request: InvitationVerRequest = {
            invitationKey: this.invitationKey
        };
        this.profileService.invitationKeyVerification(request).subscribe(() => {
            this.invitationKeyVerifed = true;
        }, () => {
            this.invitationKeyInvalid = true;
        });
    }

}
