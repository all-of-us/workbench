import {Component} from '@angular/core';
import {ProfileDashboardComponent} from '../dashboard/component';
import {ProfileService} from 'generated';

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