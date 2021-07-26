import {faTimes} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRowWrap} from 'app/components/flex';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TextColumn} from 'app/components/text-column';
import colors from 'app/styles/colors';
import {withUserProfile} from 'app/utils';
import {supportUrls} from 'app/utils/zendesk';
import {Profile} from 'generated/fetch';
import * as React from 'react';

export const CreateBillingAccountModal1 = withUserProfile() (
  class extends React.Component<{
    profileState: {profile: Profile},
    onClose: Function
  }, {}> {
    constructor(props) {
      super(props);
    }

    render() {
      return <Modal width={600} onRequestClose={() => this.props.onClose()}>
        <ModalTitle style={{marginBottom: '0.7rem'}}>Create a billing account
          <FlexRowWrap>
            <FlexColumn style={{alignItems: 'flex-start'}}>
              <img style={{width: '12rem', marginLeft: '-0.7rem'}} src='/assets/images/gcp_logo.png'/>
            </FlexColumn>
            <FlexColumn style={{alignItems: 'flex-start'}}>
              <img style={{width: '12rem', marginLeft: '-0.7rem'}} src='/assets/images/gcp_logo.png'/>
            </FlexColumn>
          </FlexRowWrap>
        </ModalTitle>
        <ModalBody>
          <FlexColumn style={{alignItems: 'flex-start'}}>
            <img style={{width: '12rem', marginLeft: '-0.7rem'}} src='/assets/images/gcp_logo.png'/>
            <TextColumn>
              <div>Submit your information below to receive a billing and additional information from
                a Google billing partner representative.
              </div>
            </TextColumn>
            <Button type='primary'
                    style={{fontWeight: 400, padding: '0 18px', height: '40px'}}
                    onClick={() => window.open(supportUrls.createBillingAccount, '_blank')}>
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
              <a href='https://cloud.google.com' target='_blank'>Create an account</a>
            </TextColumn>

            <TextColumn>
              <div>Add [ {this.props.profileState.profile.username} ] as a 'User' to your Google Cloud billing account.</div>
              <a href='https://console.cloud.google.com/billing' target='_blank'>Add user</a>
            </TextColumn>
          </FlexColumn>
        </ModalFooter>

        <FontAwesomeIcon
          icon={faTimes}
          size='lg'
          style={{
            color: colors.accent,
            position: 'absolute',
            top: '1rem',
            right: '1rem',
            cursor: 'pointer'
          }}
          onClick={() => this.props.onClose()}
        />
      </Modal>;
    }
  }
);
