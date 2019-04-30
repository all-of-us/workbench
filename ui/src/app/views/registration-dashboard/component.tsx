import * as React from 'react';

import {AlertClose, AlertDanger, AlertWarning} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {ResourceCardBase} from 'app/components/card';
import {baseStyles} from 'app/components/card';
import {ClrIcon} from 'app/components/icons';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {reactStyles} from 'app/utils';
import {navigate} from 'app/utils/navigation';
import {environment} from 'environments/environment';

const styles = reactStyles({
  registrationPage: {
    display: 'flex', flexDirection: 'column', paddingTop: '3%', paddingLeft: '3%'
  },
  mainHeader: {
    color: '#FFFFFF', fontSize: 28, fontWeight: 400,
    letterSpacing: 'normal', marginBottom: '0.2rem'
  },
  cardStyle: {
    boxShadow: '0 0 2px 0 rgba(0,0,0,0.12), 0 3px 2px 0 rgba(0,0,0,0.12)',
    padding: '0.75rem', minHeight: '305px', maxHeight: '305px', maxWidth: '250px',
    minWidth: '250px', justifyContent: 'space-between', backgroundColor: '#fff'
  },
  cardHeader: {
    color: '#262262', fontSize: '16px', lineHeight: '19px', fontWeight: 600,
    marginBottom: '0.5rem'
  },
  cardDescription: {
    color: '#262262', fontSize: '14px', lineHeight: '20px'
  },
  infoBoxButton: {
    color: '#FFFFFF', height: '49px', borderRadius: '5px', marginLeft: '1rem',
    maxWidth: '20rem'
  },
  warningIcon: {
    color: '#F7981C', position: 'relative', top: 'calc(50% - 10px)',
    height: '20px', width: '20px'
  },
  warningModal: {
    color: '#262262', fontSize: '18px', lineHeight: '28px', flexDirection: 'row',
    boxShadow: 'none', fontWeight: 600, display: 'flex', justifyContent: 'center'
  },
  closeableWarning: {
    margin: '0px 1rem 1rem 0px', display: 'flex', justifyContent: 'space-between'
  }
});

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

export const RegistrationTasks = [
  {
    title: 'Turn on Google 2-Step Verification',
    description: 'With 2-Step Verification, you’ll protect your ' +
      'account with both your password and your phone',
    buttonText: 'Get Started',
    completedText: 'Completed',
    isRefreshable: true,
    onClick: () => window.open('https://myaccount.google.com/security', '_blank')
  }, {
    title: 'Complete Online Training',
    description: 'Researchers must maintain up-to-date completion of compliance ' +
      'training courses hosted at the NNLM\'s Moodle installation',
    buttonText: 'Complete training',
    completedText: 'Completed',
    onClick: redirectToTraining
  }, {
    title: 'Login to ERA Commons',
    description: 'Researchers must maintain up-to-date completion of compliance' +
      ' training courses hosted at the NNLM’s Moodle installation',
    buttonText: 'Login',
    completedText: 'Linked',
    onClick: redirectToNiH
  }, {
    title: 'Data Use Agreement',
    description: 'This data use agreement describes how All of Us ' +
      'Research Program data can and cannot be used',
    buttonText: 'View & Sign',
    completedText: 'Signed',
    onClick: () => navigate(['data-use-agreement'])
  }
];

export interface RegistrationDashboardProps {
  betaAccessGranted: boolean;
  eraCommonsLinked: boolean;
  eraCommonsError: string;
  trainingCompleted: boolean;
  firstVisitTraining: boolean;
}

interface State {
  showRefreshButton: boolean,
  trainingWarningOpen: boolean,
  taskCompletionMap: Map<number, boolean>,
}

export class RegistrationDashboard extends React.Component<RegistrationDashboardProps, State> {

  constructor(props: RegistrationDashboardProps) {
    super(props);
    this.state = {
      trainingWarningOpen: !props.firstVisitTraining,
      taskCompletionMap: new Map<number, boolean>(),
      showRefreshButton: false
    };
    this.state.taskCompletionMap.set(0, props.trainingCompleted);
    this.state.taskCompletionMap.set(1, props.eraCommonsLinked);
  }

  componentDidMount() {
    this.setState({showRefreshButton: false});
  }

