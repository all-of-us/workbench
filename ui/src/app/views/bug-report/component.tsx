import {Component, Input} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {validate} from 'validate.js';

import {bugReportApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, summarizeErrors, withUserProfile} from 'app/utils/index';

import {AlertDanger} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {TextArea, TextInput, ValidationError} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {
  BugReport,
  BugReportType,
  Profile
} from 'generated/fetch';


const styles = reactStyles({
  fieldHeader: {
    fontSize: 14,
    color: '#262262',
    fontWeight: 200,
    display: 'block',
    marginTop: '1rem'
  },
  field: {
    fontWeight: 400,
    width: '100%'
  }
});

export const BugReportModal = withUserProfile()
(class extends React.Component<
    {profileState: { profile: Profile, reload: Function },
      bugReportDescription: string, onClose: Function},
    {bugReport: BugReport, submitting: boolean,
      sendBugReportError: boolean}> {

  private emptyBugReport = {
    bugType: BugReportType.APPLICATION,
    shortDescription: '',
    reproSteps: '',
    includeNotebookLogs: true,
    contactEmail: ''
  };

  constructor(props) {
    super(props);
    this.state = {
      submitting: false,
      sendBugReportError: false,
      bugReport: {...this.emptyBugReport,
        shortDescription: props.bugReportDescription || '',
        contactEmail: props.profileState.profile.username
      }
    };
  }

  send() {
    this.setState({submitting: true, sendBugReportError: false});
    bugReportApi().sendBugReport(this.state.bugReport).then(() => {
      this.setState({submitting: false});
      this.props.onClose();
    }).catch(() => {
      this.setState({sendBugReportError: true, submitting: false});
    });
  }

  updateBugReport(attribute: string, value: any) {
    const newReport = this.state.bugReport;
    newReport[attribute] = value;
    this.setState(({bugReport}) => ({bugReport: fp.set(attribute, value, bugReport)}));
  }

  render() {
    const {bugReport, bugReport: {shortDescription}, submitting, sendBugReportError} = this.state;
    const {onClose} = this.props;
    const errors = validate({shortDescription}, {
      shortDescription: {presence: {allowEmpty: false}}
    });

    return <Modal width={700} loading={submitting}>
      <ModalTitle style={{fontWeight: 400}}>Submit a Bug</ModalTitle>
      <ModalBody>
        <label style={styles.fieldHeader}>Description: </label>
        <TextInput value={shortDescription}
                    onChange={v => this.updateBugReport('shortDescription', v)}/>
        <ValidationError>
          {summarizeErrors(errors && errors.shortDescription)}
        </ValidationError>
        <label style={styles.fieldHeader}>Bug Type: </label>
        <select style={{height: '1.5rem', width: '100%'}}
                onChange={e => this.updateBugReport('bugReportType', e.target.value)}>
          <option value={BugReportType.APPLICATION}>Workbench Application</option>
          <option value={BugReportType.DATA}>Data Quality</option>
        </select>
        <label style={styles.fieldHeader}>How to Reproduce: </label>
        <TextArea onChange={v => this.updateBugReport('reproSteps', v) }/>
        <div style={{display: 'flex', flexDirection: 'row', marginTop: '1rem'}}>
          <input type='checkbox'
                 defaultChecked={bugReport.includeNotebookLogs}
                 onClick={() => this.updateBugReport('includeNotebookLogs',
                   !bugReport.includeNotebookLogs)}
          />
          <label style={{...styles.fieldHeader, marginTop: '0rem', marginLeft: '0.2rem'}}>
            Attach notebook logs? </label>
        </div>
        <label style={styles.fieldHeader}>Contact Email: </label>
        <TextInput value={bugReport.contactEmail}
                    onChange={e => this.updateBugReport('contactEmail', e)}/>
        {sendBugReportError && <AlertDanger style={{justifyContent: 'space-between'}}>
          <div>Error sending the bug report.</div>
          <ClrIcon shape='times'
                   onClick={() => this.setState({sendBugReportError: false})}/>
        </AlertDanger>}
        <ModalFooter>
          <Button type='secondary' onClick={onClose}>Cancel</Button>
          <Button style={{marginLeft: '0.5rem'}}
                  disabled={!!sendBugReportError || !!errors || submitting}
                  data-test-id='submit-bug-report'
                  onClick={() => this.send()}>Send</Button>
        </ModalFooter>
      </ModalBody>
    </Modal>;
  }

});


@Component({
  selector: 'app-bug-report',
  template: '<div #root></div>'
})
export class BugReportComponent extends ReactWrapperBase {
  @Input('bugReportDescription') bugReportDescription: string;
  @Input('onClose') onClose: Function;
  constructor() {
    super(BugReportModal, ['bugReportDescription', 'onClose']);
  }

}
