import * as fp from 'lodash/fp';
import * as React from 'react';

import {AlertClose, AlertDanger, AlertWarning} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {baseStyles, ResourceCardBase} from 'app/components/card';
import {FlexColumn, FlexRow, FlexSpacer} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {
  bypassAll,
  getRegistrationTasks,
  GetStartedButton,
} from 'app/utils/access-utils';
import {NavigationProps} from 'app/utils/navigation';
import {serverConfigStore} from 'app/utils/stores';
import {withNavigation} from 'app/utils/with-navigation-hoc';
import {AccessModule} from 'generated/fetch';
import {TwoFactorAuthModal} from './two-factor-auth-modal';

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
  warningIcon: {
    color: colors.warning, height: '20px', width: '20px'
  },
  warningModal: {
    color: colors.primary, fontSize: '18px', lineHeight: '28px', flexDirection: 'row',
    boxShadow: 'none', fontWeight: 600, display: 'flex', justifyContent: 'center',
    alignItems: 'center'
  },
});

export interface RegistrationDashboardProps {
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

interface HocProps extends RegistrationDashboardProps, NavigationProps {}

interface State {
  showRefreshButton: boolean;
  trainingWarningOpen: boolean;
  bypassActionComplete: boolean;
  bypassInProgress: boolean;
  twoFactorAuthModalOpen: boolean;
  accessTaskKeyToButtonAsRefresh: Map<string, boolean>;
}


export const RegistrationDashboard = fp.flow(withNavigation)(class extends React.Component<HocProps, State> {
  constructor(props: HocProps) {
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

  getRegistrationTasks() {
    return getRegistrationTasks();
  }

  get taskCompletionList(): Array<boolean> {
    return this.getRegistrationTasks().map((config) => {
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
    return this.getRegistrationTasks().map((config) => {
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
      AccessModule.DATAUSERCODEOFCONDUCT,
      AccessModule.RASLINKLOGINGOV,
    ];

    await bypassAll(modules, isBypassed);
    this.setState({bypassInProgress: false, bypassActionComplete: true});
  }

  render() {
    const {bypassActionComplete, bypassInProgress, trainingWarningOpen} = this.state;
    const {eraCommonsError, trainingCompleted, rasLoginGovLinkError} = this.props;
    const {unsafeAllowSelfBypass} = serverConfigStore.get().config;

    const anyBypassActionsRemaining = !this.allTasksCompleted();

    // Override on click for the two factor auth access task. This is important because we want to affect the DOM
    // for this specific task.
    const registrationTasksToRender = this.getRegistrationTasks().map(registrationTask =>
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
            <span>Bypass action is complete. <GetStartedButton/>
            </span>}
          {!bypassActionComplete && <span>
            [Test environment] Self-service bypass is enabled:
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
          You successfully completed all the required steps to access the Researcher Workbench. <GetStartedButton/>
        </div>
      }
      {this.state.twoFactorAuthModalOpen && <TwoFactorAuthModal
        onClick={() => {
          this.setState((state) => ({
            accessTaskKeyToButtonAsRefresh: state.accessTaskKeyToButtonAsRefresh.set('twoFactorAuth', true),
            twoFactorAuthModalOpen: false
          }));
        }}
        onCancel={() => this.setState({twoFactorAuthModalOpen: false})}/>}
    </FlexColumn>;
  }
});
