import {Component, OnInit} from '@angular/core';
import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {SignInService} from 'app/services/sign-in.service';
import {deepCopy} from 'app/utils/index';

import {
  BlockscoreIdVerificationStatus,
  ErrorResponse,
  InstitutionalAffiliation,
  Profile,
  ProfileService,
} from 'generated';

@Component({
  styleUrls: ['../../styles/buttons.css',
              '../../styles/cards.css',
              '../../styles/headers.css',
              '../../styles/inputs.css',
              './component.css'],
  templateUrl: './component.html',
})
export class ProfilePageComponent implements OnInit {
  editHover = false;
  profile: Profile;
  workingProfile: Profile;
  profileImage: string;
  profileLoaded = false;
  errorText: string;
  editing = false;
  view: any[] = [200, 200];
  numberOfTotalTasks = 4;
  completedTasksName = 'Completed';
  unfinishedTasksName = 'Unfinished';
  colorScheme = {
    domain: ['#8BC990', '#C7C8C8']
  };
  spinnerValues = [
    {
      'name': this.completedTasksName,
      'value': this.completedTasks
    },
    {
      'name': this.unfinishedTasksName,
      'value': this.numberOfTotalTasks - this.completedTasks
    }
  ];
  constructor(
      private profileService: ProfileService,
      private profileStorageService: ProfileStorageService,
      private signInService: SignInService
  ) {}

  ngOnInit(): void {
    this.profileImage = this.signInService.profileImage;
    this.errorText = null;

    this.profileStorageService.profile$.subscribe(profile => {
      this.profile = profile;
      if (!this.editing) {
        this.workingProfile = <Profile> deepCopy(profile);
      }
      this.profileLoaded = true;
      this.reloadSpinner();
    });

    this.profileStorageService.reload();
  }

  submitChanges(): void {

    this.profileService.updateProfile(this.workingProfile).subscribe(
      () => {
        this.profile = <Profile> deepCopy(this.workingProfile);
        this.editing = false;
        this.profileStorageService.reload();
      },
      error => {
        // if MailChimp throws an error, display to the user
        const response: ErrorResponse = ErrorHandlingService.convertAPIError(error);
        if (response.message && response.errorClassName.includes('MailchimpException')) {
          this.errorText = response.message;
        } else {
          this.errorText = '';
        }
      });
  }

  public get completedTasks() {
    let completedTasks = 0;
    if (this.profile === undefined) {
      return completedTasks;
    }
    if (this.profile.blockscoreIdVerificationStatus === BlockscoreIdVerificationStatus.VERIFIED) {
      completedTasks += 1;
    }
    if (this.profile.demographicSurveyCompletionTime !== null) {
      completedTasks += 1;
    }
    if (this.profile.ethicsTrainingCompletionTime !== null) {
      completedTasks += 1;
    }
    if (this.profile.termsOfServiceCompletionTime !== null) {
      completedTasks += 1;
    }
    return completedTasks;
  }

  public get completedTasksAsPercentage() {
    return this.completedTasks / this.numberOfTotalTasks * 100;
  }

  reloadSpinner(): void {
    this.spinnerValues = [
      {
        'name': this.completedTasksName,
        'value': this.completedTasks
      },
      {
        'name': this.unfinishedTasksName,
        'value': this.numberOfTotalTasks - this.completedTasks
      }
    ];
  }

  submitTermsOfService(): void {
    this.profileService.submitTermsOfService().subscribe((profile) => {
      this.profile.termsOfServiceCompletionTime = profile.termsOfServiceCompletionTime;
      this.workingProfile.termsOfServiceCompletionTime = profile.termsOfServiceCompletionTime;
      this.reloadSpinner();
    });
  }


  completeEthicsTraining(): void {
    this.profileService.completeEthicsTraining().subscribe((profile) => {
      this.profile.ethicsTrainingCompletionTime = profile.ethicsTrainingCompletionTime;
      this.workingProfile.ethicsTrainingCompletionTime = profile.ethicsTrainingCompletionTime;
      this.reloadSpinner();
    });
  }

  submitDemographicSurvey(): void {
    this.profileService.submitDemographicsSurvey().subscribe((profile) => {
      this.profile.demographicSurveyCompletionTime = profile.demographicSurveyCompletionTime;
      this.workingProfile.demographicSurveyCompletionTime = profile.demographicSurveyCompletionTime;
      this.reloadSpinner();
    });
  }

  reloadProfile(): void {
    this.workingProfile = <Profile> deepCopy(this.profile);
    this.editing = false;
  }

  pushAffiliation(): void {
    if (!this.editing) {
      return;
    }
    if (this.workingProfile) {
      if (this.workingProfile.institutionalAffiliations === undefined) {
        this.workingProfile.institutionalAffiliations = [];
      }
      this.workingProfile.institutionalAffiliations.push(
        {
          role: '',
          institution: '',
        }
      );
    }
  }

  removeAffiliation(affiliation: InstitutionalAffiliation): void {
    if (!this.editing) {
      return;
    }
    if (this.workingProfile) {
      const positionOfValue = this.workingProfile.institutionalAffiliations
        .findIndex(item => item === affiliation);
      if (positionOfValue !== -1) {
        this.workingProfile.institutionalAffiliations.splice(positionOfValue, 1);
      }
    }
  }

  requestVerification(): void {
    this.profileService.submitIdVerification().subscribe((profile) => {
      this.profile.requestedIdVerification = profile.requestedIdVerification;
      this.workingProfile.requestedIdVerification = profile.requestedIdVerification;
    });
  }
}
