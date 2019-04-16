import * as React from 'react';

import {Button} from 'app/components/buttons';
import {ResourceCardBase} from 'app/components/card';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {reactStyles} from 'app/utils';
import {environment} from 'environments/environment';
import {baseStyles} from 'app/components/card';
import {ClrIcon} from "../../components/icons";

const styles = reactStyles({
  mainHeader: {
    color: '#FFFFFF', fontSize: 28, fontWeight: 400,
    letterSpacing: 'normal', marginBottom: '1rem'
  },
  cardStyle: {
    boxShadow: '0 0 2px 0 rgba(0,0,0,0.12), 0 3px 2px 0 rgba(0,0,0,0.12)',
    padding: '0.75rem', minHeight: '305px', maxHeight: '305px', maxWidth: '250px',
    minWidth: '250px', justifyContent: 'space-between'
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
  }
});

export interface WorkbenchAccessTasksProps {
  betaAccessGranted: boolean;
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

  private registrationTasks = [
    {
      title: 'Complete Online Training',
      description: 'Researchers must maintain up-to-date completion of compliance ' +
        'training courses hosted at the NNLM\'s Moodle installation',
      buttonText: 'Complete training',
      completedText: 'Completed',
      onClick: WorkbenchAccessTasks.redirectToTraining
    }, {
      title: 'Login to ERA Commons',
      description: 'Researchers must maintain up-to-date completion of compliance' +
        ' training courses hosted at the NNLM’s Moodle installation',
      buttonText: 'Login',
      completedText: 'Linked',
      onClick: WorkbenchAccessTasks.redirectToNiH
    }
  ];

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
    const {eraCommonsLinked, eraCommonsError, trainingCompleted, betaAccessGranted} = this.props;
    return <div style={{display: 'flex', flexDirection: 'column', paddingTop: '3%', paddingLeft: '3%'}}
                data-test-id='access-tasks'>
      <div style={styles.mainHeader}>Researcher Workbench</div>
      {!betaAccessGranted && <div style={{...baseStyles.card, ...styles.warningModal}}>
        <ClrIcon shape='warning-standard' class='is-solid'
                 style={styles.warningIcon}/>
        You have not been granted beta access. Please contact help@research-aou.org.
      </div>}

      <div style={{display: 'flex', flexDirection: 'row', margin: '3%'}}>
        {this.registrationTasks.map((card, i) => {
          return <ResourceCardBase style={styles.cardStyle}>
            <div style={{display: 'flex', flexDirection: 'column', justifyContent: 'flex-start'}}>
              <div style={styles.cardHeader}>STEP {i + 1}</div>
              <div style={styles.cardHeader}>{card.title}</div>
            </div>
            <div style={styles.cardDescription}>{card.description}</div>
            <Button type='darklingSecondary'
                    onClick={() => card.onClick}>
              {card.buttonText}
            </Button>
          </ResourceCardBase>;
        })}
      </div>
    </div>;
  }
}
