import {Component, OnInit} from '@angular/core';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {SignInService} from 'app/services/sign-in.service';
import {Profile, ProfileService} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class ProfilePageComponent implements OnInit {
  verifiedStatusIsLoaded: boolean;
  verifiedStatusIsValid: boolean;

  constructor(
      private errorHandlingService: ErrorHandlingService,
      private profileService: ProfileService,
      private signInService: SignInService,
  ) {}

  ngOnInit(): void {
    this.getVerifiedStatus();
  }

  getVerifiedStatus(): void {
    this.errorHandlingService.retryApi(this.profileService.getMe()).subscribe(
        (profile: Profile) => {
      this.verifiedStatusIsValid = profile.blockscoreVerificationIsValid;
      this.verifiedStatusIsLoaded = true;
    });
  }

  deleteAccount(): void {
    this.profileService.deleteAccount().subscribe(() => {
      this.signInService.signOut();
    });
  }
}
