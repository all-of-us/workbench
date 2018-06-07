import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {BugReportComponent} from 'app/views/bug-report/component';

import {
  BillingProjectStatus,
  BlockscoreIdVerificationStatus,
  DataAccessLevel,
  Profile,
  ProfileService
} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})

export class HomepageComponent implements OnInit, OnDestroy {
  AouLogoFooter = '/assets/images/all-of-us-logo-footer.svg';
  tipHeadline = 'Welcome Back to All of Us!';
  tipSubtitle = 'Thanks for returning.';
  tipDetail = 'You can use the buttons below to view your workspaces, ' +
    'or create a new one. Please do not hesitate to use the "Report a Bug" ' +
    'button below (or found at any time via the Profile menu) ' +
    'if you experience any difficulties.';
  tipButton = 'REPORT A BUG';
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
  billingProjectInitialized = false;
  billingProjectQuery: NodeJS.Timer;
  firstSignIn: Date;
  private enforceRegistered: boolean;
  @ViewChild(BugReportComponent)
  bugReportComponent: BugReportComponent;

  constructor(
    private serverConfigService: ServerConfigService,
    private profileService: ProfileService,
    private profileStorageService: ProfileStorageService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.serverConfigService.getConfig().subscribe((config) => {
      this.enforceRegistered = config.enforceRegistered;
    });
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
      this.profile = profile;
      this.reloadSpinner();
      if (profile.firstSignInTime === null) {
        this.tipHeadline = 'Welcome to All of Us';
        this.tipSubtitle = 'This is your first visit with us!';
        this.tipDetail = 'We are setting up your account. When this process is ' +
          'completed you will be able to use the "Create a Workspace" button ' +
          'below to create a new Workspace.';
        this.tipButton = '';
      }
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
   this.router.navigate(['profile']);
  }

  listWorkspaces(): void {
   this.router.navigate(['workspaces']);
  }

  // The user is FC initialized and has access to the CDR, if enforced in this
  // environment.
  hasCdrAccess(): boolean {
    if (!this.profile) {
      return false;
    }
    if (!this.enforceRegistered) {
      return true;
    }
    return [
      DataAccessLevel.Registered,
      DataAccessLevel.Protected
    ].includes(this.profile.dataAccessLevel);
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
