import {faTimes} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {GoogleCloudLogoSvg} from 'app/components/icons';
import {CheckBox, RadioButton, TextInput} from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  withErrorModal
} from 'app/components/modals';
import {TextColumn} from 'app/components/text-column';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {switchCase} from 'app/utils';
import {reactStyles} from 'app/utils';
import {profileStore, useStore} from 'app/utils/stores';
import {supportUrls} from 'app/utils/zendesk';
import {BillingPaymentMethod} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {useState} from 'react';

export const styles = reactStyles({
  line: {
    borderLeft: '1px solid #979797',
    boxSizing: 'border-box',
    height: '34px',
    width: '1px',
    marginLeft: '2px',
    marginRight: '2px',
  },
  textInput: {
    width: '290px',
    height: '40px',
    border: '1px solid rgba(74,74,74,0.3)',
    borderRadius: '3px',
    backgroundColor: '#FFFFFF',
    marginRight: '20px',
  },
  textHeader: {
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: '16px',
    fontWeight: 600,
    lineHeight: '20px',
    letterSpacing: '0',
    marginTop: '5px',
  },
  textNormal: {
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: '14px',
    lineHeight: '22px',
    letterSpacing: '0',
    marginTop: '5px',
  },
  radioButton: {
    margin: '15px',
    lineHeight: '22px',
    letterSpacing: '0',
    marginTop: '15px',
    flexShrink: 0,
    width: '17px',
    height: '17px',
  },
});

