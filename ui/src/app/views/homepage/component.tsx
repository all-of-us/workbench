import {Component, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {navigate} from 'app/utils/navigation';
import {environment} from 'environments/environment';

import * as React from 'react';

import {ActivatedRoute} from '@angular/router';
import {Spinner} from "app/components/spinners";
import {
  Clickable,
  styles as buttonStyles
} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {configApi, profileApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withStyle} from 'app/utils';
import {
  BillingProjectStatus,
  PageVisit,
} from 'generated/fetch';
import {QuickTourReact} from "../quick-tour-modal/component";


const styles = reactStyles({
  mainHeader: {
    color: '#FFFFFF',
    fontSize: 28,
    fontWeight: 400,
    width: '25.86%',
    display: 'flex',
    minWidth: '18.2rem',
    marginLeft: '4%',
    marginTop: '4%',
    letterSpacing: 'normal'
  },
  minorHeader: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: 600,
    display: 'flex',
    marginTop: '1rem',
    lineHeight: '24px'
  },
  text: {
    color: '#FFFFFF',
    fontSize: 16,
    lineHeight: '22px',
    fontWeight: 150,
    marginTop: '3%'
  },
  infoBox: {
    padding: '1rem',
    backgroundColor: '#FFFFFF',
    borderRadius: '5px',
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between'
  },
  infoBoxHeader: {
    fontSize: 16,
    color: '#262262',
    fontWeight: 600
  },
  infoBoxBody: {
    color: '#000',
    lineHeight: '18px'
  },
  infoBoxButton: {
    color: '#FFFFFF',
    height: '49px',
    borderRadius: '5px',
    marginLeft: '1rem',
    maxWidth: '20rem'
  },
  error: {
    fontWeight: 300,
    color: '#DC5030',
    fontSize: '14px',
    marginTop: '.3rem',
    backgroundColor: '#FFF1F0',
    borderRadius: '5px',
    padding: '.3rem',
  }
});

export const Error = withStyle(styles.error)('div');

const AccountLinkingButton: React.FunctionComponent<{
  failed: boolean, completed: boolean, failedText: string,
  completedText: string, defaultText: string, onClick: Function
}> = ({failed, completed, defaultText, completedText, failedText, onClick}) => {
  if (failed) {
    return <Clickable style={{...buttonStyles.base,
      ...styles.infoBoxButton, backgroundColor: '#f27376'}} disabled={true}>
      <ClrIcon shape='exclamation-triangle'/>{failedText}
    </Clickable>;
  } else if (completed) {
    return <Clickable style={{...buttonStyles.base,
      ...styles.infoBoxButton, backgroundColor: '#8BC990'}} disabled={true}>
      <ClrIcon shape='check'/>{completedText}
    </Clickable>;
  } else {
    return <Clickable style={{...buttonStyles.base,
      ...styles.infoBoxButton, backgroundColor: '#2691D0'}}
                      onClick={onClick}
                      data-test-id={defaultText}>
      {defaultText}
    </Clickable>;
  }
};

export interface WorkbenchAccessTasksProps {
  eraCommonsLinked: boolean;
  eraCommonsError: string;
  trainingCompleted: boolean;
}

