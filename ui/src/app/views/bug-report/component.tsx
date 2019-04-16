import {Component, Input} from '@angular/core';
import * as React from 'react';

import {ReactWrapperBase, withUserProfile} from 'app/utils/index';

import {Button} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {openZendeskWidget} from 'app/utils/zendesk';
import {
  Profile
} from 'generated/fetch';


export const BugReportModal = withUserProfile()
(class extends React.Component<
    {profileState: { profile: Profile, reload: Function },
      bugReportDescription: string, onClose: Function},
    {}> {

  constructor(props) {
    super(props);
  }

  openZendesk() {
    const {profileState: {profile}} = this.props;
    openZendeskWidget(profile.givenName, profile.familyName,
      profile.username, profile.contactEmail);
  }

  render() {
    const {bugReportDescription, onClose} = this.props;
    return <Modal>
      <ModalTitle style={{fontWeight: 400}}>Something went wrong.</ModalTitle>
      <ModalBody>
        {bugReportDescription}
        Please submit a bug report to the help desk.
        <ModalFooter>
          <Button type='secondary' onClick={onClose}>Cancel</Button>
          <Button style={{marginLeft: '0.5rem'}}
                  data-test-id='submit-bug-report'
                  onClick={() => {this.openZendesk(); onClose(); }}>Submit Bug Report</Button>
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
