import * as fp from 'lodash/fp';
import * as React from 'react';

import {navigate, queryParamsStore} from 'app/utils/navigation';

import {
  StyledAnchorTag,
} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Header, SemiBoldHeader, SmallHeader} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {CustomBulletList, CustomBulletListItem} from 'app/components/lists';
import {Spinner} from 'app/components/spinners';
import {AoU} from 'app/components/text-wrappers';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {RecentResources} from 'app/pages/homepage/recent-resources';
import {RecentWorkspaces} from 'app/pages/homepage/recent-workspaces';
import {getRegistrationTasksMap, RegistrationDashboard} from 'app/pages/homepage/registration-dashboard';
import {profileApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {addOpacity} from 'app/styles/colors';
import {reactStyles, withUserProfile} from 'app/utils';
import {hasRegisteredAccess} from 'app/utils/access-tiers';
import {AnalyticsTracker} from 'app/utils/analytics';
import {buildRasRedirectUrl} from 'app/utils/ras';
import {fetchWithGlobalErrorHandler} from 'app/utils/retry';
import {serverConfigStore} from 'app/utils/stores';
import {supportUrls} from 'app/utils/zendesk';
import {Profile, WorkspaceResponseListResponse} from 'generated/fetch';
import {QuickTourAndVideos} from './quick-tour-and-videos';

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
  welcomeMessageIcon: {
    height: '2.25rem', width: '2.75rem'
  },
});

interface Props extends WithSpinnerOverlayProps {
  profileState: {
    profile: Profile,
    reload: Function
  };
}

interface State {
  accessTasksLoaded: boolean;
  accessTasksRemaining: boolean;
  dataUserCodeOfConductCompleted: boolean;
  eraCommonsError: string;
  eraCommonsLinked: boolean;
  eraCommonsLoading: boolean;
  rasLoginGovLinkError: string;
  rasLoginGovLinked: boolean;
  rasLoginGovLoading: boolean;
  firstVisit: boolean;
  firstVisitTraining: boolean;
  quickTour: boolean;
  trainingCompleted: boolean;
  twoFactorAuthCompleted: boolean;
  userWorkspacesResponse: WorkspaceResponseListResponse;
}

