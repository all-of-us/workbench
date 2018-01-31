import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {BlockscoreIdVerificationStatus, Profile, ProfileService} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class ProfileEditComponent implements OnInit {
  verifiedStatusIsLoaded: boolean;
  verifiedStatus: BlockscoreIdVerificationStatus;
  profile: Profile;
  profileLoaded = false;
  constructor(
      private errorHandlingService: ErrorHandlingService,
      private profileService: ProfileService,
      private route: ActivatedRoute,
      private router: Router,
  ) {}

  ngOnInit(): void {
    this.getVerifiedStatus();
  }

  getVerifiedStatus(): void {
    this.errorHandlingService.retryApi(this.profileService.getMe()).subscribe(
        (profile: Profile) => {
      this.verifiedStatus = profile.blockscoreIdVerificationStatus;
      this.verifiedStatusIsLoaded = true;
      this.profile = profile;
      this.profileLoaded = true;
    });
  }


  submitChanges(): void {
    this.errorHandlingService.retryApi(
        this.profileService.updateProfile(this.profile)).subscribe(() => {
        this.router.navigate(['../'], {relativeTo : this.route});
      }
    );
  }
}
