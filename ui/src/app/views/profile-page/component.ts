import {Component, OnInit} from '@angular/core';
import {Comparator, StringFilter} from 'clarity-angular';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {SignInService} from 'app/services/sign-in.service';
import {BlockscoreVerificationStatus, Profile, ProfileService} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class ProfilePageComponent implements OnInit {
  verifiedStatusIsLoaded: boolean;
  verifiedStatus: BlockscoreVerificationStatus;

  constructor(
      private errorHandlingService: ErrorHandlingService,
      private profileService: ProfileService,
      private signInService: SignInService,
  ) {}

  ngOnInit(): void {
    this.getVerifiedStatus();
  }

  getVerifiedStatus(): void {
    this.profileService.getMe().subscribe((profile: Profile) => {
      this.verifiedStatus = profile.blockscoreVerificationStatus;
      this.verifiedStatusIsLoaded = true;
    });
  }

  deleteAccount(): void {
    this.profileService.deleteAccount().subscribe(() => {
      this.signInService.signOut();
    });
  }
}
