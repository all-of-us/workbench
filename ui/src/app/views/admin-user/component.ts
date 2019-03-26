import {Component, OnInit} from '@angular/core';

import {
  AuthDomainService,
  Profile,
  ProfileService,
} from 'generated';

/**
 * Users with the ACCESS_MODULE_ADMIN permission use this
 * to manually set (approve/reject) the beta access state of a user, as well as
 * other access module bypasses.
 */
@Component({
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class AdminUserComponent implements OnInit {
  profiles: Profile[] = [];
  contentLoaded = false;

  constructor(
    private authDomainService: AuthDomainService,
    private profileService: ProfileService
  ) {}

  ngOnInit(): void {
    this.loadProfiles();
  }

  loadProfiles(): void {
    this.contentLoaded = false;
    this.profileService.getAllUsers()
      .subscribe(
        profilesResp => {
          this.profiles = this.sortProfileList(profilesResp.profileList);
          this.contentLoaded = true;
        });
  }

  updateUserDisabledStatus(disable: boolean, profile: Profile): void {
    this.authDomainService.updateUserDisabledStatus(
        {email: profile.username, disabled: disable}).subscribe(() => {
          this.loadProfiles();
        });
  }

  // We want to sort first by beta access status, then by
  // submission time (newest at the top), then alphanumerically.
  private sortProfileList(profileList: Array<Profile>): Array<Profile> {
    return profileList.sort((a, b) => {
      // put disabled accounts at the bottom
      if (a.disabled && b.disabled) {
        return this.timeCompare(a, b);
      }
      if (a.disabled) {
        return 1;
      }
      if (!!a.betaAccessBypassTime === !!b.betaAccessBypassTime) {
        return this.timeCompare(a, b);
      }
      if (!!b.betaAccessBypassTime) {
        return -1;
      }
      return 1;
    });
  }

  private timeCompare(a: Profile, b: Profile): number {
    if (a.betaAccessRequestTime === b.betaAccessRequestTime) {
      return this.nameCompare(a, b);
    } else if (a.betaAccessRequestTime === null) {
      return 1;
    } else if (b.betaAccessRequestTime === null) {
      return -1;
    }
    return b.betaAccessRequestTime - a.betaAccessRequestTime;
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
