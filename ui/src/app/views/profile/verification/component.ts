import {Component, OnInit} from '@angular/core';

import {BlockscoreIdVerificationStatus, Profile, ProfileService} from 'generated';

@Component({
  selector : 'app-profile-page',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class ProfilePageComponent implements OnInit {
  profile: Profile;
  profileLoaded = false;
  editHover = false;
  termsOfService: boolean;
  constructor(
      private profileService: ProfileService
  ) {
    this.termsOfService = false;
  }

  ngOnInit(): void {
    this.profileService.getMe().subscribe(
        (profile: Profile) => {
      this.profile = profile;
      this.profileLoaded = true;
    });
  }

  verifyEmail(): void {
    const request = {
      verifyEmail: this.profile.contactEmail,
      username: this.profile.username
    };
    this.profileService.verifyEmail(request).subscribe(
      (response) => {
        this.profile = response;
      }
    );
  }
}
