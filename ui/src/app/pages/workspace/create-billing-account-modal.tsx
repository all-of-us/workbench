import {faTimes} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow, FlexRowWrap} from 'app/components/flex';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TextColumn} from 'app/components/text-column';
import colors from 'app/styles/colors';
import {supportUrls} from 'app/utils/zendesk';
import * as React from 'react';
import {useState} from 'react';
import {ProfileStore, profileStore, useStore} from '../../utils/stores';
import {TextInput} from "../../components/inputs";
import {reactStyles} from "../../utils";

export const styles = reactStyles({
  line: {
    boxSizing: 'border-box',
    height: '100%',
    // Set relative positioning so the spinner overlay is centered in the card.
    position: 'relative'
  },
  textInput: {
    width: '75%',
    padding: '0 0 0 5px',
    border: 0,
    backgroundColor: 'transparent',
    outline: 'none',
  },
})

export interface CreateBillingAccountState {
  currentStep: number;
  showBillingAccountDescription: boolean;
  showSubmitInstruction: boolean;
  userFullName: string;
  userPhoneNumber: string;
}

export interface Props {
  onClose: Function;
}

export const CreateBillingAccountModal = ({onClose}:Props) => {
  const {profile: {
    givenName,
      familyName,
      verifiedInstitutionalAffiliation,
      username
  }} = useStore(profileStore);
  const [currentStep, setCurrentStep] = useState(1);
  const [phoneNumber, setPhoneNumber] = useState();

  return <Modal width={600} onRequestClose={() => onClose()}>
        <ModalTitle style={{marginBottom: '0.7rem'}}>Create a billing account</ModalTitle>

        <ModalBody>
          <FlexColumn style={{alignItems: 'flex-start'}}>
            <img style={{width: '12rem', marginLeft: '-0.7rem'}} src='/assets/images/gcp_logo.png'/>

            <TextColumn>
              <div>Billing accounts are managed via Google Cloud Platform™ service.</div>
              <div>Learn more on how to set up a billing account.</div>
            </TextColumn>

            <Button type='primary'
                    style={{fontWeight: 400, padding: '0 18px', height: '40px'}}
                    onClick={() => window.open(supportUrls.createBillingAccount, '_blank')}>
              Read Instructions
            </Button>
          </FlexColumn>
        </ModalBody>

        <hr style={{
          width: '100%', backgroundColor: colors.primary, borderWidth: '0px', height: '1px',
          marginTop: '1rem',
          marginBottom: '0.5rem'
        }}/>

    {currentStep === 1 && <ModalFooter style={{marginTop: 0, justifyContent: 'flex-start'}}>
      <FlexRow>
        <FlexColumn>
          <TextColumn>
            <p style={{marginTop: 0}}>Familiar with setting up a Google Cloud Platform account?</p>
            <p style={{marginTop: 0}}>Manually set up an account in GCP. Step-by-step directions</p>
          </TextColumn>
          <Button type='primary'
                  style={{fontWeight: 400, padding: '0 18px', height: '40px'}}
                  onClick={() => window.open(supportUrls.createBillingAccount, '_blank')}>
            Read Directions
          </Button>
        </FlexColumn>
        <FlexColumn>
          <TextColumn>
            <p style={{marginTop: 0}}>Let a Google billing partner create the account for you.</p>
            <p style={{marginTop: 0}}>A representative will help you set up your billing account.</p>
          </TextColumn>
          <Button type='primary'
                  style={{fontWeight: 400, padding: '0 18px', height: '40px'}}
                  onClick={() => this.setState({currentStep: 2})}>
            Get Started
          </Button>
        </FlexColumn>
      </FlexRow>
    </ModalFooter>}
    {currentStep === 2 && <ModalFooter style={{marginTop: 0, justifyContent: 'flex-start'}}>
      <h2>Your information</h2>
      <FlexRow>
        <FlexColumn>Your name</FlexColumn>
        <FlexColumn>
          <TextInput
              data-test-id='user-full-name'
              style={styles.textInput}
              onChange={(v) => this.setState({userFullName: v})}
              value={givenName + ' ' + familyName}/>
        </FlexColumn>
        <FlexColumn>Your phone number</FlexColumn>
        <FlexColumn>
          <TextInput
              data-test-id='user-phone-number'
              style={styles.textInput}
              onChange={(v) => this.setState({userPhoneNumber: v})}
              value={givenName + ' ' + familyName}/>
        </FlexColumn>
      </FlexRow>
      <FlexColumn>
        <Button type='primary'
                style={{fontWeight: 400, padding: '0 18px', height: '40px'}}
                onClick={() => this.props.onClose()}>
          Cancel
        </Button>
        <Button type='primary'
                style={{fontWeight: 400, padding: '0 18px', height: '40px'}}
                onClick={() => this.setState({currentStep: 3})}>
          Next
        </Button>
      </FlexColumn>
    </ModalFooter>}
    {currentStep === 3 && <ModalFooter style={{marginTop: 0, justifyContent: 'flex-start'}}>
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
    </ModalFooter>}
    {currentStep === 4 && <ModalFooter style={{marginTop: 0, justifyContent: 'flex-start'}}>
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
    </ModalFooter>}

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
            onClick={() => onClose()}
        />
      </Modal>;
};
