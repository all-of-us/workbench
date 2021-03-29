import * as fp from 'lodash/fp';
import * as React from 'react';

import {AlertClose, AlertDanger, AlertWarning} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {baseStyles, ResourceCardBase} from 'app/components/card';
import {FlexColumn, FlexRow, FlexSpacer} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {AoU} from 'app/components/text-wrappers';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {getLiveDataUseAgreementVersion} from 'app/utils/code-of-conduct';
import {navigate, serverConfigStore, userProfileStore} from 'app/utils/navigation';
import {buildRasRedirectUrl} from 'app/utils/ras';
import {environment} from 'environments/environment';
import {AccessModule, Profile} from 'generated/fetch';

const styles = reactStyles({
  mainHeader: {
    color: colors.primary, fontSize: '18px', fontWeight: 600,
    letterSpacing: 'normal', marginTop: '-0.25rem'
  },
  cardStyle: {
    boxShadow: '0 0 2px 0 rgba(0,0,0,0.12), 0 3px 2px 0 rgba(0,0,0,0.12)',
    padding: '0.75rem', minHeight: '305px', maxHeight: '305px', maxWidth: '250px',
    minWidth: '250px', justifyContent: 'flex-start', backgroundColor: colors.white
  },
  cardHeader: {
    color: colors.primary, fontSize: '16px', lineHeight: '19px', fontWeight: 600,
    marginBottom: '0.5rem'
  },
  cardDescription: {
    color: colors.primary, fontSize: '14px', lineHeight: '20px'
  },
  closeableWarning: {
    margin: '0px 1rem 1rem 0px', display: 'flex', justifyContent: 'space-between'
  },
  infoBoxButton: {
    color: colors.white, height: '49px', borderRadius: '5px', marginLeft: '1rem',
    maxWidth: '20rem'
  },
  twoFactorAuthModalCancelButton: {
    marginRight: '1rem',
  },
  twoFactorAuthModalHeader: {
    color: colors.primary,
    fontSize: 16,
    fontWeight: 600,
    lineHeight: '24px',
    marginBottom: 0
  },
  twoFactorAuthModalImage: {
    border: `1px solid ${colors.light}`,
    height: '6rem',
    width: '100%',
    marginTop: '1rem'
  },
  twoFactorAuthModalText: {
    color: colors.primary,
    lineHeight: '22px'
  },
  warningIcon: {
    color: colors.warning, height: '20px', width: '20px'
  },
  warningModal: {
    color: colors.primary, fontSize: '18px', lineHeight: '28px', flexDirection: 'row',
    boxShadow: 'none', fontWeight: 600, display: 'flex', justifyContent: 'center',
    alignItems: 'center'
  },
});

export function getTwoFactorSetupUrl(): string {
  const accountChooserBase = 'https://accounts.google.com/AccountChooser';
  const url = new URL(accountChooserBase);
  // If available, set the 'Email' param to give Google a hint that we want to access the
  // target URL as this specific G Suite user. This helps guide users when multi-login is in use.
  if (userProfileStore.getValue()) {
    url.searchParams.set('Email', userProfileStore.getValue().profile.username);
  }
  url.searchParams.set('continue', 'https://myaccount.google.com/signinoptions/two-step-verification/enroll');
  return url.toString();
}

function redirectToTwoFactorSetup(): void {
  AnalyticsTracker.Registration.TwoFactorAuth();
  window.open(getTwoFactorSetupUrl(), '_blank');
}

function redirectToNiH(): void {
  AnalyticsTracker.Registration.ERACommons();
  const url = serverConfigStore.getValue().shibbolethUiBaseUrl + '/login?return-url=' +
      encodeURIComponent(
        window.location.origin.toString() + '/nih-callback?token=<token>');
  window.open(url, '_blank');
}

function redirectToRas(): void {
  AnalyticsTracker.Registration.RasLoginGov();
  // The scopes are also used in backend for fetching user info.
  const url = serverConfigStore.getValue().rasHost + '/auth/oauth/v2/authorize?client_id=' + serverConfigStore.getValue().rasClientId
      + '&prompt=login+consent&redirect_uri=' + buildRasRedirectUrl()
      + '&response_type=code&scope=openid+profile+email+ga4gh_passport_v1';
  window.open(url, '_blank');
}

