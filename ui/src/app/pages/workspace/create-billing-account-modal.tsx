import {Button} from 'app/components/buttons';
import {FlexColumn} from 'app/components/flex';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TextColumn} from 'app/components/text-column';
import colors from 'app/styles/colors';
import * as React from 'react';

export const CreateBillingAccountModal = ({onClose}) => {
  return <Modal width={600} onRequestClose={() => onClose()}>
    <ModalTitle>Create a billing account</ModalTitle>

    <ModalBody>
      <FlexColumn style={{alignItems: 'flex-start'}}>
        <img style={{width: '12rem', marginLeft: '-1.2rem'}} src='/assets/images/logo_lockup_cloud_rgb.png'/>

        <TextColumn>
          <div>Billing accounts are managed by via Google Cloud Platform.</div>
          <div>Learn more on how to set up a billing account.</div>
        </TextColumn>

        <Button type='primary'
                style={{fontWeight: 400, padding: '0 18px', height: '40px'}}
                onClick={() => onClose()}>
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
          <p style={{marginTop: 0}}>If you do not have a Google Cloud billing account.</p>
          <a href='https://cloud.google.com' target='_blank'>Create billing account</a>
        </TextColumn>

        <TextColumn>
          <div>Add your <i>All of Us</i> user account to your existing Google Cloud account.</div>
          <a href='https://console.cloud.google.com/billing' target='_blank'>Add your account</a>
        </TextColumn>
      </FlexColumn>
    </ModalFooter>
  </Modal>;
};
