import {Component} from '@angular/core';
import {navigate, queryParamsStore} from 'app/utils/navigation';

import * as fp from 'lodash/fp';
import * as React from 'react';

import {
  Button,
  CardButton,
  Clickable,
} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {Modal, ModalFooter} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {configApi, profileApi} from 'app/services/swagger-fetch-clients';
import {hasRegisteredAccessFetch, reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {QuickTourReact} from 'app/views/quick-tour-modal/component';
import {RecentWork} from 'app/views/recent-work/component';
import {RegistrationDashboard} from 'app/views/registration-dashboard/component';
import {
  BillingProjectStatus,
  Profile,
} from 'generated/fetch';

export const styles = reactStyles({
  mainHeader: {
    color: '#FFFFFF', fontSize: 28, fontWeight: 400,
    display: 'flex', letterSpacing: 'normal'
  },
  backgroundImage: {
    backgroundImage: 'url("/assets/images/AoU-HP-background.jpg")',
    backgroundRepeat: 'no-repeat', backgroundSize: 'cover', height: '100%',
    marginLeft: '-1rem', marginRight: '-0.6rem', display: 'flex',
    flexDirection: 'column', justifyContent: 'space-between'
  },
  singleCard: {
    width: '87.34%', minHeight: '18rem', maxHeight: '26rem',
    display: 'flex', flexDirection: 'column', borderRadius: '5px',
    backgroundColor: 'rgba(255, 255, 255, 0.15)',
    boxShadow: '0 0 2px 0 rgba(0, 0, 0, 0.12), 0 3px 2px 0 rgba(0, 0, 0, 0.12)',
    border: 'none', marginTop: '1rem'
  },
  contentWrapperLeft: {
    display: 'flex', flexDirection: 'column', paddingLeft: '3%', width: '40%'
  },
  contentWrapperRight: {
    display: 'flex', flexDirection: 'column', justifyContent: 'space-between', width: '60%'
  },
  quickRow: {
    display: 'flex', justifyContent: 'flex-start', maxHeight: '26rem',
    flexDirection: 'row', marginLeft: '4rem', padding: '1rem'
  },
  quickTourLabel: {
    fontSize: 28, lineHeight: '34px', color: '#fff', paddingRight: '2.3rem',
    marginTop: '2rem', width: '33%'
  },
  footer: {
    height: '300px', width: '100%', backgroundColor: '#262262',
    boxShadow: '0 0 2px 0 rgba(0, 0, 0, 0.12), 0 3px 2px 0 rgba(0, 0, 0, 0.12)',
    marginTop: '2%',
  },
  footerInner: {
    display: 'flex', flexDirection: 'column', marginLeft: '5%', marginRight: '5%',
  },
  footerTitle: {
    height: '34px', opacity: 0.87, color: '#fff', fontSize: 28,
    fontWeight: 600, lineHeight: '34px', width: '87.34%', marginTop: '1.4rem'
  },
  footerText: {
    height: '176px', opacity: 0.87, color: '#83C3EC', fontSize: '16px',
    fontWeight: 400, lineHeight: '30px', display: 'flex', width: '100%',
    flexDirection: 'column', flexWrap: 'nowrap', overflowY: 'auto'
  },
  linksBlock: {
    display: 'flex', marginBottom: '1.2rem', marginLeft: '1.4rem',
    flexDirection: 'column', flexShrink: 1, minWidth: 0
  },
  bottomBanner: {
    borderTopColor: '#5DAEE1', borderStyle: 'solid', borderWidth: '0.3rem 0 0 0',
    width: '100%', display: 'flex', backgroundColor: '#483F4B', height: '5rem',
  },
  logo: {
    top: '1rem', left: '6.25rem', height: '3.5rem', width: '7rem',
    position: 'relative', lineHeight: '85px'
  },
  bottomLinks: {
    color: '#9B9B9B', fontSize: '0.7rem', height: '1rem', left: '5.5rem',
    top: '2rem', marginLeft: '2.5rem', position: 'relative', fontWeight: 400
  }
});

export const Homepage = withUserProfile()(class extends React.Component<
  { profileState: { profile: Profile, reload: Function } },
  { accessTasksLoaded: boolean,
    accessTasksRemaining: boolean,
    betaAccessGranted: boolean,
    billingProjectInitialized: boolean,
    eraCommonsError: string,
    eraCommonsLinked: boolean,
    firstVisit: boolean,
    firstVisitTraining: boolean,
    quickTour: boolean,
    trainingCompleted: boolean,
    twoFactorAuthCompleted: boolean,
    videoOpen: boolean,
    videoLink: string
  }> {
  private pageId = 'homepage';
  private timer: NodeJS.Timer;

  constructor(props: any) {
    super(props);
    this.state = {
      accessTasksLoaded: false,
      accessTasksRemaining: undefined,
      betaAccessGranted: undefined,
      billingProjectInitialized: false,
      eraCommonsError: '',
      eraCommonsLinked: undefined,
      firstVisit: undefined,
      firstVisitTraining: true,
      quickTour: false,
      trainingCompleted: undefined,
      twoFactorAuthCompleted: undefined,
      videoOpen: false,
      videoLink: '',
    };
  }

  componentDidMount() {
    this.validateNihToken();
    this.callProfile();
    this.checkBillingProjectStatus();
  }

  componentDidUpdate(prevProps) {
    const {profileState: {profile}} = this.props;
    if (!this.state.billingProjectInitialized) {
      this.checkBillingProjectStatus();
    }
    if (!fp.isEqual(prevProps.profileState.profile, profile)) {
      this.callProfile();
    }
  }

  componentWillUnmount() {
    clearTimeout(this.timer);
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

  setFirstVisit() {
    this.setState({firstVisit: true});
    profileApi().updatePageVisits({ page: this.pageId});
  }

  async callProfile() {
    const {profileState: {profile, reload}} = this.props;

    if (fp.isEmpty(profile)) {
      setTimeout(() => {
        reload();
      }, 10000);
    } else {

      if (profile.pageVisits) {
        if (!profile.pageVisits.some(v => v.page === this.pageId)) {
          this.setFirstVisit();
        }
        if (profile.pageVisits.some(v => v.page === 'moodle')) {
          this.setState({firstVisitTraining: false});
        }
      } else {
        // page visits is null; is first visit
        this.setFirstVisit();
      }
      try {
        this.setState({
          eraCommonsLinked: !!profile.eraCommonsCompletionTime || !!profile.eraCommonsBypassTime
        });
      } catch (ex) {
        this.setState({eraCommonsLinked: false});
        console.error('error fetching era commons linking status');
      }

      try {
        const result = await profileApi().syncComplianceTrainingStatus();
        this.setState({trainingCompleted: !!result.complianceTrainingCompletionTime
              || !!result.complianceTrainingBypassTime});
      } catch (ex) {
        this.setState({trainingCompleted: false});
        console.error('error fetching moodle training status');
      }

      try {
        const result = await profileApi().syncTwoFactorAuthStatus();
        this.setState({twoFactorAuthCompleted: !!result.twoFactorAuthCompletionTime ||
            !!profile.twoFactorAuthBypassTime});
      } catch (ex) {
        this.setState({twoFactorAuthCompleted: false});
        console.error('error fetching two factor auth status');
      }

      this.setState({betaAccessGranted: !!profile.betaAccessBypassTime});

      const {workbenchAccessTasks} = queryParamsStore.getValue();
      if (workbenchAccessTasks) {
        this.setState({accessTasksRemaining: true, accessTasksLoaded: true});
      } else {
        try {
          const config = await configApi().getConfig();
          if (config.enforceRegistered) {
            this.setState({
              accessTasksRemaining: !hasRegisteredAccessFetch(profile.dataAccessLevel),
              accessTasksLoaded: true
            });
          } else {
            this.setState({accessTasksRemaining: false, accessTasksLoaded: true});
          }
        } catch (ex) {
          console.error('error fetching config: ' + ex.toString());
        }
      }
    }

    this.setState(
        {quickTour: this.state.firstVisit && this.state.accessTasksRemaining === false});
  }

  checkBillingProjectStatus() {
    const {profileState: {profile, reload}} = this.props;
    if (profile.freeTierBillingProjectStatus === BillingProjectStatus.Ready) {
      this.setState({billingProjectInitialized: true});
    } else {
      this.timer = setTimeout(() => {
        reload();
      }, 10000);
    }
  }

  openVideo(videoLink: string): void {
    this.setState({videoOpen: true, videoLink: videoLink});
  }


  render() {
    const {billingProjectInitialized, betaAccessGranted, videoOpen, accessTasksLoaded,
        accessTasksRemaining, eraCommonsLinked, eraCommonsError, firstVisitTraining,
        trainingCompleted, quickTour, videoLink, twoFactorAuthCompleted} = this.state;
    const quickTourResources = [
      {
        src: '/assets/images/QT-thumbnail.svg',
        onClick: () => this.setState({quickTour: true})
      }, {
        src: '/assets/images/cohorts-thumbnail.png',
        onClick: () => this.openVideo('/assets/videos/Workbench Tutorial - Cohorts.mp4')
      }, {
        src: '/assets/images/notebooks-thumbnail.png',
        onClick: () => this.openVideo('/assets/videos/Workbench Tutorial - Notebooks.mp4')
      }
    ];
    const footerLinks = [{
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

    return <React.Fragment>
      <div style={styles.backgroundImage}>
        <div style={{display: 'flex', justifyContent: 'center'}}>
          <div style={styles.singleCard}>
            {accessTasksLoaded ?
              (accessTasksRemaining ?
                (<RegistrationDashboard eraCommonsLinked={eraCommonsLinked}
                                        eraCommonsError={eraCommonsError}
                                        trainingCompleted={trainingCompleted}
                                        firstVisitTraining={firstVisitTraining}
                                        betaAccessGranted={betaAccessGranted}
                                        twoFactorAuthCompleted={twoFactorAuthCompleted}/>
                ) : (
                  <div style={{display: 'flex', flexDirection: 'row', paddingTop: '2rem'}}>
                    <div style={styles.contentWrapperLeft}>
                      <div style={styles.mainHeader}>Researcher Workbench</div>
                      <TooltipTrigger content={<div>Your Firecloud billing project is still being
                        initialized. Workspace creation will be available in a few minutes.</div>}
                                      disabled={billingProjectInitialized}>
                        <CardButton disabled={!billingProjectInitialized}
                                    onClick={() => navigate(['workspaces/build'])}
                                    style={{margin: '1.9rem 106px 0 3%'}}>
                          Create a <br/> New Workspace
                          <ClrIcon shape='plus-circle' style={{height: '32px', width: '32px'}}/>
                        </CardButton>
                      </TooltipTrigger>
                    </div>
                    <div style={styles.contentWrapperRight}>
                      <a onClick={() => navigate(['workspaces'])}
                         style={{fontSize: '14px', color: '#FFFFFF'}}>
                        See All Workspaces</a>
                      <div style={{marginRight: '3%', display: 'flex', flexDirection: 'column'}}>
                        <div style={{color: '#fff', height: '1.9rem'}}>
                          <div style={{marginTop: '.5rem'}}>Your Last Accessed Items</div>
                        </div>
                        <RecentWork dark={true} cardMarginTop='0'/>
                      </div>
                    </div>
                  </div>)
                ) :
              <Spinner dark={true} style={{width: '100%', marginTop: '5rem'}}/>}
          </div>
        </div>
        <div>
          <div style={styles.quickRow}>
            <div style={styles.quickTourLabel}>Quick Tour & Videos</div>
            {quickTourResources.map((thumbnail, i) => {
              return <React.Fragment key={i}>
                <Clickable onClick={thumbnail.onClick}
                           data-test-id={'quick-tour-resource-' + i}>
                  <img style={{maxHeight: '121px', width: '8rem', marginRight: '1rem'}}
                       src={thumbnail.src}/>
                </Clickable>
              </React.Fragment>;
            })}
          </div>
          <div>
            <div style={styles.footer}>
              <div style={styles.footerInner}>
                <div style={styles.footerTitle}>
                  How to Use the All of Us Researcher Workbench</div>
                <div style={{display: 'flex', justifyContent: 'flex-end'}}>
                  <TooltipTrigger content='Coming Soon' side='left'>
                    <a href='#' style={{color: '#fff'}}>See all documentation</a>
                  </TooltipTrigger>
                </div>
                <div style={{display: 'flex', flexDirection: 'row',
                  width: '87.34%', justifyContent: 'space-between'}}>
                  {footerLinks.map((col, i) => {
                    return <React.Fragment key={i}>
                      <div style={styles.linksBlock}>
                        <div style={styles.footerText}>
                          <div style={{color: 'white', marginTop: '2%'}}>{col.title}</div>
                          <ul style={{color: '#83C3EC'}}>
                            {col.links.map((link, ii) => {
                              return <li key={ii}>
                                <a href='#' style={{color: '#83C3EC'}}>{link}</a>
                              </li>;
                            } )}
                          </ul>
                        </div>
                      </div>
                    </React.Fragment>;
                  })}
                </div>
              </div>
            </div>
            <div style={styles.bottomBanner}>
              <div style={styles.logo}>
                <img src='/assets/images/all-of-us-logo-footer.svg'/>
              </div>
              <div style={styles.bottomLinks}>Privacy Policy</div>
              <div style={styles.bottomLinks}>Terms of Service</div>
            </div>
          </div>
        </div>

      </div>

      {quickTour &&
        <QuickTourReact closeFunction={() => this.setState({quickTour: false})} />}
      {videoOpen && <Modal width={900}>
        <video width='100%' controls autoPlay>
          <source src={videoLink} type='video/mp4'/>
        </video>
        <ModalFooter>
          <Button type='secondary'
                  onClick={() => this.setState({videoOpen: false})}>
            Close</Button>
        </ModalFooter>
      </Modal>}
    </React.Fragment>;
  }

});

@Component({
  template: '<div #root style="height: 100%"></div>'
})
export class HomepageComponent extends ReactWrapperBase {
  constructor() {
    super(Homepage, []);
  }
}