async function redirectToTraining() {
  AnalyticsTracker.Registration.EthicsTraining();
  await profileApi().updatePageVisits({page: 'moodle'});
  window.open(environment.trainingUrl + '/static/data-researcher.html?saml=on', '_blank');
}

interface RegistrationTask {
  key: string;
  completionPropsKey: string;
  loadingPropsKey?: string;
  title: React.ReactNode;
  description: React.ReactNode;
  buttonText: string;
  completedText: string;
  completionTimestamp: (profile: Profile) => number;
  onClick: Function;
  featureFlag?: boolean;
}

// This needs to be a function, because we want it to evaluate at call time,
// not at compile time, to ensure that we make use of the server config store.
// This is important so that we can feature flag off registration tasks.
//
// Important: The completion criteria here needs to be kept synchronized with
// the server-side logic, else users can get stuck on the registration dashboard
// without a next step:
// https://github.com/all-of-us/workbench/blob/master/api/src/main/java/org/pmiops/workbench/db/dao/UserServiceImpl.java#L240-L272
export const getRegistrationTasks = () => serverConfigStore.getValue() ? ([
  {
    key: 'twoFactorAuth',
    completionPropsKey: 'twoFactorAuthCompleted',
    title: 'Turn on Google 2-Step Verification',
    description: 'Add an extra layer of security to your account by providing your phone number' +
      'in addition to your password to verify your identity upon login.',
    buttonText: 'Get Started',
    completedText: 'Completed',
    completionTimestamp: (profile: Profile) => {
      return profile.twoFactorAuthCompletionTime || profile.twoFactorAuthBypassTime;
    },
    onClick: redirectToTwoFactorSetup
  }, {
    key: 'eraCommons',
    completionPropsKey: 'eraCommonsLinked',
    loadingPropsKey: 'eraCommonsLoading',
    title: 'Connect Your eRA Commons Account',
    description: 'Connect your Researcher Workbench account to your eRA Commons account. ' +
      'There is no exchange of personal data in this step.',
    buttonText: 'Connect',
    completedText: 'Linked',
    completionTimestamp: (profile: Profile) => {
      return profile.eraCommonsCompletionTime || profile.eraCommonsBypassTime;
    },
    onClick: redirectToNiH
  }, {
    key: 'rasLoginGov',
    completionPropsKey: 'rasLoginGovLinked',
    loadingPropsKey: 'rasLoginGovLoading',
    title: 'Connect Your Login.Gov Account',
    featureFlag: serverConfigStore.getValue().enableRasLoginGovLinking,
    description: 'Connect your Researcher Workbench account to your login.gov account. ',
    buttonText: 'Connect',
    completedText: 'Linked',
    completionTimestamp: (profile: Profile) => {
      return profile.rasLinkLoginGovCompletionTime || profile.rasLinkLoginGovBypassTime;
    },
    onClick: redirectToRas
  }, {
    key: 'complianceTraining',
    completionPropsKey: 'trainingCompleted',
    title: <span><i>All of Us</i> Responsible Conduct of Research Training</span>,
    description: <div>Complete ethics training courses to understand the privacy safeguards and the
      compliance requirements for using the <AoU/> dataset.</div>,
    buttonText: 'Complete training',
    featureFlag: serverConfigStore.getValue().enableComplianceTraining,
    completedText: 'Completed',
    completionTimestamp: (profile: Profile) => {
      return profile.complianceTrainingCompletionTime || profile.complianceTrainingBypassTime;
    },
    onClick: redirectToTraining
  }, {
    key: 'dataUserCodeOfConduct',
    completionPropsKey: 'dataUserCodeOfConductCompleted',
    title: 'Data User Code of Conduct',
    description: <span>Sign the Data User Code of Conduct consenting to the <i>All of Us</i> data use policy.</span>,
    buttonText: 'View & Sign',
    featureFlag: serverConfigStore.getValue().enableDataUseAgreement,
    completedText: 'Signed',
    completionTimestamp: (profile: Profile) => {
      if (profile.dataUseAgreementBypassTime) {
        return profile.dataUseAgreementBypassTime;
      }
      // The DUA completion time field tracks the most recent DUA completion
      // timestamp, but doesn't specify whether that DUA is currently active.
      const requiredDuaVersion = getLiveDataUseAgreementVersion(serverConfigStore.getValue());
      if (profile.dataUseAgreementSignedVersion === requiredDuaVersion) {
        return profile.dataUseAgreementCompletionTime;
      }
      return null;
    },
    onClick: () => {
      AnalyticsTracker.Registration.EnterDUCC();
      navigate(['data-code-of-conduct']);
    }
  }
] as RegistrationTask[]).filter(registrationTask => registrationTask.featureFlag === undefined
|| registrationTask.featureFlag) : (() => {
  throw new Error('Cannot load registration tasks before config loaded');
})();

