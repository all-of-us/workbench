import {Component} from '@angular/core';
import {ProfileDashboardComponent} from '../dashboard/component';
import {ProfileService} from 'generated';

@Component({
 selector : 'app-profile-ethics',
  templateUrl: './component.html',
})
export class ProfileEthicsComponent {

  constructor(private profileService: ProfileService,
      private profileDashboard: ProfileDashboardComponent) {
  }

  completeEthicsTraining(): void {
    this.profileService.completeEthicsTraining().subscribe(() => {
      this.profileDashboard.profile.ethicsTrainingCompletionTime = new Date().getTime();
      this.profileDashboard.calculateProgress();
    });
  }
}