import * as fp from 'lodash/fp';
import * as React from 'react';
import * as validate from 'validate.js';

import {FlexColumn, FlexRow} from 'app/components/flex';
import {Error as ErrorDiv, TextInput} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {AccountCreationOptions} from 'app/pages/login/account-creation/account-creation-options';
import {institutionApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {isBlank, reactStyles} from 'app/utils';
import {
  ErrorResponse,
  InstitutionalRole,
  Profile,
  PublicInstitutionDetails,
} from 'generated/fetch';
import {Dropdown} from 'primereact/dropdown';
import {AouTitle} from 'app/components/text-wrappers';
import {commonStyles} from 'app/pages/login/account-creation/common-styles';
import {WhyWillSomeInformationBePublic} from 'app/pages/login/account-creation/common-content';
import {TooltipTrigger} from 'app/components/popups';
import {Button} from 'app/components/buttons';
import {FormSection} from 'app/components/forms';

const styles = reactStyles({
  ...commonStyles,
  publiclyDisplayedText: {
    fontSize: 12,
    fontWeight: 400
  },
  sectionInput: {
    width: '12rem',
    height: '1.5rem'
  },
  text: {
    fontSize: 14,
    color: colors.primary,
    lineHeight: '22px',
  }
});

// TODO: this is copy-pasted from account-creation.tsx. Consider factoring this into a true component.
function TextInputWithLabel(props) {
  return <div style={{width: '12rem', ...props.containerStyle}}>
    {props.labelContent}
    {props.labelText && <label style={{...styles.text, fontWeight: 600}}>{props.labelText}</label>}
    <div style={{marginTop: '0.1rem'}}>
      <TextInput data-test-id={props.inputId}
                 id={props.inputId}
                 name={props.inputName}
                 placeholder={props.placeholder}
                 value={props.value}
                 disabled={props.disabled}
                 onChange={props.onChange}
                 onBlur={props.onBlur}
                 invalid={props.invalid ? props.invalid.toString() : undefined}
                 style={{...styles.sectionInput, ...props.inputStyle}}/>
      {props.children}
    </div>
  </div>;
}

export interface Props {
  profile: Profile;
  onComplete: (profile: Profile) => void;
  onPreviousClick: (profile: Profile) => void;
}


interface State {
  profile: Profile;
  emailFailedValidation: boolean;
  loadingInstitutions: boolean;
  institutions: Array<PublicInstitutionDetails>;
  dataLoadError?: ErrorResponse;
}

export class AccountCreationInstitution extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      profile: props.profile,
      emailFailedValidation: false,
      institutions: [],
      loadingInstitutions: true
    };
  }

  async componentDidMount() {
    const details = await institutionApi().getPublicInstitutionDetails();
    this.setState({
      loadingInstitutions: false,
      institutions: details.institutions
    });
  }

  validateContactEmail() {
    const emailValidRegexp = new RegExp(/^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$/);
    if (!isBlank(this.state.profile.contactEmail) &&
      !emailValidRegexp.test(this.state.profile.contactEmail)) {
      this.setState({emailFailedValidation: true});
    } else {
      this.setState({emailFailedValidation: false});
    }
  }

  updateContactEmail(contactEmail: string) {
    this.setState({emailFailedValidation: false});
    this.setState(fp.set(['profile', 'contactEmail'], contactEmail));
  }

  validate(): Map<string, Array<string>> {
    const presenceCheck = {
      presence: {
        allowEmpty: false
      }
    };

    const validationCheck = {
      'verifiedInstitutionalAffiliation.institutionShortName': {
        presence: {
          allowEmpty: false,
          message: '^You must select an institution to continue',
        }
      },
      'verifiedInstitutionalAffiliation.institutionalRoleEnum': {
        presence: {
          allowEmpty: false,
          message: '^Institutional role cannot be blank',
        }
      },
      contactEmail: {
        presence: {
          allowEmpty: false,
          message: '^Email address cannot be blank',
        }
      },
    };
    if (this.state.profile.verifiedInstitutionalAffiliation.institutionalRoleEnum === InstitutionalRole.OTHER) {
      validationCheck['verifiedInstitutionalAffiliation.institutionalRoleOtherText'] = {
        presence: {
          allowEmpty: false,
          message: '^Institutional role text cannot be blank',
        }
      };
    }

    return validate(this.state.profile, validationCheck);
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
          institutionDisplayName
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
            All fields required unless indicated as optional
          </div>
          {loadingInstitutions && <SpinnerOverlay />}
          {!loadingInstitutions && <div style={{marginTop: '.5rem'}}>
            <label style={{...styles.text, fontWeight: 600}}>
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
                style={{width: '50%', minWidth: '600px'}}
                options={institutions.map(inst => ({'value': inst.shortName, 'label': inst.displayName}))}
                value={institutionShortName}
                onChange={(e) => this.updateAffiliationValue('institutionShortName', e.value)}/>
            <div style={{marginTop: '.5rem'}}>
              <a href={'https://www.researchallofus.org/apply/'} target='_blank' style={{color: colors.accent}}>
              Don't see your institution listed?
              </a>
            </div>
            <TextInputWithLabel containerStyle={{marginTop: '1rem', width: null}}
                                value={contactEmail}
                                inputId='contactEmail'
                                inputName='contactEmail'
                                labelContent={<div>
                                  <label style={{...styles.text, fontWeight: 600}}>
                                    Your institutional email address
                                  </label>
                                  <div style={{...styles.text, fontSize: 14}}>
                                    This will be the primary email contact for your new account.
                                  </div>
                                </div>}
                                invalid={this.state.emailFailedValidation}
                                onBlur={() => this.validateContactEmail()}
                                onChange={email => this.updateContactEmail(email)}/>
            {this.state.emailFailedValidation &&
              <ErrorDiv id='invalidEmailError'>
                Error: email address is invalid
              </ErrorDiv>
            }
            <div style={{marginTop: '.5rem'}}>
              <label style={{...styles.text, fontWeight: 600, marginTop: '1rem'}}>
                Which of the following best describes your role?
                <i style={{...styles.publiclyDisplayedText, marginLeft: '0.2rem'}}>
                  Publicly displayed
                </i>
              </label>
              <div>
                <Dropdown style={{width: '50%', 'minWidth': '600px'}}
                          options={this.getRoleOptions()}
                          value={institutionalRoleEnum}
                          onChange={(e) => this.updateAffiliationValue('institutionalRoleEnum', e.value)}/>
              </div>
            </div>
            {institutionalRoleEnum === InstitutionalRole.OTHER && <div style={{marginTop: '.5rem'}}>
              <label style={{...styles.text, fontWeight: 600, marginTop: '1rem'}}>
                Please describe your role
                <i style={{...styles.publiclyDisplayedText, marginLeft: '0.2rem'}}>
                  Publicly displayed
                </i>
              </label>
              <TextInputWithLabel value={institutionalRoleOtherText}
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
            <TooltipTrigger content={errors && <React.Fragment>
              <div>Please review the following: </div>
              <ul>
                {Object.keys(errors).map((key) => <li key={errors[key][0]}>{errors[key][0]}</li>)}
              </ul>
            </React.Fragment>} disabled={!errors}>
              <Button disabled={loadingInstitutions || errors != null}
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