export class WorkbenchAccessTasks extends
    React.Component<WorkbenchAccessTasksProps, {}> {

  constructor(props: WorkbenchAccessTasksProps) {
    super(props);
  }

  static redirectToNiH(): void {
    const url = environment.shibbolethUrl + '/link-nih-account?redirect-url=' +
        encodeURIComponent(
          window.location.origin.toString() + '/nih-callback?token={token}');
    window.location.assign(url);
  }

  render() {
    return <React.Fragment>
      <div style={{display: 'flex', flexDirection: 'row'}}>
        <div style={{display: 'flex', flexDirection: 'column', width: '50%'}}>
          <div style={styles.mainHeader}>Researcher Workbench</div>
          <div style={{marginLeft: '2rem', flexDirection: 'column'}}>
            <div style={styles.minorHeader}>In order to get access to data and tools
              please complete the following:</div>
            <div style={styles.text}>Please login to your ERA Commons account and complete
              the online training courses in order to gain full access to the Researcher
              Workbench data and tools.</div>
          </div>
        </div>
        <div style={{flexDirection: 'column', width: '50%', padding: '1rem'}}>
          <div style={{...styles.infoBox, flexDirection: 'column'}}>
            <div style={{display: 'flex', flexDirection: 'row', justifyContent: 'space-between'}}>
              <div style={{flexDirection: 'column', width: '70%'}}>
                <div style={styles.infoBoxHeader}>Login to ERA Commons</div>
                <div style={styles.infoBoxBody}>Clicking the login link will bring you
                  to the ERA Commons Portal and redirect you back to the
                  Workbench once you are logged in.</div>
              </div>
              {/*TODO: RW-1184 Moodle training UI */}
              <AccountLinkingButton failed={false}
                                    completed={this.props.eraCommonsLinked}
                                    defaultText='Login'
                                    completedText='Linked'
                                    failedText='Error Linking Accounts'
                                    onClick={WorkbenchAccessTasks.redirectToNiH}/>
            </div>
            {this.props.eraCommonsError && <Error data-test-id='era-commons-error'>
              <ClrIcon shape='exclamation-triangle' class='is-solid'/>
              Error Linking NIH Username: {this.props.eraCommonsError} Please try again!
            </Error>}
          </div>
          <div style={{...styles.infoBox, marginTop: '0.7rem'}}>
            <div style={{flexDirection: 'column', width: '70%'}}>
              <div style={styles.infoBoxHeader}>Complete Online Training</div>
              <div style={styles.infoBoxBody}>Clicking the training link will bring you to
                the All of Us Compliance Training Portal, which will show you any
                outstanding training material to be completed.</div>
            </div>
            <AccountLinkingButton failed={false}
                                  completed={this.props.trainingCompleted}
                                  defaultText='Complete Training'
                                  completedText='Completed'
                                  failedText=''
                                  onClick={() => {}}/>
          </div>
        </div>
      </div>
    </React.Fragment>;
  }
}

@Component({
  selector: 'app-workbench-access-tasks',
  template: '<div #root></div>',
})
export class WorkbenchAccessTasksComponent extends ReactWrapperBase {
  @Input('eraCommonsLinked') eraCommonsLinked: WorkbenchAccessTasksProps['eraCommonsLinked'];
  @Input('eraCommonsError') eraCommonsError: WorkbenchAccessTasksProps['eraCommonsError'];
  @Input('trainingCompleted') trainingCompleted: WorkbenchAccessTasksProps['trainingCompleted'];
  constructor() {
    super(WorkbenchAccessTasks, ['eraCommonsLinked', 'eraCommonsError', 'trainingCompleted']);
  }
}

const homepageStyles = reactStyles({
  backgroundImage: {
    backgroundImage: 'url("/assets/images/AoU-HP-background.jpg")',
    backgroundRepeat: 'no-repeat',
    // -webkit-background-size: cover;
    // -moz-background-size: cover;
    // -o-background-size: cover;
    backgroundSize: 'cover',
    height: '100%',
    marginLeft: '-1rem',
    marginRight: '-0.6rem',
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'space-between'
  },
  singleCard: {
    height: '33.4%',
    width: '87.34%',
    minHeight: '18rem',
    maxHeight: '26rem',
    display: 'flex',
    flexDirection: 'column',
    borderRadius: '5px',
    backgroundColor: 'rgba(255, 255, 255, 0.15)',
    boxShadow: '0 0 2px 0 rgba(0, 0, 0, 0.12), 0 3px 2px 0 rgba(0, 0, 0, 0.12)',
    border: 'none',
    marginTop: '1rem'
  },
  headingLinks: {
    marginTop: '2.74%',
    height: '2.68%',
    fontSize: '14px',
    color: '#FFFFFF'
  },
  quickRow: {
    display: 'flex', justifyContent: 'flex-start', maxHeight: '26rem',
    flexDirection: 'row', marginLeft: '4rem', padding: '1rem'
  },
  quickTourLabel: {
    fontSize: 28, lineHeight: '34px', color: '#fff', paddingRight: '2.3rem',
    marginTop: '2rem', width: '31%'
  }
});

