import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {BlockscoreIdVerificationStatus, Profile, ProfileService} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class ProfileDashboardComponent implements OnInit {
  progressCount: number;
  progressPercent: string;
  profile: Profile;
  verificationImage: boolean;
  termsOfServiceImage: boolean;
  ethicsImage: boolean;
  demographicSurveyImage: boolean;
  profileVerification: boolean;
  profileTOS: boolean;
  profileVerified: boolean;
  profileEthics: boolean;
  profileDemographic: boolean;

  constructor(private profileService: ProfileService,
      private router: Router) {
    this.progressPercent = '0%';
    this.verificationImage = false;
    this.termsOfServiceImage = false;
    this.ethicsImage = false;
    this.demographicSurveyImage = false;
    this.profileVerification = true;
    this.profileTOS = false;
    this.profileVerified = false;
    this.profileEthics = false;
    this.profileDemographic = false;
    this.progressCount = 0;
  }

  ngOnInit(): void {
    this.profileService.getMe().subscribe(
        (profile: Profile) => {
          this.profile = profile;
          this.calculateProgress();
        });
  }

  navBarClick(action): void {
    this.profileTOS = false;
    this.profileEthics = false;
    this.profileVerification = false;
    this.profileDemographic = false;
    switch(action) {
      case 'verification' : {
        this.profileVerification = true;
        break;
      }
      case 'tos' : {
        this.profileTOS = true;
        break;
      }
      case 'ethics' : {
        this.profileEthics = true;
        break;
      }
      case 'demographic' : {
        this.profileDemographic = true;
        break;
      }
    }
  }

  calculateProgress(): void {
    if (!this.verificationImage  &&
        this.profile.blockscoreIdVerificationStatus === BlockscoreIdVerificationStatus.VERIFIED) {
      this.progressCount++;
      this.verificationImage = true;
    }
    if (!this.demographicSurveyImage && null != this.profile.demographicSurveyCompletionTime) {
      this.progressCount++;
      this.demographicSurveyImage = true;
    }
    if (!this.ethicsImage && null != this.profile.ethicsTrainingCompletionTime) {
      this.progressCount++;
      this.ethicsImage = true;
    }
    if (!this.termsOfServiceImage && null != this.profile.termsOfServiceCompletionTime) {
      this.progressCount++;
      this.termsOfServiceImage = true;
    }
    this.progressPercent = (this.progressCount / 4) * 100 + '%';
  }

}