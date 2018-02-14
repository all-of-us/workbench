import {Component, OnInit} from '@angular/core';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {Profile, ProfileService} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class ProfilePageComponent implements OnInit {
  profile: Profile;
  profileLoaded = false;
  editHover = false;
  constructor(
      private profileService: ProfileService,
  ) {}

  ngOnInit(): void {
    this.profileService.getMe().subscribe(
        (profile: Profile) => {
      this.profile = profile;
      this.profileLoaded = true;
    });
  }

  submitTermsOfService(): void {
    this.profileService.submitTermsOfService().subscribe();
  }


  completeEthicsTraining(): void {
    this.profileService.completeEthicsTraining().subscribe();
  }

  submitDemographicSurvey(): void {
    this.profileService.submitDemographicsSurvey().subscribe();
  }
}