  isEnabled(i: number): boolean {
    const {taskCompletionMap} = this.state;
    if (i === 0) {
      return !taskCompletionMap.get(i);
    } else {
      return !taskCompletionMap.get(i) && !this.isEnabled(i - 1);
    }
  }

  allTasksCompleted(): boolean {
    return Array.from(this.state.taskCompletionMap.values()).reduce((acc, val) => acc && val);
  }

  render() {
    const {taskCompletionMap, trainingWarningOpen} = this.state;
    const {betaAccessGranted, eraCommonsError, trainingCompleted} = this.props;
    return <div style={styles.registrationPage}
                data-test-id='registration-dashboard'>
      <div style={styles.mainHeader}>Researcher Workbench</div>
      <div style={{...styles.mainHeader, fontSize: '18px', marginBottom: '1rem'}}>
        <ClrIcon shape='warning-standard' class='is-solid'
                 style={{color: '#fff', marginRight: '0.3rem'}}/>
        In order to get access to data and tools please complete the following steps:
      </div>
      {!betaAccessGranted && <div data-test-id='beta-access-warning'
                                  style={{...baseStyles.card, ...styles.warningModal}}>
        <ClrIcon shape='warning-standard' class='is-solid'
                 style={styles.warningIcon}/>
        You have not been granted beta access. Please contact support@researchallofus.org.
      </div>}

      <div style={{display: 'flex', flexDirection: 'row'}}>
        {RegistrationTasks.map((card, i) => {
          return <ResourceCardBase key={i} data-test-id={'registration-task-' + i.toString()}
            style={this.isEnabled(i) ? styles.cardStyle : {...styles.cardStyle,
              opacity: '0.6', maxHeight: this.allTasksCompleted() ? '160px' : '305px',
              minHeight: this.allTasksCompleted() ? '160px' : '305px'}}>
            <div style={{display: 'flex', flexDirection: 'column', justifyContent: 'flex-start'}}>
              <div style={styles.cardHeader}>STEP {i + 1}</div>
              <div style={styles.cardHeader}>{card.title}</div>
            </div>
            {!this.allTasksCompleted() &&
            <div style={styles.cardDescription}>{card.description}</div>}
            {taskCompletionMap.get(i) ?
              <Button disabled={true} data-test-id='completed-button'
                      style={{backgroundColor: '#8BC990', width: 'max-content', cursor: 'default'}}>
                <ClrIcon shape='check' style={{marginRight: '0.3rem'}}/>{card.completedText}
              </Button> :
            <Button type='darklingSecondary'
                    onClick={this.showRefreshFlow(card.isRefreshable) ?
                      () => window.location.reload() : () => { if (card.isRefreshable) { this.setState({showRefreshButton: true}); }  card.onClick(); }}
                    style={{width: 'max-content',
                      cursor: this.isEnabled(i) ? 'pointer' : 'default'}}
                    disabled={!this.isEnabled(i)} data-test-id='registration-task-link'>
              {card.buttonText}
            </Button>}
          </ResourceCardBase>;
        })}
      </div>
      {eraCommonsError && <AlertDanger data-test-id='era-commons-error'
                                        style={{margin: '0px 1rem 1rem 0px'}}>
          <ClrIcon shape='exclamation-triangle' class='is-solid'/>
          Error Linking NIH Username: {eraCommonsError} Please try again!
      </AlertDanger>}
      {trainingWarningOpen && !trainingCompleted &&
      <AlertWarning style={styles.closeableWarning}>
        <div style={{display: 'flex'}}>
          <ClrIcon shape='exclamation-triangle' class='is-solid'/>
          <div>It may take several minutes for Moodle to update your Online Training
            status once you have completed compliance training.</div>
        </div>
        <AlertClose onClick={() => this.setState({trainingWarningOpen: false})}/>
      </AlertWarning>}
      {(this.allTasksCompleted() && betaAccessGranted) &&
      <div style={{...baseStyles.card, ...styles.warningModal}} data-test-id='success-message'>
        You successfully completed all the required steps to access the Research Workbench.
        <Button style={{marginLeft: '0.5rem'}}
                onClick={() => window.location.reload()}>Get Started</Button>
      </div>}
    </div>;
  }
}