export const Homepage = withUserProfile()(class extends React.Component<Props, State> {
  private pageId = 'homepage';
  private timer: NodeJS.Timer;

  constructor(props: Props) {
    super(props);
    this.state = {
      accessTasksLoaded: false,
      accessTasksRemaining: undefined,
      dataUserCodeOfConductCompleted: undefined,
      eraCommonsError: '',
      eraCommonsLinked: undefined,
      eraCommonsLoading: false,
      rasLoginGovLinkError: '',
      rasLoginGovLinked: undefined,
      rasLoginGovLoading: false,
      firstVisit: undefined,
      firstVisitTraining: true,
      quickTour: false,
      trainingCompleted: undefined,
      twoFactorAuthCompleted: undefined,
      userWorkspacesResponse: undefined,
    };
  }

  componentDidMount() {
    this.props.hideSpinner();
    this.checkWorkspaces();
    this.validateNihToken();
    this.validateRasLoginGovLink();
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

  // This method is part of the eRA Commons linkage flow. The "return-url" sent to the Shibboleth
  // server is set to "nih-callback", which is configured in our router to point to the homepage
  // component. If we reach this component with a 'token' query parameter, we will attempt to
  // store that token in Terra.
  async validateNihToken() {
    const token = (new URL(window.location.href)).searchParams.get('token');
    if (token) {
      this.setState({eraCommonsLoading: true});
      try {
        const profileResponse = await profileApi().updateNihToken({ jwt: token });
        if (profileResponse.eraCommonsLinkedNihUsername !== undefined) {
          this.setState({eraCommonsLinked: true});
        }
      } catch (e) {
        this.setState({eraCommonsError: 'Error saving NIH Authentication status.'});
      }
    }
  }

  async validateRasLoginGovLink() {
    const authCode = (new URL(window.location.href)).searchParams.get('code');
    const redirectUrl = buildRasRedirectUrl();
    if (authCode) {
      this.setState({rasLoginGovLoading: true});
      try {
        const profileResponse = await profileApi().linkRasAccount({ authCode, redirectUrl });
        if (profileResponse.rasLinkLoginGovUsername !== undefined) {
          this.setState({rasLoginGovLinked: true});
        }
        if (profileResponse.eraCommonsLinkedNihUsername !== undefined) {
          this.setState({eraCommonsLinked: true});
        }
      } catch (e) {
        this.setState({rasLoginGovLinkError: 'Error saving RAS Login.Gov linkage status.'});
        this.setState({rasLoginGovLoading: false});
      }
    }
    // Cleanup parameter from URL after linking.
    window.history.replaceState({}, '', '/');
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
        eraCommonsLinked: (serverConfigStore.get().config.enableEraCommons ?
            (() => !!(getRegistrationTasksMap()['eraCommons']
              .completionTimestamp(profile)))() : true),
        dataUserCodeOfConductCompleted:
          (() => !!(getRegistrationTasksMap()['dataUserCodeOfConduct']
            .completionTimestamp(profile)))()
      });
      // TODO(RW-6493): Update rasCommonsLinked similar to what we are doing for eraCommons

      const {workbenchAccessTasks} = queryParamsStore.getValue();
      const hasAccess = hasRegisteredAccess(profile.accessTierShortNames);
      if (!hasAccess || workbenchAccessTasks) {
        await this.syncCompliance();
      }
      if (workbenchAccessTasks) {
        this.setState({accessTasksRemaining: true, accessTasksLoaded: true});
      } else {
        this.setState({
          accessTasksRemaining: !hasAccess,
          accessTasksLoaded: true
        });
      }
    }
    this.setState((state, props) => ({
      quickTour: state.firstVisit && state.accessTasksRemaining === false
    }));
    // TODO(RW-6494): Update accessTasksRemaining from user tier status and RAS link status.
  }

  async checkWorkspaces() {
    return fetchWithGlobalErrorHandler(() => workspacesApi().getWorkspaces())
        .then(response => this.setState({userWorkspacesResponse: response}));
  }

  userHasWorkspaces(): boolean {
    const {userWorkspacesResponse} = this.state;
    return userWorkspacesResponse && userWorkspacesResponse.items.length > 0;
  }

  render() {
    const {
      accessTasksLoaded, accessTasksRemaining,
      eraCommonsError, eraCommonsLinked, eraCommonsLoading, firstVisitTraining, quickTour,
      trainingCompleted, twoFactorAuthCompleted,
      dataUserCodeOfConductCompleted, userWorkspacesResponse,
      rasLoginGovLinkError, rasLoginGovLinked, rasLoginGovLoading,
    } = this.state;

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
                    (<RegistrationDashboard eraCommonsError={eraCommonsError}
                                            eraCommonsLinked={eraCommonsLinked}
                                            eraCommonsLoading={eraCommonsLoading}
                                            rasLoginGovLinkError={rasLoginGovLinkError}
                                            rasLoginGovLinked={rasLoginGovLinked}
                                            rasLoginGovLoading={rasLoginGovLoading}
                                            trainingCompleted={trainingCompleted}
                                            firstVisitTraining={firstVisitTraining}
                                            twoFactorAuthCompleted={twoFactorAuthCompleted}
                                            dataUserCodeOfConductCompleted={dataUserCodeOfConductCompleted}/>
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
                                See all workspaces
                              </span>
                            </FlexRow>
                            <RecentWorkspaces />
                          </FlexColumn>
                          <FlexColumn>
                            {userWorkspacesResponse &&

                              <React.Fragment>
                                {this.userHasWorkspaces() ?
                                <RecentResources workspaces={userWorkspacesResponse.items}/> :

                                <div data-test-id='getting-started'
                                     style={{
                                       backgroundColor: addOpacity(colors.primary, .1).toString(),
                                       color: colors.primary,
                                       borderRadius: 10,
                                       margin: '2em 0em'}}>
                                  <div style={{margin: '1em 2em'}}>
                                    <h2 style={{fontWeight: 600, marginTop: 0}}>Here are some tips to get you started:</h2>
                                    <CustomBulletList>
                                      <CustomBulletListItem bullet='→'>
                                        Create a <StyledAnchorTag href='https://support.google.com/chrome/answer/2364824'
                                          target='_blank'>Chrome Profile</StyledAnchorTag> with your <AoU/> Researcher
                                        Workbench Google account. This will keep your workbench browser sessions isolated from
                                        your other Google accounts.
                                      </CustomBulletListItem>
                                      <CustomBulletListItem bullet='→'>
                                        Check out <StyledAnchorTag href='library'>Featured Workspaces</StyledAnchorTag> from
                                        the left hand panel to browse through example workspaces.
                                      </CustomBulletListItem>
                                      <CustomBulletListItem bullet='→'>
                                        Browse through our <StyledAnchorTag href={supportUrls.helpCenter}
                                          target='_blank'>support materials</StyledAnchorTag> and forum topics.
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
      </FlexColumn>
      <QuickTourAndVideos showQuickTourInitially={quickTour}/>
    </React.Fragment>;
  }
});
