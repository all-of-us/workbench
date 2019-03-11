import {Component} from '@angular/core';
import {navigate, queryParamsStore} from 'app/utils/navigation';
import {environment} from 'environments/environment';

import * as fp from 'lodash/fp';
import * as React from 'react';

import {AlertClose, AlertWarning} from 'app/components/alert';
import {
  Button,
  CardButton,
  Clickable,
  styles as buttonStyles,
} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {Modal, ModalFooter} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {configApi, profileApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withStyle, withUserProfile} from 'app/utils';
import {QuickTourReact} from 'app/views/quick-tour-modal/component';
import {RecentWork} from 'app/views/recent-work/component';
import {
  BillingProjectStatus,
  Profile,
} from 'generated/fetch';


const styles = reactStyles({
  mainHeader: {
    color: '#FFFFFF', fontSize: 28, fontWeight: 400,
    display: 'flex', letterSpacing: 'normal'
  },
  minorHeader: {
    color: '#FFFFFF', fontSize: 18, fontWeight: 600, display: 'flex',
    marginTop: '1rem', lineHeight: '24px'
  },
  text: {
    color: '#FFFFFF', fontSize: 16, lineHeight: '22px', fontWeight: 150,
    marginTop: '3%'
  },
  infoBox: {
    padding: '1rem', backgroundColor: '#FFFFFF', borderRadius: '5px', display: 'flex',
    flexDirection: 'row', justifyContent: 'space-between'
  },
  infoBoxHeader: {
    fontSize: 16, color: '#262262', fontWeight: 600
  },
  infoBoxBody: {
    color: '#000', lineHeight: '18px'
  },
  infoBoxButton: {
    color: '#FFFFFF', height: '49px', borderRadius: '5px', marginLeft: '1rem',
    maxWidth: '20rem'
  },
  error: {
    fontWeight: 300, color: '#DC5030', fontSize: '14px', marginTop: '.3rem',
    backgroundColor: '#FFF1F0', borderRadius: '5px', padding: '.3rem',
  }
});

export const Error = withStyle(styles.error)('div');

const AccountLinkingButton: React.FunctionComponent<{
  failed: boolean, completed: boolean, failedText: string,
  completedText: string, defaultText: string, onClick: Function
}> = ({failed, completed, defaultText, completedText, failedText, onClick}) => {
  const dataTestId = fp.lowerCase(defaultText)
      .replace(/\s/g, '-');
  if (failed) {
    return <Clickable style={{...buttonStyles.base,
      ...styles.infoBoxButton, backgroundColor: '#f27376'}}
                      disabled={true} data-test-id={dataTestId}>
      <ClrIcon shape='exclamation-triangle'/>{failedText}
    </Clickable>;
  } else if (completed) {
    return <Clickable style={{...buttonStyles.base,
      ...styles.infoBoxButton, backgroundColor: '#8BC990'}}
                      disabled={true} data-test-id={dataTestId}>
      <ClrIcon shape='check'/>{completedText}
    </Clickable>;
  } else {
    return <Clickable style={{...buttonStyles.base,
      ...styles.infoBoxButton, backgroundColor: '#2691D0'}}
                      onClick={onClick}
                      data-test-id={dataTestId}>
      {defaultText}
    </Clickable>;
  }
};

export interface WorkbenchAccessTasksProps {
  eraCommonsLinked: boolean;
  eraCommonsError: string;
  trainingCompleted: boolean;
  firstVisitTraining: boolean;
}

export class WorkbenchAccessTasks extends
    React.Component<WorkbenchAccessTasksProps, {trainingWarningOpen: boolean}> {

  constructor(props: WorkbenchAccessTasksProps) {
    super(props);
    this.state = {trainingWarningOpen: !props.firstVisitTraining};
  }

  static redirectToNiH(): void {
    const url = environment.shibbolethUrl + '/link-nih-account?redirect-url=' +
        encodeURIComponent(
          window.location.origin.toString() + '/nih-callback?token={token}');
    window.location.assign(url);
  }

  static async redirectToTraining() {
    await profileApi().updatePageVisits({page: 'moodle'});
    window.location.assign(environment.trainingUrl + '/static/data-researcher.html?saml=on');
  }

  render() {
    const {trainingWarningOpen} = this.state;
    const {eraCommonsLinked, eraCommonsError, trainingCompleted} = this.props;
    return <div style={{display: 'flex', flexDirection: 'row'}} data-test-id='access-tasks'>
        <div style={{display: 'flex', flexDirection: 'column', width: '50%', padding: '3% 0 0 3%'}}>
          <div style={styles.mainHeader}>Researcher Workbench</div>
          <div style={{marginLeft: '1rem', flexDirection: 'column'}}>
            <div style={styles.minorHeader}>In order to get access to data and tools
              please complete the following:</div>
            <div style={styles.text}>Please login to your eRA Commons account and complete
              the online training courses in order to gain full access to the Researcher
              Workbench data and tools.</div>
          </div>
        </div>
        <div style={{flexDirection: 'column', width: '50%', padding: '1rem'}}>
          <div style={{...styles.infoBox, flexDirection: 'column'}}>
            <div style={{display: 'flex', flexDirection: 'row', justifyContent: 'space-between'}}>
              <div style={{flexDirection: 'column', width: '70%'}}>
                <div style={styles.infoBoxHeader}>Login to eRA Commons</div>
                <div style={styles.infoBoxBody}>Clicking the login link will bring you
                  to the eRA Commons Portal and redirect you back to the
                  Workbench once you are logged in.</div>
              </div>
              <AccountLinkingButton failed={false}
                                    completed={eraCommonsLinked}
                                    defaultText='Login'
                                    completedText='Linked'
                                    failedText='Error Linking Accounts'
                                    onClick={WorkbenchAccessTasks.redirectToNiH}/>
            </div>
            {eraCommonsError && <Error data-test-id='era-commons-error'>
              <ClrIcon shape='exclamation-triangle' class='is-solid'/>
              Error Linking NIH Username: {eraCommonsError} Please try again!
            </Error>}
          </div>
          <div style={{...styles.infoBox, marginTop: '0.7rem'}}>
            <div style={{flexDirection: 'column', width: '65%'}}>
              <div style={styles.infoBoxHeader}>Complete Online Training</div>
              <div style={styles.infoBoxBody}>Clicking the training link will bring you to
                the All of Us Compliance Training Portal, which will show you any
                outstanding training material to be completed.</div>
            </div>
            <AccountLinkingButton failed={false}
                                  completed={trainingCompleted}
                                  defaultText={'Complete Training'}
                                  completedText='Completed'
                                  failedText=''
                                  onClick={WorkbenchAccessTasks.redirectToTraining}/>
          </div>
          {trainingWarningOpen && !trainingCompleted &&
          <AlertWarning>
            <ClrIcon shape='exclamation-triangle' class='is-solid'
                     style={{width: '8%', height: '40px', marginRight: '.1rem'}}/>
            <div>It may take several minutes for Moodle to update your Online Training
            status once you have completed compliance training.</div>
            <AlertClose onClick={() => this.setState({trainingWarningOpen: false})}/>
          </AlertWarning>}
        </div>
      </div>;
  }
}

