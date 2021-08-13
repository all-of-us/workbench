import {faTimes} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow, FlexRowWrap} from 'app/components/flex';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TextColumn} from 'app/components/text-column';
import colors from 'app/styles/colors';
import {supportUrls} from 'app/utils/zendesk';
import * as React from 'react';
import {GoogleCloudLogoSvg} from 'app/components/icons';
import {useState} from 'react';
import {ProfileStore, profileStore, useStore} from '../../utils/stores';
import {TextInput} from "../../components/inputs";
import {reactStyles} from "../../utils";

export const styles = reactStyles({
  line: {
    borderLeft: `1px solid #97979`,
    boxSizing: 'border-box',
    height: '34px',
    width: '1px',
  },
  textInput: {
    width: '75%',
    padding: '0 0 0 5px',
    border: 0,
    backgroundColor: 'transparent',
    outline: 'none',
  },
  textHeader: {
    color: '#262262',
    fontFamily: 'Montserrat',
    fontSize: '16px',
    fontWeight: 600,
    lineHeight: '20px',
    letterSpacing: '0',
  },
  textNormal: {
    color: '#262262',
    fontFamily: 'Montserrat',
    fontSize: '14px',
    lineHeight: '22px',
    letterSpacing: '0',
  },
})

const stylesFunction = {
  stepButtonCircle: (alert: boolean): React.CSSProperties => {
    return {
      border: 'solid 1px',
      borderRadius: '50%',
      height: '50px',
      width: '50px',
      marginLeft: '12px',
      padding: '4px',
      backgroundColor: alert ? colors.danger : colors.secondary,
    };
  }
};

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

  return <Modal width={650} onRequestClose={() => onClose()}>
        <ModalBody style={{marginTop: '0'}}>
          <FlexColumn>
            <FlexRow style={{alignItems: 'center'}}>
              <GoogleCloudLogoSvg style={{height: '33px', width: '207px'}}/>
              <div style={styles.line}></div>
              <div style={{paddingTop: 5}}><div style={styles.textHeader}>Create billing account</div></div>
              {currentStep !== 1 && <FlexRow style={{alignItems: 'center'}}>

              </FlexRow>}
            </FlexRow>
            <TextColumn>
              <div style={styles.textNormal}>Billing accounts are managed via Google Cloud Platform™ service.</div>
              <div style={styles.textNormal}>Learn more on how to set up a billing account.</div>
            </TextColumn>
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
            <p style={styles.textHeader}>Familiar with setting up a Google Cloud Platform account?</p>
            <p style={styles.textNormal}>Manually set up an account in GCP. Step-by-step directions</p>
          </TextColumn>
          <Button type='primary'
                  style={{fontWeight: 400, padding: '0 18px', height: '40px'}}
                  onClick={() => window.open(supportUrls.createBillingAccount, '_blank')}>
            Read Directions
          </Button>
        </FlexColumn>
        <FlexColumn>
          <TextColumn>
            <p style={styles.textHeader}>Let a Google billing partner create the account for you.</p>
            <p style={styles.textNormal}>A representative will help you set up your billing account.</p>
          </TextColumn>
          <Button type='primary'
                  style={{fontWeight: 400, padding: '0 18px', height: '40px'}}
                  onClick={() => {setCurrentStep(2)}}>
            USE A BILLING PARTNER
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
                onClick={() => {setCurrentStep(2)}}>
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