export class Homepage extends React.Component<{}, {
  billingProjectInitialized: boolean,
  firstVisit: boolean;
  videoOpen: boolean,
  accessTasksLoaded: boolean,
  accessTasksRemaining: boolean,
  eraCommonsLinked: boolean,
  eraCommonsError: string,
  trainingCompleted: boolean,
  quickTour: boolean
}> {
  private static pageId = 'homepage';
  private route: ActivatedRoute;

  constructor(props: Object) {
    super(props);
    this.state = {
      billingProjectInitialized: false,
      firstVisit: undefined,
      eraCommonsLinked: undefined,
      accessTasksRemaining: undefined,
      eraCommonsError: '',
      trainingCompleted: true,
      videoOpen: false,
      accessTasksLoaded: false,
      quickTour: false
    }
  }

  // TODO: this probably needs to check falsy more explicitly for loading case
   get accessTasksRemaining(): boolean {
    return !!(this.state.eraCommonsLinked && this.state.trainingCompleted);
   }

  componentDidMount() {
    this.validateNihToken();
    this.callProfile();
  }

  async validateNihToken() {
    const token = (new URL(window.location.href)).searchParams.get('token');
    if (token) {
      try {
        await profileApi().updateNihToken({ jwt: token });
      } catch (e) {
        this.setState({eraCommonsError: 'Error saving NIH Authentication status.'});
      }
    }
  }

  async callProfile() {
    try {
      const profile = await profileApi().getMe();
      if (profile.pageVisits) {
        this.setState({firstVisit: !profile.pageVisits.some(v =>
          v.page === Homepage.pageId)});
      }
      this.setState({eraCommonsLinked: !!profile.linkedNihUsername});

      if (this.route.snapshot.queryParams.workbenchAccessTasks) {
        // To reach the access tasks component from dev use /?workbenchAccessTasks=true
        this.setState({accessTasksRemaining: true, accessTasksLoaded: true});
      } else {
        try {
          const config = await configApi().getConfig();
          if (environment.enableComplianceLockout && config.enforceRegistered) {
            this.setState({
              accessTasksRemaining: this.accessTasksRemaining,
              accessTasksLoaded: true});
          } else {
            this.setState({accessTasksRemaining: false, accessTasksLoaded: true});
          }

        } catch(ex) {
          console.error('error fetching config: ' + ex.toString());
        }
      }

      if (profile.freeTierBillingProjectStatus === BillingProjectStatus.Ready) {
        this.setState({billingProjectInitialized: true});
      } else {
        // todo
        // this.billingProjectQuery = setTimeout(() => {
        //   this.profileStorageService.reload();
        // }, 10000);
      }
    } catch(e) {
      console.log('error fetching profile');
    } finally {
      await profileApi().updatePageVisits({ page: Homepage.pageId});
    }
  }

  openVideo(type: string): string {
    this.setState({videoOpen: true});
    if (type === 'notebook') {
      return '/assets/videos/Workbench Tutorial - Notebooks.mp4';
    } else {
      return '/assets/videos/Workbench Tutorial - Cohorts.mp4';
    }
  }


  render() {
    const {billingProjectInitialized, firstVisit, videoOpen, accessTasksLoaded,
        accessTasksRemaining, eraCommonsLinked, eraCommonsError, trainingCompleted} = this.state;
    const quickTourResources = [
      {
        src: '/assets/images/QT-thumbnail.svg',
        //onClick: () => this.setState({quickTour: true})
      }, {
        src: '/assets/images/cohorts-thumbnail.png',
        //onClick: () => this.openVideo('cohort')
      }, {
        src: '/assets/images/notebooks-thumbnail.png',
        //onClick: () => this.openVideo('notebook')
      }
    ];

    return <React.Fragment>
      <div style={homepageStyles.backgroundImage}>
        <div style={{display: 'flex', justifyContent: 'center'}}>
          <div style={homepageStyles.singleCard}>
            {accessTasksLoaded ?
              (accessTasksRemaining ?
                (<WorkbenchAccessTasks eraCommonsLinked={eraCommonsLinked}
                                eraCommonsError={eraCommonsError}
                                trainingCompleted={trainingCompleted}/>
                ) : (
                <div style={{flexDirection: 'row', height: '17.47%', marginBottom: '0.5rem',
                  justifyContent: 'flex-start', flexWrap: 'nowrap'}}>
                  <div style={styles.mainHeader}>Researcher Workbench</div>
                  <div style={homepageStyles.headingLinks}>Create a Workspace</div>
                </div>)
                ) :
              <Spinner dark={true} style={{width: '100%', marginTop: '5rem'}}/>}
          </div>

        </div>
        <div style={homepageStyles.quickRow}>
          <div style={homepageStyles.quickTourLabel}>Quick Tour & Videos</div>
            {quickTourResources.map(thumbnail => {
              return <img style={{maxHeight: '121px', width: '8rem', marginRight: '1rem'}} src={thumbnail.src}/>;
              {/*<Clickable onClick={this.setState({quickTour: true})}>*/}
                {/*<img src={thumbnail.src}/>*/}
              {/*</Clickable>*/}
          })}
        </div>
      </div>
      {/*{firstVisit &&*/}
        {/*<QuickTourReact closeFunction={() => this.setState({firstVisit: false})} />}*/}
    </React.Fragment>;
  }

}