export const homepageStyles = reactStyles({
  backgroundImage: {
    backgroundImage: 'url("/assets/images/AoU-HP-background.jpg")',
    backgroundRepeat: 'no-repeat', backgroundSize: 'cover', height: '100%',
    marginLeft: '-1rem', marginRight: '-0.6rem', display: 'flex',
    flexDirection: 'column', justifyContent: 'space-between'
  },
  singleCard: {
    height: '33.4%', width: '87.34%', minHeight: '18rem', maxHeight: '26rem',
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
    flexDirection: 'column', flexWrap: 'nowrap', overflowY: 'scroll'
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
    billingProjectInitialized: boolean,
    eraCommonsError: string,
    eraCommonsLinked: boolean,
    firstVisit: boolean,
    firstVisitTraining: boolean,
    quickTour: boolean,
    trainingCompleted: boolean,
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
      billingProjectInitialized: false,
      eraCommonsError: '',
      eraCommonsLinked: undefined,
      firstVisit: undefined,
      firstVisitTraining: true,
      quickTour: false,
      trainingCompleted: undefined,
      videoOpen: false,
      videoLink: '',
    };
  }

  get accessTasksRemaining(): boolean {
    return !(this.state.eraCommonsLinked && this.state.trainingCompleted);
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
        this.setState({eraCommonsLinked: !!profile.eraCommonsLinkedNihUsername});
      } catch (ex) {
        this.setState({eraCommonsLinked: false});
        console.error('error fetching era commons linking status');
      }

      try {
        const syncTrainingStatus = await profileApi().syncTrainingStatus();
        this.setState({trainingCompleted: !!syncTrainingStatus.trainingCompletionTime});
      } catch (ex) {
        this.setState({trainingCompleted: false});
        console.error('error fetching moodle training status');
      }

      const {workbenchAccessTasks} = queryParamsStore.getValue();
      if (workbenchAccessTasks) {
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
    const {billingProjectInitialized, videoOpen, accessTasksLoaded,
        accessTasksRemaining, eraCommonsLinked, eraCommonsError, firstVisitTraining,
        trainingCompleted, quickTour, videoLink} = this.state;
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
      <div style={homepageStyles.backgroundImage}>
        <div style={{display: 'flex', justifyContent: 'center'}}>
          <div style={homepageStyles.singleCard}>
            {accessTasksLoaded ?
              (accessTasksRemaining ?
                (<WorkbenchAccessTasks eraCommonsLinked={eraCommonsLinked}
                                       eraCommonsError={eraCommonsError}
                                       trainingCompleted={trainingCompleted}
                                       firstVisitTraining={firstVisitTraining}/>
                ) : (
                  <div style={{display: 'flex', flexDirection: 'row', paddingTop: '2rem'}}>
                    <div style={homepageStyles.contentWrapperLeft}>
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
                    <div style={homepageStyles.contentWrapperRight}>
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
          <div style={homepageStyles.quickRow}>
            <div style={homepageStyles.quickTourLabel}>Quick Tour & Videos</div>
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
            <div style={homepageStyles.footer}>
              <div style={homepageStyles.footerInner}>
                <div style={homepageStyles.footerTitle}>
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
                      <div style={homepageStyles.linksBlock}>
                        <div style={homepageStyles.footerText}>
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
            <div style={homepageStyles.bottomBanner}>
              <div style={homepageStyles.logo}>
                <img src='/assets/images/all-of-us-logo-footer.svg'/>
              </div>
              <div style={homepageStyles.bottomLinks}>Privacy Policy</div>
              <div style={homepageStyles.bottomLinks}>Terms of Service</div>
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
