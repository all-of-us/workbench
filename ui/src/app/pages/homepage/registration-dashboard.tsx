import * as fp from 'lodash/fp';
import * as React from 'react';

import {AlertClose, AlertDanger, AlertWarning} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {baseStyles, ResourceCardBase} from 'app/components/card';
import {FlexColumn, FlexRow, FlexSpacer} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {SpinnerOverlay} from 'app/components/spinners';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {navigate, serverConfigStore, userProfileStore} from 'app/utils/navigation';
import {environment} from 'environments/environment';
import {AccessModule, Profile} from 'generated/fetch';

const styles = reactStyles({
  registrationPage: {
    display: 'flex', flexDirection: 'column',
    // Assign relative positioning so the spinner's absolute positioning anchors
    // it within the registration box.
    position: 'relative',
  },
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
  infoBoxButton: {
    color: colors.white, height: '49px', borderRadius: '5px', marginLeft: '1rem',
    maxWidth: '20rem'
  },
  warningIcon: {
    color: colors.warning, height: '20px', width: '20px'
  },
  warningModal: {
    color: colors.primary, fontSize: '18px', lineHeight: '28px', flexDirection: 'row',
    boxShadow: 'none', fontWeight: 600, display: 'flex', justifyContent: 'center',
    alignItems: 'center'
  },
  closeableWarning: {
    margin: '0px 1rem 1rem 0px', display: 'flex', justifyContent: 'space-between'
  }
});

function redirectToGoogleSecurity(): void {
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
  const url = environment.shibbolethUrl + '/link-nih-account?redirect-url=' +
          encodeURIComponent(
            window.location.origin.toString() + '/nih-callback?token={token}');
  window.location.assign(url);
}

async function redirectToTraining() {
  await profileApi().updatePageVisits({page: 'moodle'});
  window.location.assign(environment.trainingUrl + '/static/data-researcher.html?saml=on');
}

interface RegistrationTask {
  key: string;
  completionPropsKey: string;
  title: string;
  description: string;
  buttonText: string;
  completedText: string;
  isRefreshable?: boolean;
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
    description: 'Add an extra layer of security to your account by providing your ' +
      'phone number in addition to your password to verify your identity upon login.',
    buttonText: 'Get Started',
    completedText: 'Completed',
    isRefreshable: true,
    completionTimestamp: (profile: Profile) => {
      return profile.twoFactorAuthCompletionTime || profile.twoFactorAuthBypassTime;
    },
    onClick: redirectToGoogleSecurity
  }, {
    key: 'complianceTraining',
    completionPropsKey: 'trainingCompleted',
    title: 'Complete Online Training',
    description: 'Complete mandatory compliance training courses on how data should be used ' +
      'and handled.',
    buttonText: 'Complete training',
    completedText: 'Completed',
    completionTimestamp: (profile: Profile) => {
      return profile.complianceTrainingCompletionTime || profile.complianceTrainingBypassTime;
    },
    onClick: redirectToTraining
  }, {
    key: 'eraCommons',
    completionPropsKey: 'eraCommonsLinked',
    title: 'Login to eRA Commons',
    description: 'Link to your eRA Commons account to the workbench to gain full access to data ' +
      'and tools.',
    buttonText: 'Login',
    completedText: 'Linked',
    completionTimestamp: (profile: Profile) => {
      return profile.eraCommonsCompletionTime || profile.eraCommonsBypassTime;
    },
    onClick: redirectToNiH
  }, {
    key: 'dataUseAgreement',
    completionPropsKey: 'dataUseAgreementCompleted',
    title: 'Data Use Agreement',
    description: 'Sign our data use agreement consenting to the All of Us data use policy.',
    buttonText: 'View & Sign',
    featureFlag: serverConfigStore.getValue().enableDataUseAgreement,
    completedText: 'Signed',
    completionTimestamp: (profile: Profile) => {
      return profile.dataUseAgreementCompletionTime || profile.dataUseAgreementBypassTime;
    },
    onClick: () => navigate(['data-use-agreement'])
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
  eraCommonsLinked: boolean;
  eraCommonsError: string;
  trainingCompleted: boolean;
  firstVisitTraining: boolean;
  twoFactorAuthCompleted: boolean;
  dataUseAgreementCompleted: boolean;
}

interface State {
  showRefreshButton: boolean;
  trainingWarningOpen: boolean;
  bypassActionComplete: boolean;
  bypassInProgress: boolean;
}

export class RegistrationDashboard extends React.Component<RegistrationDashboardProps, State> {

  constructor(props: RegistrationDashboardProps) {
    super(props);
    this.state = {
      trainingWarningOpen: !props.firstVisitTraining,
      showRefreshButton: false,
      bypassActionComplete: false,
      bypassInProgress: false,
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
      return !taskCompletionList[i] &&
      fp.filter(index => this.isEnabled(index), fp.range(0, i)).length === 0;
    }
  }

  showRefreshFlow(isRefreshable: boolean): boolean {
    return isRefreshable && this.state.showRefreshButton;
  }

  onCardClick(card) {
    if (this.showRefreshFlow(card.isRefreshable)) {
      window.location.reload();
    } else {
      if (card.isRefreshable) {
        this.setState({
          showRefreshButton: true
        });
      }
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

    return <div style={styles.registrationPage}
                data-test-id='registration-dashboard'>
      {bypassInProgress && <SpinnerOverlay />}
      {environment.enableHomepageRestyle && <div style={styles.mainHeader}>Getting Started</div>}
      {!environment.enableHomepageRestyle &&
        <div style={{...styles.mainHeader, color: colors.white}}>Getting Started</div>}
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
        {getRegistrationTasks().map((card, i) => {
          return <ResourceCardBase key={i} data-test-id={'registration-task-' + i.toString()}
            style={this.isEnabled(i) ? styles.cardStyle : {...styles.cardStyle,
              opacity: '0.6', maxHeight: this.allTasksCompleted() ? '160px' : '305px',
              minHeight: this.allTasksCompleted() ? '160px' : '305px'}}>
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
                <ClrIcon shape='check' style={{marginRight: '0.3rem'}}/>{card.completedText}
              </Button> :
            <Button onClick={ () => this.onCardClick(card) }
                    style={{width: 'max-content',
                      cursor: this.isEnabled(i) ? 'pointer' : 'default'}}
                    disabled={!this.isEnabled(i)} data-test-id='registration-task-link'>
              {this.showRefreshFlow(card.isRefreshable) ?
                <div>
                  <ClrIcon shape='refresh' style={{marginRight: '0.3rem'}}/>
                  Refresh
                </div> : card.buttonText}
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
    </div>;
  }
}
