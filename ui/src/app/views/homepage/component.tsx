import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {BugReportComponent} from 'app/views/bug-report/component';

import * as React from 'react';

import {
  BillingProjectStatus,
  IdVerificationStatus,
  PageVisit,
  Profile,
  ProfileService
} from 'generated';
import {reactStyles, ReactWrapperBase} from "app/utils";


const styles = reactStyles({
  mainHeader: {
    color: '#FFFFFF',
    fontSize: 28,
    fontWeight: 400,
    width: '25.86%',
    display: 'flex',
    minWidth: '18.2rem',
    marginLeft: '3%',
    marginTop: '2.58%',
    letterSpacing: 'normal'
  },
  minorHeader: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: 300,
    display: 'flex',
    marginTop: '1rem',
    lineHeight: '22px'
  },
  text: {
    color: '#FFFFFF',
    contSize: 16,
    lineHeight: '22px',
    fontWeight: 100
  }
});

export class AccountLinking extends React.Component<{}, {}> {
  constructor(props) {
    super(props);
  }

  render() {
    return <React.Fragment>
      <div style={{display: 'flex', flexDirection: 'row'}}>
        <div style={{display: 'flex', flexDirection: 'column'}}>
          <div style={styles.mainHeader}>Researcher Workbench</div>
          <div style={{marginLeft: '2rem'}}>
            <div style={styles.minorHeader}>In order to get access to data and tools
              please complete the following:</div>
            <div style={styles.text}>Please login to your ERA Commons account and complete
              the online training courses in order to gain full access to the Researcher
              Workbench data and tools.</div>
          </div>
        </div>
      </div>
    </React.Fragment>;
  }
}

@Component({
  selector: 'app-account-linking',
  template: '<div #root></div>',
})
export class AccountLinkingComponent extends ReactWrapperBase {
  constructor() {
    super(AccountLinking, []);
  }
}


@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})

export class HomepageComponent implements OnInit, OnDestroy {
  private static pageId = 'homepage';
  @ViewChild('myVideo') myVideo: any;
  profile: Profile;
  view: any[] = [180, 180];
  numberOfTotalTasks = 4;
  completedTasksName = 'Completed';
  unfinishedTasksName = 'Unfinished';
  open = false;
  src = '';
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
  firstVisit = true;
  newPageVisit: PageVisit = { page: HomepageComponent.pageId};
  footerLinks = [
    {
      title: 'Working Within Researcher Workbench',
      links: ['Researcher Workbench Mission',
        'User interface components',
        'What to do when things go wrong',
        'Contributing to the Workbench']
    },
    {
      title: 'Workspace',
      links: ['Workspace interface components',
        'User interface components',
        'Collaborating with other researchers',
        'Sharing and Publishing Workspaces']
    },
    {
      title: 'Working with Notebooks',
      links: ['Notebook interface components',
        'Notebooks and data',
        'Collaborating with other researchers',
        'Sharing and Publishing Notebooks']
    }];
  @ViewChild(BugReportComponent)
  bugReportComponent: BugReportComponent;
  quickTour: boolean;
  linkAccount: boolean;

  constructor(
    private profileService: ProfileService,
    private profileStorageService: ProfileStorageService,
    private route: ActivatedRoute,
    private router: Router,
  ) {
    // create bound methods to use as callbacks
    this.closeQuickTour = this.closeQuickTour.bind(this);
  }

  ngOnInit(): void {
    this.profileService.getMe().subscribe(profile => {
      if (profile.pageVisits) {
        this.firstVisit = !profile.pageVisits.some(v =>
        v.page === HomepageComponent.pageId);
      }
    },
      e => {},
      () => {
        if (this.firstVisit) {
          this.quickTour = true;
        }
        this.profileService.updatePageVisits(this.newPageVisit).subscribe();
      });
    this.profileStorageService.profile$.subscribe((profile) => {
      // This will block workspace creation until the billing project is initialized
      if (profile.freeTierBillingProjectStatus === BillingProjectStatus.Ready) {
        this.billingProjectInitialized = true;
      } else {
        this.billingProjectQuery = setTimeout(() => {
          this.profileStorageService.reload();
        }, 10000);
      }
      this.profile = profile;
      this.reloadSpinner();
    });
    this.profileStorageService.reload();
    this.linkAccount = true;
  }

  public closeQuickTour(): void {
    this.quickTour = false;
  }

  play(type): void {
    this.src = '/assets/videos/Workbench Tutorial - Cohorts.mp4';
    if (type === 'notebook') {
      this.src = '/assets/videos/Workbench Tutorial - Notebooks.mp4';
    }
    this.open = true;
  }

  public get completedTasks() {
    let completedTasks = 0;
    if (this.profile === undefined) {
      return completedTasks;
    }
    if (this.profile.idVerificationStatus === IdVerificationStatus.VERIFIED) {
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
    this.router.navigate(['workspaces/build'], {relativeTo : this.route});
  }

  navigateToProfile(): void {
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
    return !(new Date().getTime() - this.firstSignIn.getTime() > 7 * 24 * 60 * 60 * 1000);
  }
}
