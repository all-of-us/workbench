import {Component, OnInit} from '@angular/core';

import {
  AuthDomainService,
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
export class AdminUserComponent implements OnInit {
  profiles: Profile[] = [];
  contentLoaded = false;
  IdVerificationStatus = IdVerificationStatus;

  constructor(
    private authDomainService: AuthDomainService,
    private profileService: ProfileService
  ) {}

  ngOnInit(): void {
    this.loadProfiles();
  }

  loadProfiles(): void {
    this.contentLoaded = false;
    this.profileService.getIdVerificationsForReview()
      .subscribe(
        profilesResp => {
          this.profiles = this.sortProfileList(profilesResp.profileList);
          this.contentLoaded = true;
        });
  }

  // TODO: do we still need this endpoint?
  // is approving someone the same as bypassing them?
  setIdVerificationStatus(profile: Profile, newStatus: IdVerificationStatus): void {
    if (profile.idVerificationStatus !== newStatus) {
      this.contentLoaded = false;
      const request = <IdVerificationReviewRequest> {newStatus};
      this.profileService.reviewIdVerification(profile.userId, request).subscribe(
        profilesResp => {
          this.profiles = this.sortProfileList(profilesResp.profileList);
          this.contentLoaded = true;
        });
    }
  }

  updateUserDisabledStatus(disable: boolean, profile: Profile): void {
    this.authDomainService.updateUserDisabledStatus(
        {email: profile.username, disabled: disable}).subscribe(() => {
          this.loadProfiles();
        });
  }

  // We want to sort first by verification status, then by
  // submission time (newest at the top), then alphanumerically.
  private sortProfileList(profileList: Array<Profile>): Array<Profile> {
    return profileList.sort((a, b) => {
      if (a.idVerificationStatus === b.idVerificationStatus) {
        return this.timeCompare(a, b);
      }
      if (verificationSortMap[a.idVerificationStatus]
        < verificationSortMap[b.idVerificationStatus]) {
        return -1;
      }
      return 1;
    });
  }

  private timeCompare(a: Profile, b: Profile): number {
    if (a.idVerificationRequestTime === b.idVerificationRequestTime) {
      return this.nameCompare(a, b);
    } else if (a.idVerificationRequestTime === null) {
      return 1;
    } else if (b.idVerificationRequestTime === null) {
      return -1;
    }
    return b.idVerificationRequestTime - a.idVerificationRequestTime;
  }

  private nameCompare(a: Profile, b: Profile): number {
    if (a.familyName === null) {
      return 1;
    }
    if (a.familyName.localeCompare(b.familyName) === 0) {
      if (a.givenName === null) {
        return 1;
      }
      return a.givenName.localeCompare(b.givenName);
    }
    return a.familyName.localeCompare(b.familyName);
  }
}
