import {Component, OnInit} from '@angular/core';

import {ErrorHandlingService} from 'app/services/error-handling.service';

import {
  IdVerificationReviewRequest,
  Profile,
  ProfileService,
} from 'generated';


/**
 * Review ID Verifications. Users with the REVIEW_RESEARCH_PURPOSE permission use this
 * to view other users' workspaces for which a review has been requested, and approve/reject them.
 */
// TODO(RW-85) Design this UI. Current implementation is a rough sketch.
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
              for (const profile of profilesResp.profileList) {
                this.profiles.push(profile);
              }
              this.contentLoaded = true;
            });
  }

  approve(contactEmail: string, approved: boolean): void {
    const request = <IdVerificationReviewRequest>{
      approved: approved,
    };
    this.errorHandlingService.retryApi(this.profileService.reviewIdVerification(
        contactEmail, request));
  }
 }
