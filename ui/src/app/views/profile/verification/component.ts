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
  constructor(
      private profileService: ProfileService
  ) {}

  ngOnInit(): void {
    this.profileService.getMe().subscribe(
        (profile: Profile) => {
      this.profile = profile;
      this.profileLoaded = true;
    });
  }

}
