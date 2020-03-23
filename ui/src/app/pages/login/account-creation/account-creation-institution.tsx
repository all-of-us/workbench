import * as fp from 'lodash/fp';
import * as React from 'react';
import * as validate from 'validate.js';

import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {FormSection} from 'app/components/forms';
import {ValidationIcon} from 'app/components/icons';
import {Error as ErrorDiv, styles as inputStyles} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {AccountCreationOptions} from 'app/pages/login/account-creation/account-creation-options';
import {commonStyles, TextInputWithLabel, WhyWillSomeInformationBePublic} from 'app/pages/login/account-creation/common';
import {institutionApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {isBlank, reactStyles} from 'app/utils';
import {isAbortError, reportError} from 'app/utils/errors';
import {
  CheckEmailResponse,
  InstitutionalRole,
  Profile,
  PublicInstitutionDetails,
} from 'generated/fetch';
import {Dropdown} from 'primereact/dropdown';

const styles = reactStyles({
  ...commonStyles,
  publiclyDisplayedText: {
    fontSize: 12,
    fontWeight: 400
  },
  wideInputSize: {
    width: '50%',
    minWidth: '600px'
  },
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
        institutions: details.institutions
      });
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
    this.setState({checkEmailResponse: null});

    // Trigger an email-verification check, in case the user has entered or changed their email and
    // such a check hasn't yet been sent.
    this.checkEmail();
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
      const result = await institutionApi().checkEmail(institutionShortName, contactEmail,
        {signal: this.aborter.signal});
      this.setState({checkEmailResponse: result});
    } catch (e) {
      if (isAbortError(e)) {
        // Ignore abort errors.
      } else {
        this.setState({
          checkEmailError: true
        });
      }
    }
  }

  /**
   * Indicates whether the currently-entered email is a valid member of the indicated institution.
   * This controls the display of the "validity icon" next to the email input.
   *
   * Note: this does *not* check for format correctness of the indicated email. That is checked
   * separately at the form-level via validate.js
   */
  isEmailValid() {
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
    this.setState(fp.set(['profile', 'contactEmail'], contactEmail));
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

  getRoleOptions(): Array<{label: string, value: InstitutionalRole}> {
    const {institutions, profile: {verifiedInstitutionalAffiliation: {institutionShortName}}} = this.state;
    if (isBlank(institutionShortName)) {
      return [];
    }

    const selectedOrgType = institutions.find(
      inst => inst.shortName === institutionShortName).organizationTypeEnum;
    const availableRoles: Array<InstitutionalRole> =
      AccountCreationOptions.institutionalRolesByOrganizationType
      .find(obj => obj.type === selectedOrgType)
        .roles;

    return AccountCreationOptions.institutionalRoleOptions.filter(option =>
      availableRoles.includes(option.value)
    );
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
                style={{paddingTop: '1.5rem', paddingRight: '3rem', paddingLeft: '3rem'}}>
      <div style={{fontSize: 28, fontWeight: 400, color: colors.primary}}>Create your account</div>
      <FlexRow>
        <FlexColumn style={{marginTop: '0.5rem', marginRight: '2rem'}}>
          <div style={{...styles.text, fontSize: 16, marginTop: '1rem'}}>
            Please complete Step 1 of 3
          </div>
          <div style={{...styles.text, fontSize: 14, marginTop: '0.7rem'}}>
            For access to the <i>All of Us</i> Research Program data, your institution needs to have signed a Data Use Agreement
            with the program. The institutions listed below have an Institutional Data Use Agreement with the program that
            enables us to provide their researchers with access to the Workbench.
          </div>
          <div style={{...styles.text, fontSize: 12, marginTop: '0.5rem'}}>
            All fields are required.
          </div>
          {loadingInstitutions && <SpinnerOverlay />}
          {!loadingInstitutions && <div style={{marginTop: '.5rem'}}>
            <label style={styles.boldText}>
              Select your institution
              <i style={{...styles.publiclyDisplayedText, marginLeft: '0.2rem'}}>
                Publicly displayed
              </i>
            </label>
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
              <a href='mailto:support@researchallofus.org'>support@researchallofus.org</a>.
            </ErrorDiv>
            }
            <div style={{marginTop: '.5rem'}}>
              <a href={'https://www.researchallofus.org/apply/'} target='_blank' style={{color: colors.accent}}>
              Don't see your institution listed?
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
              <TooltipTrigger content={<div data-test-id='email-invalid-tooltip'>
                Email address is not a valid member of the selected institution.
              </div>} disabled={this.isEmailValid()}>
                <div style={{...inputStyles.iconArea}}>
                    <ValidationIcon validSuccess={this.isEmailValid()}/>
                </div>
              </TooltipTrigger>
            </TextInputWithLabel>
            {this.state.checkEmailError &&
              <ErrorDiv data-test-id='check-email-error'>
                An error occurred checking institution membership of this email. Please try again or
                contact <a href='mailto:support@researchallofus.org'>support@researchallofus.org</a>.
              </ErrorDiv>
            }
            <div style={{marginTop: '.5rem'}}>
              <label style={{...styles.boldText, marginTop: '1rem'}}>
                Which of the following best describes your role?
                <i style={{...styles.publiclyDisplayedText, marginLeft: '0.2rem'}}>
                  Publicly displayed
                </i>
              </label>
              <div>
                <Dropdown data-test-id='role-dropdown'
                          style={styles.wideInputSize}
                          placeholder={this.getRoleOptions() ?
                            '' : 'First select an institution above'}
                          options={this.getRoleOptions()}
                          value={institutionalRoleEnum}
                          onChange={(e) => this.updateAffiliationValue('institutionalRoleEnum', e.value)}/>
              </div>
            </div>
            {institutionalRoleEnum === InstitutionalRole.OTHER && <div style={{marginTop: '.5rem'}}>
              <label style={{...styles.boldText, marginTop: '1rem'}}>
                Please describe your role
                <i style={{...styles.publiclyDisplayedText, marginLeft: '0.2rem'}}>
                  Publicly displayed
                </i>
              </label>
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
              <ul>
                {Object.keys(errors).map((key) => <li key={errors[key][0]}>{errors[key][0]}</li>)}
              </ul>
            </div>} disabled={!errors}>
              <Button data-test-id='submit-button'
                      disabled={loadingInstitutions || errors != null}
                      onClick={() => this.props.onComplete(this.state.profile)}>
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
