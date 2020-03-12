import {Component} from '@angular/core';
import {navigate, queryParamsStore, serverConfigStore} from 'app/utils/navigation';

import * as fp from 'lodash/fp';
import * as React from 'react';

import {
  Clickable, StyledAnchorTag,
} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {CustomBulletList, CustomBulletListItem} from 'app/components/custom-bullet-list';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Header, SemiBoldHeader, SmallHeader} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {Modal} from 'app/components/modals';
import {Spinner} from 'app/components/spinners';
import {QuickTourReact} from 'app/pages/homepage/quick-tour-modal';
import {RecentResources} from 'app/pages/homepage/recent-resources';
import {RecentWorkspaces} from 'app/pages/homepage/recent-workspaces';
import {getRegistrationTasksMap, RegistrationDashboard} from 'app/pages/homepage/registration-dashboard';
import {profileApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {addOpacity} from 'app/styles/colors';
import {hasRegisteredAccessFetch, reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {
  Profile,
} from 'generated/fetch';

export const styles = reactStyles({
  bottomBanner: {
    width: '100%', display: 'flex', backgroundColor: colors.primary,
    paddingLeft: '3.5rem', alignItems: 'center', marginTop: '2rem'
  },
  bottomLinks: {
    color: colors.white, fontSize: '.5rem', height: '1rem',
    marginLeft: '2.5rem', fontWeight: 400
  },
  contentWrapperLeft: {
    paddingLeft: '3%', width: '40%'
  },
  contentWrapperRight: {
    justifyContent: 'space-between', width: '60%'
  },
  fadeBox: {
    margin: '1rem 0 0 3%', width: '95%', padding: '0 0.1rem'
  },
  logo: {
    height: '3.5rem', width: '7rem', lineHeight: '85px'
  },
  mainHeader: {
    color: colors.primary, fontSize: 28, fontWeight: 400, letterSpacing: 'normal'
  },
  pageWrapper: {
    marginLeft: '-1rem', marginRight: '-0.6rem', justifyContent: 'space-between', fontSize: '1.2em'
  },
  quickTourCardsRow: {
    justifyContent: 'flex-start', maxHeight: '26rem', marginTop: '0.5rem'
  },
  quickTourLabel: {
    fontSize: 22, lineHeight: '34px', color: colors.primary, paddingRight: '2.3rem',
    fontWeight: 600, marginTop: '1rem', width: '33%'
  },
  welcomeMessageIcon: {
    height: '2.25rem', width: '2.75rem'
  },
});

export const Homepage = withUserProfile()(class extends React.Component<
  { profileState: { profile: Profile, reload: Function } },
  { accessTasksLoaded: boolean,
    accessTasksRemaining: boolean,
    betaAccessGranted: boolean,
    dataUseAgreementCompleted: boolean,
    eraCommonsError: string,
    eraCommonsLinked: boolean,
    firstVisit: boolean,
    firstVisitTraining: boolean,
    quickTour: boolean,
    trainingCompleted: boolean,
    twoFactorAuthCompleted: boolean,
    videoOpen: boolean,
    videoLink: string,
    userHasWorkspaces: boolean | null
  }> {
  private pageId = 'homepage';
  private timer: NodeJS.Timer;

  constructor(props: any) {
    super(props);
    this.state = {
      accessTasksLoaded: false,
      accessTasksRemaining: undefined,
      betaAccessGranted: undefined,
      dataUseAgreementCompleted: undefined,
      eraCommonsError: '',
      eraCommonsLinked: undefined,
      firstVisit: undefined,
      firstVisitTraining: true,
      quickTour: false,
      trainingCompleted: undefined,
      twoFactorAuthCompleted: undefined,
      videoOpen: false,
      videoLink: '',
      userHasWorkspaces: null,
    };
  }

  componentDidMount() {
    this.checkWorkspaces();
    this.validateNihToken();
    this.callProfile();
  }

  componentDidUpdate(prevProps) {
    const {profileState: {profile}} = this.props;
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

  async syncCompliance() {
    const complianceStatus = profileApi().syncComplianceTrainingStatus().then(result => {
      this.setState({
        trainingCompleted: !!(getRegistrationTasksMap()['complianceTraining']
          .completionTimestamp(result))
      });
    }).catch(err => {
      this.setState({trainingCompleted: false});
      console.error('error fetching moodle training status:', err);
    });
    const twoFactorAuthStatus = profileApi().syncTwoFactorAuthStatus().then(result => {
      this.setState({
        twoFactorAuthCompleted: !!(getRegistrationTasksMap()['twoFactorAuth'].completionTimestamp(result))
      });
    }).catch(err => {
      this.setState({twoFactorAuthCompleted: false});
      console.error('error fetching two factor auth status:', err);
    });
    return Promise.all([complianceStatus, twoFactorAuthStatus]);
  }

  async callProfile() {
    const {profileState: {profile, reload}} = this.props;

    if (fp.isEmpty(profile)) {
      setTimeout(() => {
        reload();
      }, 10000);
    } else {
      if (!profile.betaAccessRequestTime) {
        profileApi().requestBetaAccess();
      }
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

      this.setState({
        eraCommonsLinked: !!(getRegistrationTasksMap()['eraCommons'].completionTimestamp(profile)),
        dataUseAgreementCompleted: (serverConfigStore.getValue().enableDataUseAgreement ?
          (() => !!(getRegistrationTasksMap()['dataUseAgreement']
            .completionTimestamp(profile)))() : true)
      });
      this.setState({betaAccessGranted: !!profile.betaAccessBypassTime});

      const {workbenchAccessTasks} = queryParamsStore.getValue();
      const hasRegisteredAccess = hasRegisteredAccessFetch(profile.dataAccessLevel);
      if (!hasRegisteredAccess || workbenchAccessTasks) {
        await this.syncCompliance();
      }
      if (workbenchAccessTasks) {
        this.setState({accessTasksRemaining: true, accessTasksLoaded: true});
      } else {
        this.setState({
          accessTasksRemaining: !hasRegisteredAccess,
          accessTasksLoaded: true
        });
      }
    }
    this.setState((state, props) => ({
      quickTour: state.firstVisit && state.accessTasksRemaining === false
    }));
  }

  async checkWorkspaces() {
    workspacesApi().getWorkspaces().then(response => {
      this.setState({
        userHasWorkspaces: response.items.length > 0
      });
    });
  }

  openVideo(videoLink: string): void {
    this.setState({videoOpen: true, videoLink: videoLink});
  }


  render() {
    const {betaAccessGranted, videoOpen, accessTasksLoaded, accessTasksRemaining,
      eraCommonsLinked, eraCommonsError, firstVisitTraining, trainingCompleted, quickTour,
      videoLink, twoFactorAuthCompleted, dataUseAgreementCompleted
    } = this.state;

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

    return <React.Fragment>
      <FlexColumn style={styles.pageWrapper}>
        <FlexRow style={{marginLeft: '3%'}}>
          <FlexColumn style={{width: '50%'}}>
            <FlexRow>
              <FlexColumn>
                <Header style={{fontWeight: 500, color: colors.secondary, fontSize: '0.92rem'}}>
                  Welcome to the</Header>
                <Header style={{textTransform: 'uppercase', marginTop: '0.2rem'}}>
                  Researcher Workbench</Header>
              </FlexColumn>
              <FlexRow style={{alignItems: 'flex-end', marginLeft: '1rem'}}>
                <img style={styles.welcomeMessageIcon} src='/assets/images/workspace-icon.svg'/>
                <img style={styles.welcomeMessageIcon} src='/assets/images/cohort-icon.svg'/>
                <img style={styles.welcomeMessageIcon} src='/assets/images/analysis-icon.svg'/>
              </FlexRow>
            </FlexRow>
            <SmallHeader style={{color: colors.primary, marginTop: '0.25rem'}}>
              The secure platform to analyze <i>All of Us</i> data</SmallHeader>
          </FlexColumn>
          <div></div>
        </FlexRow>
        <FadeBox style={styles.fadeBox}>
          {/* The elements inside this fadeBox will be changed as part of ongoing
          homepage redesign work*/}
          <FlexColumn style={{justifyContent: 'flex-start'}}>
              {accessTasksLoaded ?
                (accessTasksRemaining ?
                    (<RegistrationDashboard eraCommonsLinked={eraCommonsLinked}
                                            eraCommonsError={eraCommonsError}
                                            trainingCompleted={trainingCompleted}
                                            firstVisitTraining={firstVisitTraining}
                                            betaAccessGranted={betaAccessGranted}
                                            twoFactorAuthCompleted={twoFactorAuthCompleted}
                                          dataUseAgreementCompleted={dataUseAgreementCompleted}/>
                    ) : (
                        <React.Fragment>
                          <FlexColumn>
                            <FlexRow style={{justifyContent: 'space-between', alignItems: 'center'}}>
                              <FlexRow style={{alignItems: 'center'}}>
                                <SemiBoldHeader style={{marginTop: '0px'}}>Workspaces</SemiBoldHeader>
                                <ClrIcon
                                  shape='plus-circle'
                                  size={30}
                                  className={'is-solid'}
                                  style={{color: colors.accent, marginLeft: '1rem', cursor: 'pointer'}}
                                  onClick={() => {
                                    AnalyticsTracker.Workspaces.OpenCreatePage();
                                    navigate(['workspaces/build']);
                                  }}
                                />
                              </FlexRow>
                              <span
                                style={{alignSelf: 'flex-end', color: colors.accent, cursor: 'pointer'}}
                                onClick={() => navigate(['workspaces'])}
                              >
                                See all Workspaces
                              </span>
                            </FlexRow>
                            <RecentWorkspaces />
                          </FlexColumn>
                          <FlexColumn>
                            {this.state.userHasWorkspaces !== null &&

                              <React.Fragment>
                                {this.state.userHasWorkspaces ?
                                <RecentResources/> :

                                <div style={{
                                  backgroundColor: addOpacity(colors.primary, .1).toString(),
                                  color: colors.primary,
                                  borderRadius: 10,
                                  margin: '2em 0em'}}>
                                  <div style={{margin: '1em 2em'}}>
                                    <h2 style={{fontWeight: 600, marginTop: 0}}>Here are some tips to get you started:</h2>
                                    <CustomBulletList>
                                      <CustomBulletListItem bullet='→'>
                                        Create a <StyledAnchorTag href='https://support.google.com/chrome/answer/2364824'>Chrome
                                        Profile</StyledAnchorTag> with your <i>All of Us</i> Researcher Workbench Google account.
                                        This will keep your Workbench browser sessions isolated from your other Google accounts.
                                      </CustomBulletListItem>
                                      <CustomBulletListItem bullet='→'>
                                        Check out <a onClick={() => navigate(['library'])}> Featured Workspaces </a>
                                        from the left hand panel to browse through example workspaces.
                                      </CustomBulletListItem>
                                      <CustomBulletListItem bullet='→'>
                                        Browse through our <StyledAnchorTag href='https://aousupporthelp.zendesk.com/hc/en-us'>support
                                        materials</StyledAnchorTag> and forum topics.
                                      </CustomBulletListItem>
                                    </CustomBulletList>
                                  </div>
                                </div>}
                              </React.Fragment>
                            }
                          </FlexColumn>
                        </React.Fragment>
                      )
                ) :
                <Spinner dark={true} style={{width: '100%', marginTop: '5rem'}}/>}
          </FlexColumn>
        </FadeBox>
        <div style={{backgroundColor: addOpacity(colors.light, .4).toString()}}>
          <FlexColumn style={{marginLeft: '3%'}}>
            <div style={styles.quickTourLabel}>Quick Tour and Videos</div>
            <FlexRow style={styles.quickTourCardsRow}>
              {quickTourResources.map((thumbnail, i) => {
                return <React.Fragment key={i}>
                  <Clickable onClick={thumbnail.onClick}
                             data-test-id={'quick-tour-resource-' + i}>
                    <img style={{width: '11rem', marginRight: '0.5rem'}}
                         src={thumbnail.src}/>
                  </Clickable>
                </React.Fragment>;
              })}
            </FlexRow>
          </FlexColumn>
          <div style={styles.bottomBanner}>
            <div style={styles.logo}>
              <img src='/assets/images/all-of-us-logo-footer.svg'/>
            </div>
            <div style={styles.bottomLinks}>Copyright ©2018</div>
            <div style={styles.bottomLinks}>Privacy Policy</div>
            <div style={styles.bottomLinks}>Terms of Service</div>
          </div>
        </div>
      </FlexColumn>
      {quickTour &&
      <QuickTourReact closeFunction={() => this.setState({quickTour: false})} />}
      {videoOpen && <Modal width={900}>
        <div style={{display: 'flex'}}>
          <div style={{flexGrow: 1}}></div>
          <Clickable onClick={() => this.setState({videoOpen: false})}>
            <ClrIcon
              shape='times'
              size='24'
              style={{color: colors.accent, marginBottom: 17}}
            />
          </Clickable>
        </div>
        <video width='100%' controls autoPlay>
          <source src={videoLink} type='video/mp4'/>
        </video>
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
