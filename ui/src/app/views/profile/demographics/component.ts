import {Component} from '@angular/core';
import {ProfileService} from 'generated';
import {ProfileDashboardComponent} from '../dashboard/component';


@Component({
  selector: 'app-profile-demographics',
  templateUrl: './component.html',
})
export class ProfileDemographicComponent {

  constructor(private profileService: ProfileService,
      private profileDashboard: ProfileDashboardComponent) {
  }

  submitDemographicSurvey(): void {
    this.profileService.submitDemographicsSurvey().subscribe(() => {
      this.profileDashboard.profile.demographicSurveyCompletionTime = new Date().getTime();
      this.profileDashboard.calculateProgress();
    });
  }
}

