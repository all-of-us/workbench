import {Component, OnInit} from '@angular/core';

import {ErrorHandlingService} from 'app/services/error-handling.service';

import {
  IdVerificationReviewRequest,
  Profile,
  ProfileService,
} from 'generated';

/**
 * Review ID Verifications. Users with the REVIEW_ID_VERIFICATION permission use this
 * to manually set (approve/reject) the ID verification state of a user.
 */
@Component({
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class AdminReviewIdVerificationComponent implements OnInit {
  profiles: Profile[] = [];
  contentLoaded = false;

  constructor(
      private errorHandlingService: ErrorHandlingService,
      private profileService: ProfileService
  ) {}

  ngOnInit(): void {
    this.errorHandlingService.retryApi(this.profileService.getIdVerificationsForReview())
        .subscribe(
            profilesResp => {
              this.profiles = profilesResp.profileList;
              this.contentLoaded = true;
            });
  }

  approve(profile: Profile, approved: boolean): void {
    const request = <IdVerificationReviewRequest> {approved};
    this.errorHandlingService.retryApi(this.profileService.reviewIdVerification(
        profile.userId, request))
        .subscribe(
            resp => {
              const i = this.profiles.indexOf(profile, 0);
              if (i >= 0) {
                this.profiles.splice(i, 1);
              }
            });
  }
 }
