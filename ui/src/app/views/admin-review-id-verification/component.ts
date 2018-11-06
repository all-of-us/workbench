import {Component, OnInit} from '@angular/core';

import {
  IdVerificationReviewRequest,
  IdVerificationStatus,
  Profile,
  ProfileService,
} from 'generated';

const verificationSortMap = {
  [IdVerificationStatus.UNVERIFIED]: 0,
  [IdVerificationStatus.REJECTED]: 1,
  [IdVerificationStatus.VERIFIED]: 2
};

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
    private profileService: ProfileService
  ) {}

  ngOnInit(): void {
    this.profileService.getIdVerificationsForReview()
      .subscribe(
          profilesResp => {
            this.profiles = this.sortProfileResponseList(profilesResp.profileList);
            this.contentLoaded = true;
          });
  }

  setIdVerificationStatus(profile: Profile, newStatus: IdVerificationStatus): void {
    if (profile.idVerificationStatus !== newStatus) {
      this.contentLoaded = false;
      const request = <IdVerificationReviewRequest> {newStatus};
      this.profileService.reviewIdVerification(profile.userId, request).subscribe(
        profilesResp => {
          this.profiles = this.sortProfileResponseList(profilesResp.profileList);
          this.contentLoaded = true;
        });
    }
  }

  // On talking to Kayla, we want to sort first by verification status, then by
  // submission time (newest at the top), then alphanumerically.
  sortProfileResponseList(profileList: Array<Profile>): Array<Profile> {
    return profileList.sort((profileOne, profileTwo) => {
      if (profileOne.idVerificationStatus === profileTwo.idVerificationStatus) {
        return this.timeCompare(profileOne, profileTwo);
      } else {
        if (verificationSortMap[profileOne.idVerificationStatus]
          < verificationSortMap[profileTwo.idVerificationStatus]) {
          return -1;
        } else {
          return 1;
        }
      }
    });
  }

  timeCompare(profileOne: Profile, profileTwo: Profile) {
    if (profileOne.idVerificationRequestTime === profileTwo.idVerificationRequestTime) {
      return this.nameCompare(profileOne, profileTwo);
    } else if (profileOne.idVerificationRequestTime === null) {
      return 1;
    } else if (profileTwo.idVerificationRequestTime === null) {
      return -1;
    } else {
      return profileTwo.idVerificationRequestTime - profileOne.idVerificationRequestTime;
    }
  }

  nameCompare(profileOne: Profile, profileTwo: Profile) {
    if (profileOne.familyName === profileTwo.familyName) {
      if (profileOne.givenName === profileTwo.givenName) {
        return 0;
      } else if (profileOne.givenName < profileTwo.givenName) {
        return -1;
      } else {
        return 1;
      }
    } else if (profileOne.familyName < profileTwo.familyName) {
      return -1;
    } else {
      return 1;
    }
  }
}
