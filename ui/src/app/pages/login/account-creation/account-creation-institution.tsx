import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';
import validate from 'validate.js';

import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {FormSection} from 'app/components/forms';
import {ValidationIcon} from 'app/components/icons';
import {Error as ErrorDiv, styles as inputStyles, TextInputWithLabel} from 'app/components/inputs';
import {BulletAlignedUnorderedList} from 'app/components/lists';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {AouTitle} from 'app/components/text-wrappers';
import {PubliclyDisplayed} from 'app/icons/publicly-displayed-icon';
import {
  commonStyles,
  WhyWillSomeInformationBePublic
} from 'app/pages/login/account-creation/common';
import {institutionApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {isBlank, reactStyles} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {reportError} from 'app/utils/errors';
import {
  getEmailValidationErrorMessage,
  getRoleOptions,
  checkInstitutionalEmail
} from 'app/utils/institutions';
import {
  CheckEmailResponse,
  InstitutionalRole,
  InstitutionMembershipRequirement,
  Profile,
  PublicInstitutionDetails,
} from 'generated/fetch';
import {SupportMailto} from 'app/components/support';

const styles = reactStyles({
  ...commonStyles,
  wideInputSize: {
    // We have a fixed width here because the text for dropdown options sometimes
    // expands beyond the width of the dropdown, which leads to display bugs.
    // For example, https://precisionmedicineinitiative.atlassian.net/browse/RW-4817
    width: 600
  },
  institutionalDuaTextBox: {
    ...commonStyles.text,
    fontSize: 14,
    marginTop: '0.7rem',
    padding: '0.5rem',
    backgroundColor: colorWithWhiteness(colors.accent, .75),
    borderRadius: '5px',
  }
});

/**
 * Create a custom validate.js validator to validate against a CheckEmailResponse API response
 * object. This validator should be enabled when the state object has a non-empty email and
 * institute. It requires that the CheckEmailResponse has returned and indicates that the
 * entered email address is a valid member of the institution.
 *
 * @param value
 * @param options
 * @param key
 * @param attributes
 */
validate.validators.checkEmailResponse = (value: CheckEmailResponse, options, key, attributes) => {
  if (value == null) {
    return '^Institutional membership check has not completed';
  }
  if (value && value.isValidMember) {
    return null;
  } else {
    return '^Email address is not a member of the selected institution';
  }
};

export interface Props {
  profile: Profile;
  onComplete: (profile: Profile) => void;
  onPreviousClick: (profile: Profile) => void;
}

interface State {
  profile: Profile;
  loadingInstitutions: boolean;
  institutions: Array<PublicInstitutionDetails>;
  institutionLoadError: boolean;
  checkEmailResponse?: CheckEmailResponse;
  checkEmailError: boolean;
}

export class AccountCreationInstitution extends React.Component<Props, State> {

  private aborter: AbortController;

  constructor(props: Props) {
    super(props);
    this.state = {
      profile: props.profile,
      loadingInstitutions: true,
      institutions: [],
      institutionLoadError: false,
      checkEmailError: false,
    };
  }

  async componentDidMount() {
    try {
      const details = await institutionApi().getPublicInstitutionDetails();
      this.setState({
        loadingInstitutions: false,
        institutions: fp.sortBy( institution => institution.displayName.trim(), details.institutions)
      });
      // Check email and populate appropriate icon In case page is loaded :
      // after clicking PREVIOUS BUTTON from step 3 or
      // if the fields were populated and researcher moves to TOS and back
      if (this.props.profile && this.props.profile.contactEmail) {
        this.checkEmail();
      }
    } catch (e) {
      this.setState({
        loadingInstitutions: false,
        institutionLoadError: true
      });
      reportError(e);
    }
  }

  componentWillUnmount(): void {
    if (this.aborter) {
      this.aborter.abort();
    }
  }

  onInstitutionChange(shortName: string): void {
    this.updateAffiliationValue('institutionShortName', shortName);
    // Clear out any existing values for role when the institution changes.
    this.updateAffiliationValue('institutionalRoleEnum', undefined);
    this.updateAffiliationValue('institutionalRoleOtherText', undefined);

    // Clear the email validation response, in case the email had previously been validated against
    // the prior institution.
    this.setState({checkEmailResponse: null}, () => {
      // Trigger an email-verification check, in case the user has entered or changed their email and
      // such a check hasn't yet been sent.
      this.checkEmail();
    });

  }

  onEmailBlur() {
    this.checkEmail();
  }

  /**
   * Checks that the entered email address is a valid member of the chosen institution.
   */
  async checkEmail() {
    const {
      profile: {
        contactEmail,
        verifiedInstitutionalAffiliation: {institutionShortName}
      }
    } = this.state;

    // Cancel any outstanding API calls.
    if (this.aborter) {
      this.aborter.abort();
    }
    this.aborter = new AbortController();
    this.setState({checkEmailResponse: null});

    // Early-exit with no result if either input is blank.
    if (!institutionShortName || isBlank(contactEmail)) {
      return;
    }

    try {
      const result = await checkInstitutionalEmail(contactEmail, institutionShortName, this.aborter);
      this.setState({checkEmailResponse: result});
    } catch (e) {
      this.setState({
        checkEmailError: true
      });
    }
  }

  /**
   * Indicates whether the currently-entered email is a valid member of the indicated institution.
   * This controls the display of the "validity icon" next to the email input. When undefined
   * is returned, the icon will not display anything.
   *
   * Note: this does *not* check for format correctness of the indicated email. That is checked
   * separately at the form-level via validate.js
   */
  isEmailValid(): boolean|undefined {
    const {
      checkEmailResponse,
      profile: {
        contactEmail,
        verifiedInstitutionalAffiliation: {institutionShortName}
      }
    } = this.state;

    if (!institutionShortName || isBlank(contactEmail) || checkEmailResponse == null) {
      return undefined;
    }

    return checkEmailResponse.isValidMember;
  }

  updateContactEmail(contactEmail: string) {
    this.setState(fp.set(['profile', 'contactEmail'], contactEmail.trim()));
  }

  /**
   * Returns a DOM fragment explaining to the user why institutional email verification has failed. This may be due to
   * (1) failing an exact email address match, (2) failing to match the email domain against a list of domains, or
   * (3) a server error when making the checkEmail request.
   */
  displayEmailErrorMessageIfNeeded(): React.ReactNode {
    const {institutions, checkEmailError, checkEmailResponse,
      profile: {
        contactEmail,
        verifiedInstitutionalAffiliation: {institutionShortName}
      }} = this.state;

    // No error if we haven't entered an email or chosen an institution.
    if (!institutionShortName || isBlank(contactEmail)) {
      return '';
    }

    // Institution email Address is either being verified or researcher has just entered it and has not changed focus
    if (!checkEmailResponse && !checkEmailError) {
      return '';
    }
    // No error if the institution check was successful.
    if (checkEmailResponse && checkEmailResponse.isValidMember) {
      return '';
    }

    // Show an error message if there was a server error.
    if (checkEmailError) {
      return <ErrorDiv data-test-id='check-email-error'>
        An error occurred checking institution membership of this email. Please try again or
        contact <SupportMailto/>.
      </ErrorDiv>;
    }

    // Finally, we distinguish between the two types of InstitutionMembershipRequirements in terms of user messaging.
    const selectedInstitutionObj = fp.find((institution) =>
      institution.shortName === institutionShortName, institutions);
    return getEmailValidationErrorMessage(selectedInstitutionObj);
  }

  /**
   * Runs client-side validation against the form inputs, and returns an object containing errors
   * strings, if empty. If validation passes, undefined is returned.
   *
   * Visible for testing.
   */
  public validate(): {[key: string]: Array<string>} {
    const validationCheck = {
      'profile.verifiedInstitutionalAffiliation.institutionShortName': {
        presence: {
          allowEmpty: false,
          message: '^You must select an institution to continue',
        }
      },
      'profile.contactEmail': {
        presence: {
          allowEmpty: false,
          message: '^Email address cannot be blank',
        },
        email: {
          message: '^Email address is invalid'
        }
      },
      'profile.verifiedInstitutionalAffiliation.institutionalRoleEnum': {
        presence: {
          allowEmpty: false,
          message: '^Institutional role cannot be blank',
        }
      },
    };
    if (!isBlank(this.state.profile.verifiedInstitutionalAffiliation.institutionShortName) &&
        !isBlank(this.state.profile.contactEmail)) {
      validationCheck['checkEmailResponse'] = {
        checkEmailResponse: {}
      };
    }

    if (this.state.profile.verifiedInstitutionalAffiliation.institutionalRoleEnum === InstitutionalRole.OTHER) {
      validationCheck['profile.verifiedInstitutionalAffiliation.institutionalRoleOtherText'] = {
        presence: {
          allowEmpty: false,
          message: '^Institutional role text cannot be blank',
        }
      };
    }

    return validate(this.state, validationCheck);
  }

  updateAffiliationValue(attribute: string, value) {
    this.setState(fp.set(['profile', 'verifiedInstitutionalAffiliation', attribute], value));
  }

  render() {
    const {
      loadingInstitutions,
      institutions,
      profile: {
        contactEmail,
        verifiedInstitutionalAffiliation: {
          institutionShortName, institutionalRoleEnum, institutionalRoleOtherText,
        }
      }
    } = this.state;

    const errors = this.validate();

    return <div id='account-creation-institution'
      style={{paddingTop: '1.5rem', paddingRight: '3rem', paddingLeft: '1rem'}}>
      <div style={{fontSize: 28, fontWeight: 400, color: colors.primary}}>Create your account</div>
      <FlexRow>
        <FlexColumn style={{marginTop: '0.5rem', marginRight: '2rem'}}>
          <div style={{...styles.text, fontSize: 16, marginTop: '1rem'}}>
            Please complete Step 1 of 3
          </div>
          <div style={styles.institutionalDuaTextBox}>
            For access to the <AouTitle/> data, your institution needs to have signed a Data Use Agreement
            with the program. The institutions listed below have an Institutional Data Use Agreement with the program that
            enables us to provide their researchers with access to the Researcher Workbench.
          </div>
          <div style={{...styles.text, fontSize: 12, marginTop: '0.5rem'}}>
            All fields are required unless indicated as optional
          </div>
          {loadingInstitutions && <SpinnerOverlay />}
          {!loadingInstitutions && <div style={{marginTop: '.5rem'}}>
            <FlexRow style={{alignItems: 'center', margin: '.5rem 0'}}>
              <label style={styles.boldText}>Select your institution</label>
              <PubliclyDisplayed style={{marginLeft: '1rem'}}/>
            </FlexRow>
            <div style={{...styles.text, fontSize: 14}}>
              Your institution will be notified that you have registered using your institutional credentials.
            </div>
            <Dropdown
              data-test-id='institution-dropdown'
              style={styles.wideInputSize}
              options={institutions.map(inst => ({'value': inst.shortName, 'label': inst.displayName}))}
              value={institutionShortName}
              onChange={(e) => this.onInstitutionChange(e.value)}
            />
            {this.state.institutionLoadError &&
            <ErrorDiv data-test-id='data-load-error'>
              An error occurred loading the institution list. Please try again or contact
              <SupportMailto/>.
            </ErrorDiv>
            }
            <div style={{marginTop: '.5rem'}}>
              <label style={styles.text}>
                Don't see your institution listed? Help us add it to our growing list by </label>
              <a href={'https://www.researchallofus.org/institutional-agreements/'} target='_blank'
                style={{color: colors.accent}}
                onClick={() => {
                  AnalyticsTracker.Registration.InstitutionNotListed();
                }}>
                submitting a request.
              </a>
            </div>
            <TextInputWithLabel containerStyle={{marginTop: '1rem', width: null}}
              value={contactEmail}
              inputId='contact-email'
              inputName='contactEmail'
              inputStyle={{width: '14rem'}}
              labelContent={<div>
                <label style={styles.boldText}>
                                    Your institutional email address
                </label>
                <div style={{...styles.text, fontSize: 14}}>
                                    This will be the primary email contact for your new account.
                </div>
              </div>}
              invalid={!this.isEmailValid()}
              onBlur={() => this.onEmailBlur()}
              onChange={email => this.updateContactEmail(email)}>
              <div style={{...inputStyles.iconArea}}>
                <ValidationIcon data-test-id='email-validation-icon' validSuccess={this.isEmailValid()}/>
              </div>
            </TextInputWithLabel>
            {this.displayEmailErrorMessageIfNeeded()}
            <div style={{marginTop: '.5rem'}}>
              <FlexRow style={{alignItems: 'center', margin: '.5rem 0'}}>
                <label style={styles.boldText}>
                  Which of the following best describes your role?
                </label>
                <PubliclyDisplayed style={{marginLeft: '1rem'}}/>
              </FlexRow>
              <div>
                <Dropdown data-test-id='role-dropdown'
                  style={styles.wideInputSize}
                  placeholder={getRoleOptions(institutions, institutionShortName) ?
                    '' : 'First select an institution above'}
                  options={getRoleOptions(institutions, institutionShortName)}
                  value={institutionalRoleEnum}
                  onChange={(e) => this.updateAffiliationValue('institutionalRoleEnum', e.value)}/>
              </div>
            </div>
            {institutionalRoleEnum === InstitutionalRole.OTHER && <div style={{marginTop: '.5rem'}}>
              <FlexRow style={{alignItems: 'center'}}>
                <label style={{...styles.boldText, margin: '.5rem 0'}}>
                  Please describe your role
                </label>
                <PubliclyDisplayed style={{marginLeft: '1rem'}}/>
              </FlexRow>
              <TextInputWithLabel value={institutionalRoleOtherText}
                inputStyle={styles.wideInputSize}
                inputId='institutionalRoleOtherText'
                inputName='institutionalRoleOtherText'
                onChange={v => this.updateAffiliationValue('institutionalRoleOtherText', v)}/>
            </div>
            }
          </div>
          }
          <FormSection style={{paddingBottom: '1rem'}}>
            <Button type='secondary' style={{marginRight: '1rem'}}
              onClick={() => this.props.onPreviousClick(this.state.profile)}>
              Previous
            </Button>
            <TooltipTrigger content={errors && <div data-test-id='validation-errors'>
              <div>Please review the following: </div>
              <BulletAlignedUnorderedList>
                {Object.keys(errors).map((key) => <li key={errors[key][0]}>{errors[key][0]}</li>)}
              </BulletAlignedUnorderedList>
            </div>} disabled={!errors}>
              <Button data-test-id='submit-button'
                disabled={loadingInstitutions || errors != null}
                onClick={() => {
                  AnalyticsTracker.Registration.InstitutionPage();
                  this.props.onComplete(this.state.profile);
                }}>
                Next
              </Button>
            </TooltipTrigger>
          </FormSection>
        </FlexColumn>
        <FlexColumn>
          <FlexColumn style={styles.asideContainer}>
            <WhyWillSomeInformationBePublic />
          </FlexColumn>
        </FlexColumn>
      </FlexRow>
    </div>;
  }
}
