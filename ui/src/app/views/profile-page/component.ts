import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ErrorHandlingService} from 'app/services/error-handling.service';
import {SignInService} from 'app/services/sign-in.service';

import {
  BlockscoreIdVerificationStatus,
  ErrorResponse,
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
  profile: Profile;
  profileImage: string;
  profileLoaded = false;
  errorText: string;
  editing = false;
  view: any[] = [120, 120];
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
      private errorHandlingService: ErrorHandlingService,
      private profileService: ProfileService,
      private route: ActivatedRoute,
      private router: Router,
      private signInService: SignInService
  ) {}

  ngOnInit(): void {
    this.profileImage = this.signInService.profileImage;
    this.errorText = null;
    this.profileService.getMe().subscribe(
      (profile: Profile) => {
        this.profile = profile;
        this.profileLoaded = true;
        this.reloadSpinner();
      });
  }

  submitChanges(): void {
    this.profileService.updateProfile(this.profile).subscribe(
      () => {
        this.editing = false;
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
      this.profile = profile;
      this.reloadSpinner();
    });
  }


  completeEthicsTraining(): void {
    this.profileService.completeEthicsTraining().subscribe((profile) => {
      this.profile = profile;
      this.reloadSpinner();
    });
  }

  submitDemographicSurvey(): void {
    this.profileService.submitDemographicsSurvey().subscribe((profile) => {
      this.profile = profile;
      this.reloadSpinner();
    });
  }
}
