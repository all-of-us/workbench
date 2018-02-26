import {Component} from '@angular/core';
import {ProfileService} from 'generated';

import {ProfileDashboardComponent} from '../dashboard/component';

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