const stylesFunction = {
  stepButtonCircle: (currentStep: number, buttonStep: number): React.CSSProperties => {
    return {
      visibility: currentStep === 0 || currentStep === 4 ? 'hidden' : 'visible',
      borderRadius: '50%',
      height: '37px',
      width: '37px',
      opacity: currentStep === buttonStep ? '1.0' :  '0.2',
      backgroundColor: currentStep === buttonStep ? colors.primary : '#333F52',
      fontFamily: 'Montserrat',
      textAlign: 'center',
      lineHeight: '37px',
      fontSize: '18px',
      fontWeight: 600,
      letterSpacing: '0',
      color: '#FFFFFF',
      marginRight: '10px',
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

const BillingConfirmItem = ({title, value, dataTestId}) => {
  return <FlexRow id = {`${dataTestId}-wrapper`} style={{marginTop: '5px'}}>
    <div style={{width: '170px'}}>{title}:</div>
    <div data-test-id={dataTestId}>{value}</div>
  </FlexRow>;
};

export const CreateBillingAccountModal = ({onClose}: Props) => {
  const {profile: {
    contactEmail,
    givenName,
    familyName,
    verifiedInstitutionalAffiliation,
    username
  }} = useStore(profileStore);
  const [currentStep, setCurrentStep] = useState(0);
  const [phoneNumber, setPhoneNumber] = useState<string>();
  const [invalidPhoneNumberInput, setInvalidPhoneNumberInput] = useState<boolean>(null);
  const [useCreditCard, setUseCreditCard] = useState<boolean>(null);
  const [nihFunded, setNihFunded] = useState<boolean>(null);

  const validatePhoneNumber = (phoneInput: string) => {
    const sanitizedPhone = phoneInput.replace(/\D/g, '');

    if (sanitizedPhone.length >= 10 && sanitizedPhone.length <= 11) {
      setInvalidPhoneNumberInput(false);
      setPhoneNumber(sanitizedPhone);
    } else {
      setInvalidPhoneNumberInput(true);
    }
  };

  const sendCreateBillingEmail = fp.flow(
    withErrorModal({
      title: 'Failed To Send Email',
      message: 'An error occurred trying to send email. Please try again.',
    })
  )(async() => {await profileApi().sendBillingSetupEmail(
    {
      phone: phoneNumber,
      paymentMethod: useCreditCard ? BillingPaymentMethod.CREDITCARD : BillingPaymentMethod.PURCHASEORDER,
      isNihFunded: nihFunded,
      institution: verifiedInstitutionalAffiliation.institutionDisplayName
    }
  );
    setCurrentStep(4);
  });

  return <Modal width={650} onRequestClose={() => onClose()}>
        <ModalBody>
          <FlexColumn>
            <FlexRow style={{alignItems: 'center', width: '620px', marginBottom: '0.8rem'}}>
              <GoogleCloudLogoSvg style={{height: '33px', width: '207px', marginLeft: '-0.5rem', marginRight: '0.5rem'}}/>
              <div style={styles.line}></div>
              <div style={{paddingTop: 5, marginLeft: '1rem', marginRight: '2rem'}}>
                <div style={styles.textHeader}>Create billing account</div>
              </div>
              {fp.range(1, 4).map((i) => <div style={stylesFunction.stepButtonCircle(currentStep, i)}>{i}</div>)}
            </FlexRow>
            {currentStep === 0 && <TextColumn>
              <div style={styles.textNormal}>Billing accounts are managed via Google Cloud Platform™ service.</div>
              <div style={styles.textNormal}><a href={supportUrls.createBillingAccount}>Learn more</a>
                &nbsp;on how to set up a billing account.
              </div>
            </TextColumn>}
            {currentStep !== 0 && currentStep !== 4 && <TextColumn>
              <div style={styles.textNormal}>Submit your information below to receive billing and additional information from
                a Google billing partner representative.</div>
            </TextColumn>}
          </FlexColumn>
        </ModalBody>
        <hr style={{
          width: '100%', backgroundColor: '#979797', borderWidth: '0px', height: '1px',
          marginTop: '0.5rem',
          marginBottom: '0.5rem'
        }}/>
    <ModalFooter data-test-id={`step-${currentStep}-modal`} style={{marginTop: 0, justifyContent: 'flex-start'}}>
    {switchCase(currentStep,
      [0, () => (<FlexRow style={{justifyContent: 'space-evenly'}}>
      <FlexColumn>
        <TextColumn>
          <p style={styles.textHeader}>Familiar with setting up a Google Cloud Platform account?</p>
          <p style={styles.textNormal}>Manually set up an account in <br/> Google Cloud Platform.</p>
        </TextColumn>
        <Button type='primary'
                style={{marginTop: '0.5rem', fontWeight: 500, fontSize: '14px', height: '39px', width: '188px'}}
                onClick={() => window.open(supportUrls.createBillingAccount, '_blank')}>
          Read Directions
        </Button>
      </FlexColumn>
      <FlexColumn>
        <TextColumn>
          <p style={styles.textHeader}>Let a Google billing partner create the account for you.</p>
          <p style={styles.textNormal}>A representative will help you set up <br/>your billing account.</p>
        </TextColumn>
        <Button data-test-id='use-billing-partner-button'
                type='primary'
                style={{marginTop: '0.5rem', fontWeight: 500, fontSize: '14px', height: '39px', width: '220px'}}
                onClick={() => {setCurrentStep(1); }}>
          USE A BILLING PARTNER
        </Button>
       </FlexColumn>
      </FlexRow>)
      ], [1, () => (
        <FlexColumn style={{justifyContent: 'space-evenly', width: '37rem'}}>
          <div style={styles.textHeader}>Your Information</div>
          <FlexRow style={{marginTop: '20px'}}>
            <FlexColumn style={styles.textNormal}>
              Your name
              <TextInput
                  data-test-id='user-full-name'
                  style={styles.textInput}
                  disabled={true}
                  value={givenName + ' ' + familyName}/>
            </FlexColumn>
            <FlexColumn style={styles.textNormal}>
              Your phone number
              <TextInput
                  data-test-id='user-phone-number'
                  style={styles.textInput}
                  onChange={(v) => validatePhoneNumber(v)}/>
              {invalidPhoneNumberInput && <div data-test-id='invalidPhoneNumber' style={{color: colors.danger}}>
                Invalid phone number input
              </div>}
            </FlexColumn>
          </FlexRow>
          <FlexRow style={{marginTop: '20px'}}>
            <FlexColumn style={styles.textNormal}>
              Your contact email address
              <TextInput
                  data-test-id='user-contact-email'
                  disabled={true}
                  style={styles.textInput}
                  value={contactEmail}/>
            </FlexColumn>
            <FlexColumn style={styles.textNormal}>
              Your researchallofus.org ID
              <TextInput
                  data-test-id='user-workbench-id'
                  style={styles.textInput}
                  disabled={true}
                  value={username}/>
            </FlexColumn>
          </FlexRow>
          <FlexRow style={{marginTop: '20px'}}>
            <FlexColumn style={styles.textNormal}>
              Your institution
              <TextInput
                  data-test-id='user-institution'
                  style={styles.textInput}
                  disabled={true}
                  value={verifiedInstitutionalAffiliation.institutionDisplayName}/>
            </FlexColumn>
          </FlexRow>
          <FlexRow style={{marginTop: '100px', justifyContent: 'flex-end'}}>
            <Button type='secondary'
                    style={{fontWeight: 400, padding: '0 18px', height: '40px', marginRight: '10px'}}
                    onClick={() => onClose()}>
              Cancel
            </Button>
            <Button data-test-id='next-button'
                    type='primary'
                    style={{fontWeight: 400, padding: '0 18px', height: '40px', width: '93px'}}
                    disabled={!phoneNumber}
                    onClick={() => {setCurrentStep(2); }}>
              Next
            </Button>
          </FlexRow>
        </FlexColumn>)],
      [
        2, () => (
        <FlexColumn style={{justifyContent: 'space-evenly', width: '100%'}}>
          <div style={styles.textHeader}>What payment method would you like to use?</div>
          <FlexColumn style={{marginTop: '20px', width: '100%'}}>
            <FlexRow style={{boxSizing: 'border-box', borderRadius: '8px', border: '1px solid #CCCFD4', marginBottom: '7px'}}>
              <RadioButton data-test-id='credit-card-radio'
                           style={styles.radioButton}
                           checked={useCreditCard === true}
                           onChange={() => setUseCreditCard(true)}/>
              <FlexColumn style={{marginTop: '9px', marginLeft: '15px', marginBottom: '15px'}}>
                <FlexRow>
                  <div style={styles.textHeader}>Credit Card&nbsp;&nbsp;</div>
                  <i style={styles.textNormal}>24 hours to process</i>
                </FlexRow>
                <div style={styles.textNormal}>A Google billing partner representative will contact you to process
                  your request.</div>
              </FlexColumn>
            </FlexRow>
            <FlexRow style={{boxSizing: 'border-box', borderRadius: '8px', border: '1px solid #CCCFD4', marginBottom: '7px'}}>
              <RadioButton
                  style={styles.radioButton}
                  checked={useCreditCard === false}
                  onChange={() => setUseCreditCard(false)}/>
              <FlexColumn style={{marginTop: '9px', marginLeft: '15px', marginBottom: '15px'}}>
                <FlexRow>
                  <div style={styles.textHeader}>Purchase Order/Other&nbsp;&nbsp;</div>
                  <i style={styles.textNormal}>5-7 days to process</i>
                </FlexRow>
                <div style={styles.textNormal}>You will need to provide more info for the quote, a
                  Google billing partner representative will contact you to process your request.
                </div>
              </FlexColumn>
            </FlexRow>
            <FlexRow style={{marginTop: '10px'}}>
              <CheckBox style={styles.radioButton}
                        checked={nihFunded === true}
                        onChange={(v) => setNihFunded(v)}/>
              <FlexColumn style={{marginTop: '9px', marginLeft: '15px', marginBottom: '15px'}}>
                <div style={styles.textHeader}>NIH-funded Research.</div>
                <div style={styles.textNormal}>My research is funded by the National Institute of Health, (NIH).
                  NIH funded research is eligible for discounted cloud rates through
                  the NIH STRIDES initiative.
                </div>
              </FlexColumn>
            </FlexRow>
          </FlexColumn>
          <FlexRow style={{marginTop: '100px', justifyContent: 'space-between'}}>
            <Button type='secondary'
                    style={{fontWeight: 400, padding: '0 18px', height: '40px'}}
                    onClick={() => {setCurrentStep(1); }}>
              Back
            </Button>
            <FlexRow style={{justifyContent: 'flex-end'}}>
              <Button type='secondary'
                      style={{fontWeight: 400, padding: '0 18px', height: '40px', marginRight: '10px'}}
                      onClick={() => onClose()}>
                Cancel
              </Button>
              <Button data-test-id='next-button'
                      type='primary'
                      style={{fontWeight: 400, padding: '0 18px', height: '40px', width: '93px'}}
                      disabled={useCreditCard === null}
                      onClick={() => {setCurrentStep(3); }}>
                Next
              </Button>
            </FlexRow>
          </FlexRow>
        </FlexColumn>)
      ], [
        3, () => (
        <FlexColumn style={{width: '100%'}}>
          <div style={styles.textHeader}>Please review your information</div>
          <TextColumn>
            <BillingConfirmItem title='Name' value={givenName + ' ' + familyName} dataTestId='user-full-name-text'/>
            <BillingConfirmItem title='Phone number' value={phoneNumber} dataTestId='user-phone-number-text'/>
            <BillingConfirmItem title='Contact email' value={contactEmail} dataTestId='user-contact-email-text'/>
            <BillingConfirmItem title='Researchallofus.org ID' value={username} dataTestId='user-workbench-id-text'/>
            <BillingConfirmItem title='Institution' value={verifiedInstitutionalAffiliation.institutionDisplayName}
                                dataTestId='user-institution-text'/>
            <BillingConfirmItem title='Payment type' value={useCreditCard ? 'Credit credit' : 'Purchase order/Other'}
                                dataTestId='use-credit-card-text'/>
            <BillingConfirmItem title='NiH-funded' value={nihFunded ? 'NIH’s STRIDES initiative' : 'N/A'} dataTestId='nih-funded-text'/>
          </TextColumn>
          <FlexRow style={{marginTop: '100px', justifyContent: 'space-between'}}>
            <Button type='secondary'
                    style={{fontWeight: 400, padding: '0 18px', height: '40px'}}
                    onClick={() => {setCurrentStep(2); }}>
              Back
            </Button>
            <FlexRow style={{justifyContent: 'flex-end'}}>
              <Button type='secondary'
                      style={{fontWeight: 400, padding: '0 18px', height: '40px', marginRight: '10px'}}
                      onClick={() => onClose()}>
                Cancel
              </Button>
              <Button data-test-id='submit-button'
                      type='primary'
                      style={{fontWeight: 400, padding: '0 18px', height: '40px', width: '93px'}}
                      onClick={() => {sendCreateBillingEmail(); }}>
                Submit
              </Button>
            </FlexRow>
          </FlexRow>
        </FlexColumn>)
      ], [
        4, () => (
        <FlexColumn>
          <div style={styles.textHeader}>Your request has been sent to a Google billing partner.
            One of their representatives will contact you shortly.</div>
          <br/>
          <div style={styles.textHeader}>Once your account is set up, you can use it to create
            a new workspace or change a current workspace billing account.</div>
          <FlexRow style={{marginTop: '100px', justifyContent: 'flex-end'}}>
            <Button type='primary'
                    style={{fontWeight: 400, padding: '0 18px', height: '40px', width: '93px'}}
                    onClick={() => onClose()}>
              OK
            </Button>
          </FlexRow>
        </FlexColumn>)
      ])}
      </ModalFooter>
    {currentStep === 0 && <FontAwesomeIcon
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
    />}
  </Modal>;
};
