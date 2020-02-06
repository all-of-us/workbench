import {Button} from 'app/components/buttons';
import {FlexColumn} from 'app/components/flex';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TextColumn} from 'app/components/text-column';
import colors from 'app/styles/colors';
import * as React from 'react';
import {FileDetail, Profile} from '../../../generated/fetch';
import {withUserProfile} from '../../utils';
import {WorkspaceData} from '../../utils/workspace-data';

export const CreateBillingAccountModal = withUserProfile() (
  class extends React.Component<{
    profileState: {profile: Profile},
    onClose: Function
  }, {}> {
    constructor(props) {
      super(props);
    }

    render() {
      return <Modal width={600} onRequestClose={() => this.props.onClose()}>
        <ModalTitle>Create a billing account</ModalTitle>

        <ModalBody>
          <FlexColumn style={{alignItems: 'flex-start'}}>
            <img style={{width: '12rem', marginLeft: '-1.2rem'}} src='/assets/images/logo_lockup_cloud_rgb.png'/>

            <TextColumn>
              <div>Billing accounts are managed by via Google Cloud Platform™ service.</div>
              <div>Learn more on how to set up a billing account.</div>
            </TextColumn>

            <Button type='primary'
                    style={{fontWeight: 400, padding: '0 18px', height: '40px'}}
                    onClick={() => this.props.onClose()}>
              Read Instructions
            </Button>
          </FlexColumn>
        </ModalBody>

        <hr style={{width: '100%', backgroundColor: colors.primary, borderWidth: '0px', height: '1px',
          marginTop: '1rem',
          marginBottom: '0.5rem'}}/>

        <ModalFooter style={{marginTop: 0, justifyContent: 'flex-start'}}>
          <FlexColumn>
            <TextColumn>
              <p style={{marginTop: 0}}>Don't already have a Google Cloud account?</p>
              <a href='https://cloud.google.com' target='_blank'>Create billing account</a>
            </TextColumn>

            <TextColumn>
              <div>Add [ {this.props.profileState.profile.username} ] as a 'User' to your Google Cloud billing account.</div>
              <a href='https://console.cloud.google.com/billing' target='_blank'>Add user</a>
            </TextColumn>
          </FlexColumn>
        </ModalFooter>
      </Modal>;
    }
  }
);