export const getRegistrationTasksMap = () => getRegistrationTasks().reduce((acc, curr) => {
  acc[curr.key] = curr;
  return acc;
}, {});

export interface RegistrationDashboardProps {
  betaAccessGranted: boolean;
  eraCommonsError: string;
  eraCommonsLinked: boolean;
  eraCommonsLoading: boolean;
  rasLoginGovLinkError: string;
  rasLoginGovLinked: boolean;
  rasLoginGovLoading: boolean;
  trainingCompleted: boolean;
  firstVisitTraining: boolean;
  twoFactorAuthCompleted: boolean;
  dataUserCodeOfConductCompleted: boolean;
}

interface State {
  showRefreshButton: boolean;
  trainingWarningOpen: boolean;
  bypassActionComplete: boolean;
  bypassInProgress: boolean;
  twoFactorAuthModalOpen: boolean;
  accessTaskKeyToButtonAsRefresh: Map<string, boolean>;
}

export class RegistrationDashboard extends React.Component<RegistrationDashboardProps, State> {

  constructor(props: RegistrationDashboardProps) {
    super(props);
    this.state = {
      trainingWarningOpen: !props.firstVisitTraining,
      showRefreshButton: false,
      bypassActionComplete: false,
      bypassInProgress: false,
      twoFactorAuthModalOpen: false,
      accessTaskKeyToButtonAsRefresh: new Map()
    };
  }

  componentDidMount() {
    this.setState({showRefreshButton: false});
  }

  get taskCompletionList(): Array<boolean> {
    return getRegistrationTasks().map((config) => {
      return this.props[config.completionPropsKey] as boolean;
    });
  }

  allTasksCompleted(): boolean {
    const {betaAccessGranted} = this.props;
    const {enableBetaAccess} = serverConfigStore.getValue();

    // Beta access is awkwardly not treated as a task in the completion list. So we manually
    // check whether (1) beta access requirement is turned off for this env, or (2) the user
    // has been granted beta access.
    return this.taskCompletionList.every(v => v) &&
      (!enableBetaAccess || betaAccessGranted);
  }

  isEnabled(i: number): boolean {
    const taskCompletionList = this.taskCompletionList;
    if (i === 0) {
      return !taskCompletionList[i];
    } else {
      // Only return the first uncompleted button.
      return !taskCompletionList[i] &&
        fp.filter(index => this.isEnabled(index), fp.range(0, i)).length === 0;
    }
  }

  get taskLoadingList(): Array<boolean> {
    return getRegistrationTasks().map((config) => {
      return this.props[config.loadingPropsKey] as boolean;
    });
  }

  isLoading(i: number): boolean {
    return this.taskLoadingList[i];
  }

  onCardClick(card) {
    if (this.state.accessTaskKeyToButtonAsRefresh.get(card.key)) {
      window.location.reload();
    } else {
      card.onClick();
    }
  }