@Component({
  template: '<div #root></div>'
})
export class HomepageComponent extends ReactWrapperBase {
  constructor() {
    super(Homepage, []);
  }
}


// @Component({
//   styleUrls: ['./component.css'],
//   templateUrl: './component.html',
// })
// export class HomepageComponent implements OnInit, OnDestroy {
//   private static pageId = 'homepage';
//   @ViewChild('myVideo') myVideo: any;
//   open = false;
//   src = '';
//   billingProjectInitialized = false;
//   billingProjectQuery: NodeJS.Timer;
//   firstSignIn: Date;
//   firstVisit = true;
//   newPageVisit: PageVisit = { page: HomepageComponent.pageId};
//   footerLinks = [
//     {
//       title: 'Working Within Researcher Workbench',
//       links: ['Researcher Workbench Mission',
//         'User interface components',
//         'What to do when things go wrong',
//         'Contributing to the Workbench']
//     },
//     {
//       title: 'Workspace',
//       links: ['Workspace interface components',
//         'User interface components',
//         'Collaborating with other researchers',
//         'Sharing and Publishing Workspaces']
//     },
//     {
//       title: 'Working with Notebooks',
//       links: ['Notebook interface components',
//         'Notebooks and data',
//         'Collaborating with other researchers',
//         'Sharing and Publishing Notebooks']
//     }];
//   quickTour: boolean;
//   accessTasksRemaining: boolean;
//   eraCommonsLinked: boolean;
//   eraCommonsError = '';
//   // TODO RW-1184; defaulting to true
//   trainingCompleted = true;
//
//   constructor(
//     private profileStorageService: ProfileStorageService,
//     private serverConfigService: ServerConfigService,
//     private route: ActivatedRoute,
//   ) {
//     // create bound methods to use as callbacks
//     this.closeQuickTour = this.closeQuickTour.bind(this);
//   }
//
//   ngOnInit(): void {
//     // this.validateNihToken();
//     //
//     // // TODO: combine these two profile() requests
//     // this.profileService.getMe().subscribe(profile => {
//     //   if (profile.pageVisits) {
//     //     this.firstVisit = !profile.pageVisits.some(v =>
//     //     v.page === HomepageComponent.pageId);
//     //   }
//     //
//     //   // Set Access Tasks flags
//     //   // TODO RW-1184 set trainingCompleted flag
//     //   this.eraCommonsLinked = !!profile.linkedNihUsername;
//     //
//     //   if (this.route.snapshot.queryParams.workbenchAccessTasks) {
//     //     // To reach the access tasks component from dev use /?workbenchAccessTasks=true
//     //     this.accessTasksRemaining = true;
//     //   } else {
//     //     this.serverConfigService.getConfig().subscribe((config) => {
//     //       if (environment.enableComplianceLockout && config.enforceRegistered) {
//     //         this.accessTasksRemaining = !this.eraCommonsLinked;
//     //       } else {
//     //         this.accessTasksRemaining = false;
//     //       }
//     //     });
//     //   }
//     // },
//     //   e => {},
//     //   () => {
//     //     if (this.firstVisit) {
//     //       this.quickTour = true;
//     //     }
//     //     this.profileService.updatePageVisits(this.newPageVisit).subscribe();
//     //   });
//     //
//     // this.profileStorageService.profile$.subscribe((profile) => {
//     //   // This will block workspace creation until the billing project is initialized
//     //   if (profile.freeTierBillingProjectStatus === BillingProjectStatus.Ready) {
//     //     this.billingProjectInitialized = true;
//     //   } else {
//     //     this.billingProjectQuery = setTimeout(() => {
//     //       this.profileStorageService.reload();
//     //     }, 10000);
//     //   }
//     // });
//
//   }
//
//   public closeQuickTour(): void {
//     this.quickTour = false;
//   }
//
//
//   // ngOnDestroy(): void {
//   //   clearTimeout(this.billingProjectQuery);
//   // }
//   //
//   // addWorkspace(): void {
//   //   navigate(['workspaces/build']);
//   // }
//   //
//   // navigateToProfile(): void {
//   //   navigate(['profile']);
//   // }
//   //
//   // listWorkspaces(): void {
//   //   navigate(['workspaces']);
//   // }
//   //
//   // get twoFactorBannerEnabled() {
//   //   if (this.firstSignIn == null) {
//   //     return false;
//   //   }
//   //   // Don't show the banner after 1 week as their account would
//   //   // have been disabled had they not enabled 2-factor auth.
//   //   return !(new Date().getTime() - this.firstSignIn.getTime() > 7 * 24 * 60 * 60 * 1000);
//   // }
// }
