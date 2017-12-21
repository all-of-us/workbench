import {Component} from '@angular/core';
import {InvitationVerRequest} from '../../../generated';
import {ProfileService} from 'generated';


function isBlank(s: string) {
    return (!s || /^\s*$/.test(s));
}
@Component ({
    selector : 'app-invitation-code',
    styleUrls: ['./component.css'],
    templateUrl: './component.html'
})

export class InvitationCodeComponent {

    invitationKey: string;
    invitationSucc: boolean;
    invitationKeyReq: boolean;
    invitationKeyInvalid: boolean;
    constructor(private profileService: ProfileService) {
        this.invitationSucc = false;
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
        this.profileService.invitationCodeVerification(request).subscribe(() => {
            this.invitationSucc = true;
        }, () => {
            this.invitationKeyInvalid = true;
        });
    }

}