  async setAllModulesBypassState(isBypassed: boolean) {
    this.setState({bypassInProgress: true});

    // TypeScript enum iteration is nonfunctional
    // so just copy the whole list
    const modules = [
      AccessModule.COMPLIANCETRAINING,
      AccessModule.ERACOMMONS,
      AccessModule.TWOFACTORAUTH,
      AccessModule.DATAUSEAGREEMENT,
      AccessModule.BETAACCESS
    ];

    for (const module of modules) {
      await profileApi().unsafeSelfBypassAccessRequirement({
        moduleName: module,
        isBypassed: isBypassed
      });
    }

    this.setState({bypassInProgress: false, bypassActionComplete: true});
  }

  render() {
    const {bypassActionComplete, bypassInProgress, trainingWarningOpen} = this.state;
    const {betaAccessGranted, eraCommonsError, trainingCompleted, rasLoginGovLinkError} = this.props;
    const {enableBetaAccess, unsafeAllowSelfBypass} = serverConfigStore.getValue();

    const anyBypassActionsRemaining = !(this.allTasksCompleted() && betaAccessGranted);

    // Override on click for the two factor auth access task. This is important because we want to affect the DOM
    // for this specific task.
    const registrationTasksToRender = getRegistrationTasks().map(registrationTask =>
      registrationTask.key === 'twoFactorAuth' ? {...registrationTask,
        onClick: () => this.setState({twoFactorAuthModalOpen: true})} :
        registrationTask);
    // Assign relative positioning so the spinner's absolute positioning anchors
    // it within the registration box.
    // TODO(RW-6495): Decide the correct error message for login.gov linking failure.
    return <FlexColumn style={{position: 'relative'}} data-test-id='registration-dashboard'>
      {bypassInProgress && <SpinnerOverlay />}
      <div style={styles.mainHeader}>Complete Registration</div>
      {unsafeAllowSelfBypass &&
        <div data-test-id='self-bypass'
             style={{...baseStyles.card, ...styles.warningModal, margin: '0.85rem 0 0'}}>
          {bypassActionComplete &&
            <span>Bypass action is complete. Reload the page to continue.</span>}
          {!bypassActionComplete && <span>
            [Test environment] Self-service bypass is enabled:&nbsp;
            {anyBypassActionsRemaining &&
              <Button style={{marginLeft: '0.5rem'}}
                      onClick={() => this.setAllModulesBypassState(true)}
                      disabled={bypassInProgress}>Bypass all</Button>}
            {!anyBypassActionsRemaining &&
              <Button style={{marginLeft: '0.5rem'}}
                      onClick={() => this.setAllModulesBypassState(false)}
                      disabled={bypassInProgress}>Un-bypass all</Button>}
          </span>
          }
        </div>
      }
      {enableBetaAccess && !betaAccessGranted &&
        <div data-test-id='beta-access-warning'
             style={{...baseStyles.card, ...styles.warningModal, margin: '1rem 0 0'}}>
          <ClrIcon shape='warning-standard' class='is-solid'
                   style={styles.warningIcon}/>
          You have not been granted beta access. Please contact support@researchallofus.org.
        </div>}
      <FlexRow style={{marginTop: '0.85rem'}}>
        {registrationTasksToRender.map((card, i) => {
          return <ResourceCardBase key={i} data-test-id={'registration-task-' + card.key}
            style={this.isEnabled(i) ? styles.cardStyle : {...styles.cardStyle,
              opacity: '0.6', maxHeight: this.allTasksCompleted() ? '190px' : '305px',
              minHeight: this.allTasksCompleted() ? '190px' : '305px'}}>
            <FlexColumn style={{justifyContent: 'flex-start'}}>
              <div style={styles.cardHeader}>STEP {i + 1}</div>
              <div style={styles.cardHeader}>{card.title}</div>
            </FlexColumn>
            {!this.allTasksCompleted() &&
            <div style={styles.cardDescription}>{card.description}</div>}
            <FlexSpacer/>
            {this.taskCompletionList[i] ?
              <Button disabled={true} data-test-id='completed-button'
                      style={{backgroundColor: colors.success,
                        width: 'max-content',
                        cursor: 'default'}}>
                {card.completedText}
                <ClrIcon shape='check' style={{marginLeft: '0.5rem'}}/>
              </Button> :
            <Button onClick={() => this.isLoading(i) ? true : this.onCardClick(card)}
                    style={{width: 'max-content',
                      cursor: this.isEnabled(i) && !this.isLoading(i) ? 'pointer' : 'default'}}
                    disabled={!this.isEnabled(i)} data-test-id='registration-task-link'>
              {this.state.accessTaskKeyToButtonAsRefresh.get(card.key) ?
                <div>
                  Refresh
                  <ClrIcon shape='refresh' style={{marginLeft: '0.5rem'}}/>
                </div> : card.buttonText}
              {this.isLoading(i) ? <Spinner style={{marginLeft: '0.5rem', width: 20, height: 20}}/> : null}
            </Button>}
          </ResourceCardBase>;
        })}
      </FlexRow>

      {eraCommonsError && <AlertDanger data-test-id='era-commons-error'
                                        style={{margin: '0px 1rem 1rem 0px'}}>
          <ClrIcon shape='exclamation-triangle' class='is-solid'/>
          Error Linking NIH Username: {eraCommonsError} Please try again!
      </AlertDanger>}
      {rasLoginGovLinkError && <AlertDanger data-test-id='ras-login-gov-error'
                                       style={{margin: '0px 1rem 1rem 0px'}}>
        <ClrIcon shape='exclamation-triangle' class='is-solid'/>
        Error Linking login.gov account: {rasLoginGovLinkError} Please try again!
      </AlertDanger>}
      {trainingWarningOpen && !trainingCompleted &&
      <AlertWarning style={styles.closeableWarning}>
        <div style={{display: 'flex'}}>
          <ClrIcon shape='exclamation-triangle' class='is-solid'/>
          <div>Please try refreshing this page in a few minutes as it takes time to update your
            status once you have completed compliance training.</div>
        </div>
        <AlertClose onClick={() => this.setState({trainingWarningOpen: false})}/>
      </AlertWarning>}
      {this.allTasksCompleted() &&
        <div style={{...baseStyles.card, ...styles.warningModal, marginRight: 0}}
             data-test-id='success-message'>
          You successfully completed all the required steps to access the Researcher Workbench.
          <Button style={{marginLeft: '0.5rem'}}
                  onClick={() => {
                    // Quirk / hack note: the goal here is to send the user to the homepage once they've completed
                    // all access modules. Normally we would just navigate(['']) to do this. However, because
                    // of the way this dashboard is rendered *within* the homepage component, a call to
                    // navigate is not enough to trigger the normal homepage to load. As a workaround, we
                    // explicitly clear the search query and redirect to the root path.
                    window.location.pathname = '/';
                    window.location.search = '';
                  }}>Get Started</Button>
        </div>
      }
      {this.state.twoFactorAuthModalOpen && <Modal width={500}>
          <ModalTitle style={styles.twoFactorAuthModalHeader}>Redirecting to turn on Google 2-step Verification</ModalTitle>
          <ModalBody>
              <div style={styles.twoFactorAuthModalText}>Clicking ‘Proceed’ will direct you to a Google page where you
                  need to login with your <span style={{fontWeight: 600}}>researchallofus.org</span> account and turn
                  on 2-Step Verification. Once you complete this step, you will see the screen shown below. At that
                  point, you can return to this page and click 'Refresh’.</div>
              <img style={styles.twoFactorAuthModalImage} src='assets/images/2sv-image.png' />
          </ModalBody>
          <ModalFooter>
              <Button onClick = {() => this.setState({twoFactorAuthModalOpen: false})}
                      type='secondary' style={styles.twoFactorAuthModalCancelButton}>Cancel</Button>
              <Button onClick = {() => {
                redirectToTwoFactorSetup();
                this.setState((state) => ({
                  accessTaskKeyToButtonAsRefresh: state.accessTaskKeyToButtonAsRefresh.set('twoFactorAuth', true),
                  twoFactorAuthModalOpen: false
                }));
              }}
                      type='primary'>Proceed</Button>
          </ModalFooter>
      </Modal>}
    </FlexColumn>;
  }
}
