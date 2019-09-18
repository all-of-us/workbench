import {Component} from '@angular/core';
import {navigate, queryParamsStore, serverConfigStore} from 'app/utils/navigation';

import * as fp from 'lodash/fp';
import * as React from 'react';

import {
  CardButton,
  Clickable,
} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Header, SmallHeader} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {Modal} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {QuickTourReact} from 'app/pages/homepage/quick-tour-modal';
import {RecentWork} from 'app/pages/homepage/recent-work';
import {RecentWorkspaces} from 'app/pages/homepage/recent-workspaces';
import {getRegistrationTasksMap, RegistrationDashboard} from 'app/pages/homepage/registration-dashboard';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {hasRegisteredAccessFetch, reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {environment} from 'environments/environment';
import {
  Profile,
} from 'generated/fetch';

export const styles = reactStyles({
  bottomBanner: {
    width: '100%', display: 'flex', backgroundColor: colors.primary,
    paddingLeft: '3.5rem', alignItems: 'center'
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
  footer: {
    width: '100%', backgroundColor: colors.light, marginTop: '2%'
  },
  footerInner: {
    marginLeft: '3%', marginRight: '2%',
  },
  footerText: {
    height: '176px', opacity: 0.87, color: colors.secondary,
    fontSize: '0.6rem', fontWeight: 500, lineHeight: '30px', width: '100%',
    flexWrap: 'nowrap', overflowY: 'auto'
  },
  footerTextTitle: {
    color: colors.primary,
    fontSize: '0.67rem',
    fontWeight: 500
  },
  footerTitle: {
    height: '34px', opacity: 0.87, color: colors.primary, fontSize: '0.75rem',
    fontWeight: 600, lineHeight: '34px', marginTop: '1rem', marginBottom: '0.5rem'
  },
  linksBlock: {
    marginBottom: '1rem', flexShrink: 1, minWidth: '13rem'
  },
  logo: {
    height: '3.5rem', width: '7rem', lineHeight: '85px'
  },
  mainHeader: {
    color: colors.primary, fontSize: 28, fontWeight: 400, letterSpacing: 'normal'
  },
  pageWrapper: {
    marginLeft: '-1rem', marginRight: '-0.6rem', justifyContent: 'space-between', fontSize: '16px'
  },
  quickTourCardsRow: {
    justifyContent: 'flex-start', maxHeight: '26rem', marginTop: '0.5rem'
  },
  quickTourLabel: {
    fontSize: 18, lineHeight: '34px', color: colors.primary, paddingRight: '2.3rem',
    fontWeight: 600, marginTop: '2rem', width: '33%'
  },
  singleCard: {
    width: '87.34%', minHeight: '18rem', maxHeight: '26rem',
    borderRadius: '5px', backgroundColor: 'rgba(255, 255, 255, 0.15)',
    boxShadow: '0 0 2px 0 rgba(0, 0, 0, 0.12), 0 3px 2px 0 rgba(0, 0, 0, 0.12)',
    border: 'none', marginTop: '1rem'
  },
  welcomeMessageIcon: {
    height: '2.25rem', width: '2.75rem'
  },
  // once enableHomepageRestyle is enabled, delete all styles ending in 'ToDelete'
  mainHeaderToDelete: {
    color: colors.white, fontSize: 28, fontWeight: 400,
    display: 'flex', letterSpacing: 'normal'
  },
  backgroundImageToDelete: {
    backgroundImage: 'url("/assets/images/AoU-HP-background.jpg")',
    backgroundRepeat: 'no-repeat', backgroundSize: 'cover', height: '100%',
    marginLeft: '-1rem', marginRight: '-0.6rem', display: 'flex',
    flexDirection: 'column', justifyContent: 'space-between'
  },
  quickRowToDelete: {
    display: 'flex', justifyContent: 'flex-start', maxHeight: '26rem',
    flexDirection: 'row', marginLeft: '4rem', padding: '1rem'
  },
  quickTourLabelToDelete: {
    fontSize: 28, lineHeight: '34px', color: colors.white, paddingRight: '2.3rem',
    marginTop: '2rem', width: '33%'
  },
  footerToDelete: {
    height: '300px', width: '100%', backgroundColor: colors.primary,
    boxShadow: '0 0 2px 0 rgba(0, 0, 0, 0.12), 0 3px 2px 0 rgba(0, 0, 0, 0.12)',
    marginTop: '2%',
  },
  footerInnerToDelete: {
    display: 'flex', flexDirection: 'column', marginLeft: '5%', marginRight: '5%',
  },
  footerTitleToDelete: {
    height: '34px', opacity: 0.87, color: colors.white, fontSize: 28,
    fontWeight: 600, lineHeight: '34px', width: '87.34%', marginTop: '1.4rem'
  },
  footerTextToDelete: {
    height: '176px', opacity: 0.87, color: colors.secondary, fontSize: '16px',
    fontWeight: 400, lineHeight: '30px', display: 'flex', width: '100%',
    flexDirection: 'column', flexWrap: 'nowrap', overflowY: 'auto'
  },
  linksBlockToDelete: {
    display: 'flex', marginBottom: '1.2rem', marginLeft: '1.4rem',
    flexDirection: 'column', flexShrink: 1, minWidth: 0
  },
  bottomBannerToDelete: {
    width: '100%', display: 'flex', backgroundColor: colors.primary, height: '5rem',
    paddingLeft: '3.5rem', alignItems: 'center'
  },
  bottomLinksToDelete: {
    color: colors.white, fontSize: '0.7rem', height: '1rem',
    marginLeft: '2.5rem', fontWeight: 400
  }
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
    };
  }

  componentDidMount() {
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
        trainingCompleted: getRegistrationTasksMap()['complianceTraining'].isComplete(result)
      });
    }).catch(err => {
      this.setState({trainingCompleted: false});
      console.error('error fetching moodle training status:', err);
    });
    const twoFactorAuthStatus = profileApi().syncTwoFactorAuthStatus().then(result => {
      this.setState({
        twoFactorAuthCompleted: getRegistrationTasksMap()['twoFactorAuth'].isComplete(result)
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
        eraCommonsLinked: getRegistrationTasksMap()['eraCommons'].isComplete(profile),
        dataUseAgreementCompleted: (serverConfigStore.getValue().enableDataUseAgreement ?
          (() => getRegistrationTasksMap()['dataUseAgreement'].isComplete(profile))() : true)
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

    if (environment.enableHomepageRestyle) {
      return <React.Fragment>
        <FlexColumn style={styles.pageWrapper}>
          <FlexRow style={{marginLeft: '3%'}}>
            <FlexColumn style={{width: '50%'}}>
              <FlexRow>
                <FlexColumn>
                  <Header style={{fontWeight: 500, color: colors.secondary, fontSize: '0.92rem'}}>
                    Welcome to</Header>
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
                the secure analysis platform for All of Us data</SmallHeader>
            </FlexColumn>
            <div></div>
          </FlexRow>
          <FadeBox style={styles.fadeBox}>
            {/* The elements inside this fadeBox will be changed as part of ongoing
            homepage redesign work*/}
            <div style={{display: 'flex', justifyContent: 'flex-start', flexDirection: 'column'}}>
              {/*<FlexColumn style={styles.singleCard}>*/}
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
                        /*<FlexRow style={{paddingTop: '2rem'}}>
                          <FlexColumn style={styles.contentWrapperLeft}>
                            <FlexRow style={styles.mainHeader}>Researcher Workbench</FlexRow>
                            <CardButton onClick={() => navigate(['workspaces/build'])}
                                        style={{margin: '1.9rem 106px 0 3%'}}>
                              Create a <br/> New Workspace
                              <ClrIcon shape='plus-circle' style={{height: '32px', width: '32px'}}/>
                            </CardButton>
                          </FlexColumn>
                          <FlexColumn style={styles.contentWrapperRight}>
                            <a onClick={() => navigate(['workspaces'])}
                               style={{fontSize: '14px', color: colors.primary}}>
                              See All Workspaces</a>
                            <FlexColumn style={{marginRight: '3%'}}>
                              <div style={{color: colors.primary, height: '1.9rem'}}>
                                <div style={{marginTop: '.5rem'}}>Your Last Accessed Items</div>
                              </div>
                              <RecentWork dark={true}/>
                            </FlexColumn>
                          </FlexColumn>
                        </FlexRow>*/
                          <React.Fragment>
                            <FlexColumn>
                              <FlexRow style={{justifyContent: 'space-between', alignItems: 'center'}}>
                                <FlexRow style={{alignItems: 'center'}}>
                                  <SmallHeader style={{marginTop: '0px'}}>Workspaces</SmallHeader>
                                  <ClrIcon
                                    shape='plus-circle'
                                    size={21}
                                    className={'is-solid'}
                                    style={{color: colors.accent, marginLeft: '1rem', cursor: 'pointer'}}
                                    onClick={() => navigate(['workspaces/build'])}
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
                              <SmallHeader>Recently Accessed Items</SmallHeader>
                              <RecentWork/>
                            </FlexColumn>
                          </React.Fragment>
                        )
                  ) :
                  <Spinner dark={true} style={{width: '100%', marginTop: '5rem'}}/>}
              {/*</FlexColumn>*/}
            </div>
          </FadeBox>
          <div>
            <FlexColumn style={{marginLeft: '3%'}}>
              <div style={styles.quickTourLabel}>Quick Tour and Videos</div>
              <FlexRow style={styles.quickTourCardsRow}>
                {quickTourResources.map((thumbnail, i) => {
                  return <React.Fragment key={i}>
                    <Clickable onClick={thumbnail.onClick}
                               data-test-id={'quick-tour-resource-' + i}>
                      <img style={{maxHeight: '7rem', width: '9rem', marginRight: '0.5rem'}}
                           src={thumbnail.src}/>
                    </Clickable>
                  </React.Fragment>;
                })}
              </FlexRow>
            </FlexColumn>
            <div>
              <div style={styles.footer}>
                <FlexColumn style={styles.footerInner}>
                  <div style={styles.footerTitle}>
                    How to Use the All of Us Researcher Workbench</div>
                  <FlexRow style={{justifyContent: 'space-between'}}>
                    {footerLinks.map((col, i) => {
                      return <React.Fragment key={i}>
                        <FlexColumn style={styles.linksBlock}>
                          <FlexColumn style={styles.footerText}>
                            <div style={styles.footerTextTitle}>{col.title}</div>
                            <ul style={{color: colors.secondary, marginLeft: '2%'}}>
                              {col.links.map((link, ii) => {
                                return <li key={ii}>
                                  <a href='#' style={{color: colors.accent}}>{link}</a>
                                </li>;
                              } )}
                            </ul>
                          </FlexColumn>
                        </FlexColumn>
                      </React.Fragment>;
                    })}
                  </FlexRow>
                </FlexColumn>
              </div>
            </div>
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
    } else { // delete this block below once enableHomepageRestyle is removed
      return <React.Fragment>
        <div style={styles.backgroundImageToDelete}>
          <div style={{display: 'flex', justifyContent: 'center'}}>
            <div style={styles.singleCard}>
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
                      <div style={{display: 'flex', flexDirection: 'row', paddingTop: '2rem'}}>
                        <div style={styles.contentWrapperLeft}>
                          <div style={styles.mainHeaderToDelete}>Researcher Workbench</div>
                          <CardButton onClick={() => navigate(['workspaces/build'])}
                                      style={{margin: '1.9rem 106px 0 3%'}}>
                            Create a <br/> New Workspace
                            <ClrIcon shape='plus-circle' style={{height: '32px', width: '32px'}}/>
                          </CardButton>
                        </div>
                        <div style={styles.contentWrapperRight}>
                          <a onClick={() => navigate(['workspaces'])}
                             style={{fontSize: '14px', color: colors.white}}>
                            See All Workspaces</a>
                          <div
                            style={{marginRight: '3%', display: 'flex', flexDirection: 'column'}}>
                            <div style={{color: colors.white, height: '1.9rem'}}>
                              <div style={{marginTop: '.5rem'}}>Your Last Accessed Items</div>
                            </div>
                            <RecentWork dark={true}/>
                          </div>
                        </div>
                      </div>
                      )
                ) :
                <Spinner dark={true} style={{width: '100%', marginTop: '5rem'}}/>}
            </div>
          </div>
          <div>
            <div style={styles.quickRowToDelete}>
              <div style={styles.quickTourLabelToDelete}>Quick Tour & Videos</div>
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
              <div style={styles.footerToDelete}>
                <div style={styles.footerInnerToDelete}>
                  <div style={styles.footerTitleToDelete}>
                    How to Use the All of Us Researcher Workbench</div>
                  <div style={{display: 'flex', justifyContent: 'flex-end'}}>
                    <TooltipTrigger content='Coming Soon' side='left'>
                      <a href='#' style={{color: colors.white}}>See all documentation</a>
                    </TooltipTrigger>
                  </div>
                  <div style={{display: 'flex', flexDirection: 'row',
                    width: '87.34%', justifyContent: 'space-between'}}>
                    {footerLinks.map((col, i) => {
                      return <React.Fragment key={i}>
                        <div style={styles.linksBlockToDelete}>
                          <div style={styles.footerTextToDelete}>
                            <div style={{color: colors.white, marginTop: '2%'}}>{col.title}</div>
                            <ul style={{color: colors.secondary}}>
                              {col.links.map((link, ii) => {
                                return <li key={ii}>
                                  <a href='#' style={{color: colors.secondary}}>{link}</a>
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
              <div style={styles.bottomBannerToDelete}>
                <div style={styles.logo}>
                  <img src='/assets/images/all-of-us-logo-footer.svg'/>
                </div>
                <div style={styles.bottomLinksToDelete}>Privacy Policy</div>
                <div style={styles.bottomLinksToDelete}>Terms of Service</div>
              </div>
            </div>
          </div>

        </div>

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
