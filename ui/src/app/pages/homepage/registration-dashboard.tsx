import * as fp from 'lodash/fp';
import * as React from 'react';

import {AlertClose, AlertDanger, AlertWarning} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {baseStyles, ResourceCardBase} from 'app/components/card';
import {FlexColumn, FlexRow, FlexSpacer} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {navigate, serverConfigStore, userProfileStore} from 'app/utils/navigation';
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

function redirectToGoogleSecurity(): void {
  AnalyticsTracker.Registration.TwoFactorAuth();
  let url = 'https://myaccount.google.com/u/2/signinoptions/two-step-verification/enroll';
  const {profile} = userProfileStore.getValue();
  // The profile should always be available at this point, but avoid making an
  // implicit hard dependency on that, since the authuser'less URL is still useful.
  if (profile.username) {
    // Attach the authuser, in case users are using Google multilogin - their
    // AoU researcher account is unlikely to be their primary Google login.
    url += `?authuser=${profile.username}`;
  }
  window.open(url, '_blank');
}

function redirectToNiH(): void {
  AnalyticsTracker.Registration.ERACommons();
  const url = environment.shibbolethUrl + '/link-nih-account?redirect-url=' +
          encodeURIComponent(
            window.location.origin.toString() + '/nih-callback?token={token}');
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
export const getRegistrationTasks = () => serverConfigStore.getValue() ? ([
  {
    key: 'twoFactorAuth',
    completionPropsKey: 'twoFactorAuthCompleted',
    title: 'Turn on Google 2-Step Verification',
    description: 'Add an extra layer of security to your account by providing your phone number in addition to your password to verify your identity upon login.',
    buttonText: 'Get Started',
    completedText: 'Completed',
    completionTimestamp: (profile: Profile) => {
      return profile.twoFactorAuthCompletionTime || profile.twoFactorAuthBypassTime;
    },
    onClick: redirectToGoogleSecurity
  }, {
    key: 'eraCommons',
    completionPropsKey: 'eraCommonsLinked',
    loadingPropsKey: 'eraCommonsLoading',
    title: 'Connect Your eRA Commons Account',
    description: 'Connect your Workbench account to your eRA Commons account. There is no exchange of personal data in this step.',
    buttonText: 'Connect',
    completedText: 'Linked',
    completionTimestamp: (profile: Profile) => {
      return profile.eraCommonsCompletionTime || profile.eraCommonsBypassTime;
    },
    onClick: redirectToNiH
  }, {
    key: 'complianceTraining',
    completionPropsKey: 'trainingCompleted',
    title: <span><i>All of Us</i> Responsible Conduct of Research Training</span>,
    description: <div>Complete ethics training courses to understand the privacy safeguards and the
      compliance requirements for using the <i>All of Us</i> Dataset.</div>,
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
    description: <span>Sign the data user code of conduct consenting to the <i>All of Us</i> data use policy.</span>,
    buttonText: 'View & Sign',
    featureFlag: serverConfigStore.getValue().enableDataUseAgreement,
    completedText: 'Signed',
    completionTimestamp: (profile: Profile) => {
      return profile.dataUseAgreementCompletionTime || profile.dataUseAgreementBypassTime;
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
    return this.taskCompletionList.every(v => v);
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
    const {betaAccessGranted, eraCommonsError, trainingCompleted} = this.props;
    const canUnsafeSelfBypass = serverConfigStore.getValue().unsafeAllowSelfBypass;

    const anyBypassActionsRemaining = !(this.allTasksCompleted() && betaAccessGranted);

    // Override on click for the two factor auth access task. This is important because we want to affect the DOM
    // for this specific task.
    const registrationTasksToRender = getRegistrationTasks().map(registrationTask =>
      registrationTask.key === 'twoFactorAuth' ? {...registrationTask,
        onClick: () => this.setState({twoFactorAuthModalOpen: true})} :
        registrationTask);
    // Assign relative positioning so the spinner's absolute positioning anchors
    // it within the registration box.
    return <FlexColumn style={{position: 'relative'}} data-test-id='registration-dashboard'>
      {bypassInProgress && <SpinnerOverlay />}
      <div style={styles.mainHeader}>Complete Registration</div>
      {canUnsafeSelfBypass &&
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
      {!betaAccessGranted &&
        <div data-test-id='beta-access-warning'
             style={{...baseStyles.card, ...styles.warningModal, margin: '1rem 0 0'}}>
          <ClrIcon shape='warning-standard' class='is-solid'
                   style={styles.warningIcon}/>
          You have not been granted beta access. Please contact support@researchallofus.org.
        </div>}
      <FlexRow style={{marginTop: '0.85rem'}}>
        {registrationTasksToRender.map((card, i) => {
          return <ResourceCardBase key={i} data-test-id={'registration-task-' + i.toString()}
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
            <Button onClick={ () => this.isLoading(i) ? true : this.onCardClick(card) }
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
      {trainingWarningOpen && !trainingCompleted &&
      <AlertWarning style={styles.closeableWarning}>
        <div style={{display: 'flex'}}>
          <ClrIcon shape='exclamation-triangle' class='is-solid'/>
          <div>Please try refreshing this page in a few minutes as it takes time to update your
            status once you have completed compliance training.</div>
        </div>
        <AlertClose onClick={() => this.setState({trainingWarningOpen: false})}/>
      </AlertWarning>}
      {(this.allTasksCompleted() && betaAccessGranted) &&
      <div style={{...baseStyles.card, ...styles.warningModal, marginRight: 0}}
           data-test-id='success-message'>
        You successfully completed all the required steps to access the Researcher Workbench.
        <Button style={{marginLeft: '0.5rem'}}
                onClick={() => window.location.reload()}>Get Started</Button>
      </div>}
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
                redirectToGoogleSecurity();
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
