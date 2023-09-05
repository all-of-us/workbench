import * as React from 'react';
import { useState } from 'react';
import * as fp from 'lodash/fp';
import { faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { switchCase } from '@terra-ui-packages/core-utils';
import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { GoogleCloudLogoSvg, InfoIcon } from 'app/components/icons';
import { CheckBox, TextInput } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  withErrorModal,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { SpinnerOverlay } from 'app/components/spinners';
import { TextColumn } from 'app/components/text-column';
import { profileApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { profileStore, useStore } from 'app/utils/stores';
import { supportUrls } from 'app/utils/zendesk';

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
    lineHeight: '21px',
    letterSpacing: '0',
    marginTop: '5px',
  },
  textNormal: {
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: '14px',
    lineHeight: '21px',
    letterSpacing: '0',
    marginTop: '5px',
  },
  radioButton: {
    lineHeight: '21px',
    letterSpacing: '0',
    marginTop: '15px',
    flexShrink: 0,
    width: '17px',
    height: '17px',
  },
  infoIcon: {
    height: '15px',
    marginLeft: '0.3rem',
    width: '15px',
  },
});

const stylesFunction = {
  stepButtonCircle: (
    currentStep: number,
    buttonStep: number
  ): React.CSSProperties => {
    return {
      visibility: currentStep === 0 || currentStep === 3 ? 'hidden' : 'visible',
      borderRadius: '50%',
      height: '37px',
      width: '37px',
      opacity: currentStep === buttonStep ? '1.0' : '0.2',
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
  },
};

export interface CreateBillingAccountState {
  currentStep: number;
  showBillingAccountDescription: boolean;
  showSubmitInstruction: boolean;
  userFullName: string;
  userPhoneNumber: string;
}

interface Props {
  onClose: Function;
}

const BillingConfirmItem = ({ title, value, dataTestId }) => {
  return (
    <FlexRow id={`${dataTestId}-wrapper`} style={{ marginTop: '5px' }}>
      <div style={{ width: '220px' }}>{title}:</div>
      <div data-test-id={dataTestId}>{value}</div>
    </FlexRow>
  );
};

const BilingPartnerTooltip = () => (
  <TooltipTrigger
    content={
      'Carahsoft, a third party associated with Google Cloud and the NIH, will have a representative help you set up your billing ' +
      'account. Carahsoft can also guide you to any additional third party with which your institution may already have an agreement.'
    }
  >
    <InfoIcon style={styles.infoIcon} />
  </TooltipTrigger>
);

const numSteps = 3;

export const CreateBillingAccountModal = ({ onClose }: Props) => {
  const {
    profile: {
      contactEmail,
      givenName,
      familyName,
      verifiedInstitutionalAffiliation,
      username,
    },
  } = useStore(profileStore);
  const [currentStep, setCurrentStep] = useState(0);
  const [phoneNumber, setPhoneNumber] = useState<string>();
  const [invalidPhoneNumberInput, setInvalidPhoneNumberInput] =
    useState<boolean>(null);
  const [nihFunded, setNihFunded] = useState<boolean>(null);
  const [emailSending, setEmailSending] = useState(false);

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
  )(async () => {
    setEmailSending(true);
    await profileApi().sendBillingSetupEmail({
      phone: phoneNumber,
      isNihFunded: nihFunded,
      institution: verifiedInstitutionalAffiliation.institutionDisplayName,
    });
    setCurrentStep(numSteps);
    setEmailSending(false);
  });

  return (
    <Modal width={650} onRequestClose={() => onClose()}>
      <ModalBody>
        <FlexColumn>
          <FlexRow
            style={{
              alignItems: 'center',
              width: '620px',
              marginBottom: '1.2rem',
            }}
          >
            <GoogleCloudLogoSvg
              style={{
                height: '33px',
                width: '207px',
                marginLeft: '-0.75rem',
                marginRight: '0.75rem',
              }}
            />
            <div style={styles.line}></div>
            <div
              style={{
                paddingTop: 5,
                marginLeft: '1.5rem',
                marginRight: '3rem',
              }}
            >
              <div style={styles.textHeader}>Create billing account</div>
            </div>
            {fp.range(1, numSteps).map((i) => (
              <div
                style={stylesFunction.stepButtonCircle(currentStep, i)}
                key={i}
              >
                {i}
              </div>
            ))}
          </FlexRow>
          {currentStep === 0 && (
            <TextColumn>
              <div style={styles.textNormal}>
                Billing accounts are managed via Google Cloud Platform™ service.
              </div>
            </TextColumn>
          )}
          {currentStep !== 0 && currentStep !== numSteps && (
            <TextColumn>
              <div style={styles.textNormal}>
                Submit your information below to receive billing and additional
                information from a Google billing partner representative.
              </div>
            </TextColumn>
          )}
        </FlexColumn>
      </ModalBody>
      <hr
        style={{
          width: '100%',
          backgroundColor: '#979797',
          borderWidth: '0px',
          height: '1px',
          marginTop: '0.75rem',
          marginBottom: '0.75rem',
        }}
      />
      <ModalFooter
        data-test-id={`step-${currentStep}-modal`}
        style={{ marginTop: 0, justifyContent: 'flex-start' }}
      >
        {switchCase(
          currentStep,
          [
            0,
            () => (
              <FlexRow style={{ justifyContent: 'space-evenly' }}>
                <FlexColumn>
                  <TextColumn>
                    <p style={styles.textHeader}>
                      Familiar with setting up a Google Cloud Platform account?
                    </p>
                    <p style={styles.textNormal}>
                      Manually set up an account in <br /> Google Cloud
                      Platform.
                    </p>
                  </TextColumn>
                  <Button
                    type='primary'
                    style={{
                      marginTop: '0.75rem',
                      fontWeight: 500,
                      fontSize: '14px',
                      height: '39px',
                      width: '188px',
                    }}
                    onClick={() =>
                      window.open(supportUrls.createBillingAccount, '_blank')
                    }
                  >
                    Read Directions
                  </Button>
                </FlexColumn>
                <FlexColumn>
                  <TextColumn>
                    <p style={styles.textHeader}>
                      Let a Google billing partner create the account for you.
                    </p>
                    <p style={styles.textNormal}>
                      A representative will help you set up <br />
                      your billing account.
                      <BilingPartnerTooltip />
                    </p>
                  </TextColumn>
                  <Button
                    data-test-id='use-billing-partner-button'
                    type='primary'
                    style={{
                      marginTop: '0.75rem',
                      fontWeight: 500,
                      fontSize: '14px',
                      height: '39px',
                      width: '220px',
                    }}
                    onClick={() => {
                      setCurrentStep(1);
                    }}
                  >
                    USE A BILLING PARTNER
                  </Button>
                </FlexColumn>
              </FlexRow>
            ),
          ],
          [
            1,
            () => (
              <FlexColumn
                style={{ justifyContent: 'space-evenly', width: '55.5rem' }}
              >
                <div style={styles.textHeader}>Your Information</div>
                <FlexRow style={{ marginTop: '20px' }}>
                  <FlexColumn style={styles.textNormal}>
                    <b>First name</b>
                    <TextInput
                      data-test-id='user-first-name'
                      style={styles.textInput}
                      disabled={true}
                      value={givenName}
                    />
                  </FlexColumn>
                  <FlexColumn style={styles.textNormal}>
                    <b>Last name</b>
                    <TextInput
                      data-test-id='user-last-name'
                      style={styles.textInput}
                      disabled={true}
                      value={familyName}
                    />
                  </FlexColumn>
                </FlexRow>
                <FlexRow style={{ marginTop: '20px' }}>
                  <FlexColumn style={styles.textNormal}>
                    <b>Your phone number</b>
                    <TextInput
                      data-test-id='user-phone-number'
                      style={styles.textInput}
                      onChange={(v) => validatePhoneNumber(v)}
                    />
                    {invalidPhoneNumberInput && (
                      <div
                        data-test-id='invalidPhoneNumber'
                        style={{ color: colors.danger }}
                      >
                        Invalid phone number input
                      </div>
                    )}
                  </FlexColumn>
                  <FlexColumn style={styles.textNormal}>
                    <b>Your contact email address</b>
                    <TextInput
                      data-test-id='user-contact-email'
                      disabled={true}
                      style={styles.textInput}
                      value={contactEmail}
                    />
                  </FlexColumn>
                </FlexRow>
                <FlexRow style={{ marginTop: '20px' }}>
                  <FlexColumn style={styles.textNormal}>
                    <b>Your Researcher Workbench login ID</b>
                    <TextInput
                      data-test-id='user-workbench-id'
                      style={styles.textInput}
                      disabled={true}
                      value={username}
                    />
                  </FlexColumn>
                  <FlexColumn style={styles.textNormal}>
                    <b>Your institution</b>
                    <TextInput
                      data-test-id='user-institution'
                      style={styles.textInput}
                      disabled={true}
                      value={
                        verifiedInstitutionalAffiliation.institutionDisplayName
                      }
                    />
                  </FlexColumn>
                </FlexRow>
                <FlexRow style={{ marginTop: '10px' }}>
                  <CheckBox
                    style={styles.radioButton}
                    checked={nihFunded === true}
                    onChange={(v) => setNihFunded(v)}
                  />
                  <FlexColumn
                    style={{
                      marginTop: '9px',
                      marginLeft: '15px',
                      marginBottom: '15px',
                    }}
                  >
                    <div style={styles.textNormal}>
                      <b>NIH-funded Research.</b>
                    </div>
                    <div style={styles.textNormal}>
                      My research is funded by the National Institute of Health
                      (NIH). NIH-funded research is eligible for discounted
                      cloud rates through the NIH STRIDES initiative.
                    </div>
                  </FlexColumn>
                </FlexRow>
                <FlexRow
                  style={{ marginTop: '100px', justifyContent: 'flex-end' }}
                >
                  <Button
                    type='secondary'
                    style={{
                      fontWeight: 400,
                      padding: '0 18px',
                      height: '40px',
                      marginRight: '10px',
                    }}
                    onClick={() => onClose()}
                  >
                    Cancel
                  </Button>
                  <Button
                    data-test-id='next-button'
                    type='primary'
                    style={{
                      fontWeight: 400,
                      padding: '0 18px',
                      height: '40px',
                      width: '93px',
                    }}
                    disabled={!phoneNumber}
                    onClick={() => {
                      setCurrentStep(2);
                    }}
                  >
                    Next
                  </Button>
                </FlexRow>
              </FlexColumn>
            ),
          ],
          [
            2,
            () => (
              <FlexColumn style={{ width: '100%' }}>
                <div style={styles.textHeader}>
                  Please review your information
                </div>
                <TextColumn>
                  <BillingConfirmItem
                    title='First Name'
                    value={givenName}
                    dataTestId='user-first-name-text'
                  />
                  <BillingConfirmItem
                    title='Last Name'
                    value={familyName}
                    dataTestId='user-last-name-text'
                  />
                  <BillingConfirmItem
                    title='Phone number'
                    value={phoneNumber}
                    dataTestId='user-phone-number-text'
                  />
                  <BillingConfirmItem
                    title='Contact email'
                    value={contactEmail}
                    dataTestId='user-contact-email-text'
                  />
                  <BillingConfirmItem
                    title='Researcher Workbench ID'
                    value={username}
                    dataTestId='user-workbench-id-text'
                  />
                  <BillingConfirmItem
                    title='Institution'
                    value={
                      verifiedInstitutionalAffiliation.institutionDisplayName
                    }
                    dataTestId='user-institution-text'
                  />
                  <BillingConfirmItem
                    title='NIH-funded'
                    value={nihFunded ? 'Yes' : 'No'}
                    dataTestId='nih-funded-text'
                  />
                </TextColumn>
                <FlexRow
                  style={{
                    marginTop: '100px',
                    justifyContent: 'space-between',
                  }}
                >
                  <Button
                    type='secondary'
                    style={{
                      fontWeight: 400,
                      padding: '0 18px',
                      height: '40px',
                    }}
                    onClick={() => {
                      setCurrentStep(1);
                    }}
                  >
                    Back
                  </Button>
                  <FlexRow style={{ justifyContent: 'flex-end' }}>
                    <Button
                      type='secondary'
                      style={{
                        fontWeight: 400,
                        padding: '0 18px',
                        height: '40px',
                        marginRight: '10px',
                      }}
                      onClick={() => onClose()}
                    >
                      Cancel
                    </Button>
                    <Button
                      data-test-id='submit-button'
                      type='primary'
                      style={{
                        fontWeight: 400,
                        padding: '0 18px',
                        height: '40px',
                        width: '93px',
                      }}
                      onClick={() => {
                        sendCreateBillingEmail();
                      }}
                    >
                      Submit
                    </Button>
                  </FlexRow>
                </FlexRow>
              </FlexColumn>
            ),
          ],
          [
            3,
            () => (
              <FlexColumn>
                <div style={styles.textHeader}>
                  Your request has been sent to a Google billing partner. One of
                  their representatives will contact within 1 business day.
                </div>
                <br />
                <div style={styles.textHeader}>
                  Once your account is set up, you can use it to create a new
                  workspace or change a current workspace billing account.
                </div>
                <FlexRow
                  style={{ marginTop: '100px', justifyContent: 'flex-end' }}
                >
                  <Button
                    type='primary'
                    style={{
                      fontWeight: 400,
                      padding: '0 18px',
                      height: '40px',
                      width: '93px',
                    }}
                    onClick={() => onClose()}
                  >
                    OK
                  </Button>
                </FlexRow>
              </FlexColumn>
            ),
          ]
        )}
      </ModalFooter>
      {currentStep === 0 && (
        <FontAwesomeIcon
          icon={faTimes}
          size='lg'
          style={{
            color: colors.accent,
            position: 'absolute',
            top: '1.5rem',
            right: '1.5rem',
            cursor: 'pointer',
          }}
          onClick={() => onClose()}
        />
      )}
      {emailSending && <SpinnerOverlay />}
    </Modal>
  );
};
