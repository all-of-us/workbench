import {Component} from '@angular/core';
import {ProfileDashboardComponent} from '../dashboard/component';
import {Profile, ProfileService} from 'generated';

@Component({
  selector : 'app-profile-tos',
  templateUrl: './component.html',
})
export class ProfileTermsOfServiceComponent {
  termsAccepted: boolean;

  constructor(private profileService: ProfileService,
      private profileDashboard: ProfileDashboardComponent) {
    this.profileService.getMe().subscribe(
        (profile: Profile) => {
          if (null == profile.termsOfServiceCompletionTime) {
            this.termsAccepted = false;
          } else {
            this.termsAccepted = true;
          }
        });
  }

  submitTermsOfService(): void {
    this.profileService.submitTermsOfService().subscribe(() =>{
    this.profileDashboard.profile.termsOfServiceCompletionTime = new Date().getTime();
    this.profileDashboard.calculateProgress();
    });
  }
}