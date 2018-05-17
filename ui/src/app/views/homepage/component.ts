import {Component, OnDestroy, OnInit, Input} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ProfileStorageService} from 'app/services/profile-storage.service';

import {
  BlockscoreIdVerificationStatus,
  BillingProjectStatus,
  Profile,
  ProfileService
} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})

export class HomepageComponent implements OnInit, OnDestroy {
  AouLogo = '/assets/images/all-of-us-logo.svg';
  profile: Profile;
  view: any[] = [180, 180];
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
  FCAccountInitialized = false;
  billingProjectInitialized = false;
  billingProjectQuery: NodeJS.Timer;
  firstSignIn: Date;
  constructor(
    private profileService: ProfileService,
    private profileStorageService: ProfileStorageService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}
  ngOnInit(): void {
    this.profileStorageService.profile$.subscribe((profile) => {
      if (this.firstSignIn === undefined) {
        this.firstSignIn = new Date(profile.firstSignInTime);
      }
      if (profile.freeTierBillingProjectStatus === BillingProjectStatus.Ready) {
        this.billingProjectInitialized = true;
      } else {
        this.billingProjectQuery = setTimeout(() => {
          this.profileStorageService.reload();
        }, 10000);
      }
      if(profile != null) {
        this.FCAccountInitialized = true;
      }
      this.profile = profile;
      this.reloadSpinner();
    });
    this.profileStorageService.reload();
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

  ngOnDestroy(): void {
    clearTimeout(this.billingProjectQuery);
  }

  addWorkspace(): void {
    this.router.navigate(['workspace/build'], {relativeTo : this.route});
  }
   navigateToProfile(): void {
    console.log("Click!");
    this.router.navigate(['profile']);
   }

   listWorkspaces(): void {
    this.router.navigate(['workspaces']);
   }



  get twoFactorBannerEnabled() {
    if (this.firstSignIn == null) {
      return false;
    }
    // Don't show the banner after 1 week as their account would
    // have been disabled had they not enabled 2-factor auth.
    if (new Date().getTime() - this.firstSignIn.getTime() > 1 * 7 * 24 * 60 * 60 * 1000) {
      return false;
    }
    return true;
  }
}
