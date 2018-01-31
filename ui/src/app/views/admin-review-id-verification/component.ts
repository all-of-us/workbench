import {Component, OnInit} from '@angular/core';

import {ErrorHandlingService} from 'app/services/error-handling.service';

import {
  BlockscoreIdVerificationStatus,
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

  setIdVerificationStatus(profile: Profile, newStatus: BlockscoreIdVerificationStatus): void {
    if (profile.blockscoreIdVerificationStatus !== newStatus) {
      this.contentLoaded = false;
      const request = <IdVerificationReviewRequest> {newStatus};
      this.errorHandlingService.retryApi(this.profileService.reviewIdVerification(
        profile.userId, request))
          .subscribe(
            profilesResp => {
              this.profiles = profilesResp.profileList;
              this.contentLoaded = true;
            });
    }
  }
 }
