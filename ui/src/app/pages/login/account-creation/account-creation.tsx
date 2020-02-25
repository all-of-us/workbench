import {Button} from 'app/components/buttons';
import {FormSection} from 'app/components/forms';

import {
  InfoIcon,
  ValidationIcon
} from 'app/components/icons';

import {
  Error as ErrorDiv,
  ErrorMessage,
  RadioButton,
  styles as inputStyles,
  TextArea,
  TextInput
} from 'app/components/inputs';

import {
  TooltipTrigger
} from 'app/components/popups';

import {
  profileApi
} from 'app/services/swagger-fetch-clients';

import {FlexColumn, FlexRow, flexStyle} from 'app/components/flex';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  EducationalRole,
  IndustryRole,
  InstitutionalAffiliation,
  NonAcademicAffiliation,
  Profile,
} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import {MultiSelect} from 'primereact/multiselect';
import * as React from 'react';
import * as validate from 'validate.js';

import {AouTitle} from 'app/components/text-wrappers';
import {reactStyles} from 'app/utils';
import {serverConfigStore} from 'app/utils/navigation';
import {AccountCreationOptions} from './account-creation-options';
import {Divider} from "app/components/divider";

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

const styles = reactStyles({
  asideContainer: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.85),
    borderRadius: 8,
    height: '17rem',
    width: '18rem',
    padding: '0.5rem'
  },
  asideHeader: {
    color: colors.primary,
    fontWeight: 600,
    fontSize: 16,
  },
  asideList: {
    display: 'flex',
    height: '100%',
    flexDirection: 'column',
    justifyContent: 'space-evenly'
  },
  asideText: {
    fontSize: 14,
    fontWeight: 400,
    color: colors.primary,
  },
  multiInputSpacing: {
    marginLeft: '2rem'
  },
  publiclyDisplayedText: {
    fontSize: 12,
    fontWeight: 400
  },
  sectionHeader: {
    width: '26rem',
    color: colors.primary,
    fontWeight: 600,
    fontSize: 18,
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

const researchPurposeList = [
  <span>Your research training and background</span>,
  <span>How you hope to use <i>All of Us</i> data for your research</span>,
  <span>Your research approach and the tools you use for answering your research questions (eg: Large datasets
     of phenotypes and genotypes, community engagement and community-based participatory research methods, etc.)</span>,
  <span>Your experience working with underrepresented populations as a scientist or outside of research, and how that
     experience may inform your work with <i>All of Us</i> data</span>
];

const nameLength = 80;

export const Section = (props) => {
  return <FormSection
      style={{...flexStyle.column, ...props.style}}>
    <div>
      <label style={{...styles.sectionHeader, ...props.sectionHeaderStyles}}>
        {props.header}
      </label>
      <label style={{color: colors.primary, fontSize: '12px', marginLeft: '.25rem'}}> {props.subHeader} </label>
    </div>
    <Divider style={{marginTop: '.25rem'}}/>
    {props.children}
  </FormSection>;
};

export const TextInputWithLabel = (props) => {
  return <FlexColumn style={{width: '12rem', ...props.containerStyle}}>
    <label style={{...styles.text, fontWeight: 600}}>{props.labelText}</label>
    <FlexRow style={{alignItems: 'center', marginTop: '0.1rem'}}>
      <TextInput id={props.inputId} name={props.inputName} placeholder={props.placeholder}
                 value={props.value}
                 disabled={props.disabled}
                 onChange={props.onChange}
                 invalid={props.invalid ? props.invalid.toString() : undefined}
                 style={{...styles.sectionInput, ...props.inputStyle}}/>
      {props.children}
    </FlexRow>
  </FlexColumn>;
};

export const MultiSelectWithLabel = (props) => {
  return <FlexColumn style={{width: '12rem', ...props.containerStyle}}>
    <label style={{...styles.text, fontWeight: 600}}>{props.labelText}</label>
    <FlexRow style={{alignItems: 'center', marginTop: '0.1rem'}}>
      <MultiSelect className='create-account__degree-select' placeholder={props.placeholder}
                   filter={false}
                   value={props.value} onChange={props.onChange}
                   options={props.options} data-test-id={props.dataTestId}
                   style={{...styles.sectionInput, overflowY: 'none'}}/>
      {props.children}
    </FlexRow>
  </FlexColumn>;
};

export interface AccountCreationProps {
  profile: Profile;
  invitationKey: string;
  onComplete: (profile: Profile) => void;
}

export interface AccountCreationState {
  creatingAccount: boolean;
  errors: any;
  invalidEmail: boolean;
  rolesOptions: any;
  profile: Profile;
  showAllFieldsRequiredError: boolean;
  showInstitution: boolean;
  showNonAcademicAffiliationRole: boolean;
  showNonAcademicAffiliationOther: boolean;
  usernameCheckInProgress: boolean;
  usernameConflictError: boolean;
  institutionName: string;
  institutionRole: string;
  nonAcademicAffiliation: string;
  nonAcademicAffiliationRole: string;
  nonAcademicAffiliationOther: string;
}

export class AccountCreation extends React.Component<AccountCreationProps, AccountCreationState> {
  private usernameCheckTimeout: NodeJS.Timer;

  constructor(props: AccountCreationProps) {
    // What's going on with this assertion: the account creation form only edits a single
    // institutional affiliation entry, even though it's a repeated field. This component has
    // a convention of requiring the Profile set in props to have a single, empty institutional
    // affiliation already populated, for editing by this form. See sign-in.tsx where the "empty"
    // profile object is created.
    if (props.profile.institutionalAffiliations.length !== 1) {
      throw new Error('Profile must be pre-allocated with 1 institutional affiliation.');
    }
    super(props);
    this.state = this.createInitialState();

  }

  componentDidMount() {
    this.updateNonAcademicAffiliationRoles(
      this.state.profile.institutionalAffiliations[0].nonAcademicAffiliation);
    this.selectNonAcademicAffiliationRoles(
      this.state.profile.institutionalAffiliations[0].role);
  }

  createInitialState(): AccountCreationState {
    const state: AccountCreationState = {
      errors: undefined,
      profile: this.props.profile,
      usernameCheckInProgress: false,
      usernameConflictError: false,
      creatingAccount: false,
      showAllFieldsRequiredError: false,
      // showInstitution defaults to true, since we expect most users coming in will be academics.
      showInstitution: true,
      showNonAcademicAffiliationRole: false,
      showNonAcademicAffiliationOther: false,
      invalidEmail: false,
      rolesOptions: [],
      institutionName: '',
      institutionRole: '',
      nonAcademicAffiliation: '',
      nonAcademicAffiliationRole: '',
      nonAcademicAffiliationOther: ''
    };

    const institutionalAffiliation = this.props.profile.institutionalAffiliations[0];
    if (institutionalAffiliation.institution) {
      state.showInstitution = true;
    }

    return state;
  }

  // This method will be deleted once we enable new account pages
  createAccount(): void {
    const {invitationKey, onComplete} = this.props;
    const profile = this.state.profile;
    profile.institutionalAffiliations = [];
    const emailValidRegex = new RegExp(/^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$/);
    this.setState({showAllFieldsRequiredError: false});
    this.setState({invalidEmail: false});
    const requiredFields =
      [profile.givenName, profile.familyName, profile.username, profile.contactEmail,
        profile.currentPosition, profile.organization, profile.areaOfResearch];
    if (requiredFields.some(isBlank)) {
      this.setState({showAllFieldsRequiredError: true});
      return;
    } else if (this.isUsernameValidationError) {
      return;
    } else if (!emailValidRegex.test(profile.contactEmail)) {
      this.setState({invalidEmail: true});
      return;
    }
    this.setState({creatingAccount: true});
    profileApi().createAccount({profile, invitationKey})
      .then((savedProfile) => {
        this.setState({profile: savedProfile, creatingAccount: false});
        onComplete(savedProfile);
      })
      .catch(error => {
        console.log(error);
        this.setState({creatingAccount: false});
      });
  }

  get usernameValid(): boolean {
    if (isBlank(this.state.profile.username) || this.state.usernameCheckInProgress) {
      return undefined;
    }
    return !this.isUsernameValidationError;
  }

  get isUsernameValidationError(): boolean {
    return (this.state.usernameConflictError || this.usernameInvalidError());
  }

  usernameInvalidError(): boolean {
    const username = this.state.profile.username;
    if (isBlank(username)) {
      return false;
    }
    if (username.trim().length > 64 || username.trim().length < 3) {
      return true;
    }
    // Include alphanumeric characters, -'s, _'s, apostrophes, and single .'s in a row.
    if (username.includes('..') || username.endsWith('.')) {
      return true;
    }
    return !(new RegExp(/^[\w'-][\w.'-]*$/).test(username));
  }

  usernameChanged(value: string): void {
    this.updateProfileToBeDeleted('username', value);
    const {username} = this.state.profile;
    if (username === '') {
      return;
    }
    this.setState({usernameConflictError: false});
    // TODO: This should use a debounce, rather than manual setTimeout()s.
    clearTimeout(this.usernameCheckTimeout);
    this.setState({usernameCheckInProgress: true});
    this.usernameCheckTimeout = setTimeout(() => {
      if (!username.trim()) {
        this.setState({usernameCheckInProgress: false});
        return;
      }
      profileApi().isUsernameTaken(username)
          .then((body) => {
            this.setState({usernameCheckInProgress: false, usernameConflictError: body.isTaken});
          })
          .catch((error) => {
            console.log(error);
            this.setState({usernameCheckInProgress: false});
          });
    }, 300);
  }

  updateProfileToBeDeleted(attribute: string, value: string) {
    if (attribute === 'contactEmail') {
      this.setState({invalidEmail: false});
    }
    const newProfile = this.state.profile;
    newProfile[attribute] = value;
    this.setState(({profile}) => ({profile: fp.set(attribute, value, profile)}));
  }

  updateProfileObject(attribute: string, value) {
    this.setState(fp.set(['profile', attribute], value));
  }

  updateAddress(attribute: string , value) {
    this.setState(fp.set(['profile', 'address', attribute], value));
  }

  updateInstitutionAffiliation(attribute: string, value) {
    this.setState(fp.set(['profile', 'institutionalAffiliations', '0', attribute], value));
  }

  showFreeTextField(option) {
    return option === NonAcademicAffiliation.FREETEXT || option === IndustryRole.FREETEXT ||
        option === EducationalRole.FREETEXT;
  }

  clearInstitutionAffiliation() {
    this.setState(fp.set(['profile', 'institutionalAffiliations', '0'], {
      nonAcademicAffiliation: null,
      role: '',
      institution: '',
      other: ''
    }));
  }

  updateNonAcademicAffiliationRoles(nonAcademicAffiliation) {
    this.updateInstitutionAffiliation('nonAcademicAffiliation', nonAcademicAffiliation);
    this.setState({showNonAcademicAffiliationRole: false, showNonAcademicAffiliationOther: false});
    if (nonAcademicAffiliation === NonAcademicAffiliation.INDUSTRY) {
      this.setState({rolesOptions: AccountCreationOptions.industryRole,
        showNonAcademicAffiliationRole: true});
    } else if (nonAcademicAffiliation === NonAcademicAffiliation.EDUCATIONALINSTITUTION) {
      this.setState({rolesOptions: AccountCreationOptions.educationRole, showNonAcademicAffiliationRole: true});
    } else if (this.showFreeTextField(nonAcademicAffiliation)) {
      this.setState({showNonAcademicAffiliationOther: true});
      return;
    }
    this.selectNonAcademicAffiliationRoles(this.state.nonAcademicAffiliationRole);
  }

  selectNonAcademicAffiliationRoles(role) {
    if (this.showFreeTextField(role)) {
      this.setState({nonAcademicAffiliationRole: role, showNonAcademicAffiliationOther: true});
    } else {
      this.setState({nonAcademicAffiliationRole: role, showNonAcademicAffiliationOther: false});
    }
    this.updateInstitutionAffiliation('role', role);

  }

  // This will be deleted once enableAccountPages is set to true for prod
  validate() {
    const {profile} = this.state;
    const requiredFields =
      [profile.givenName, profile.familyName, profile.username, profile.contactEmail,
        profile.currentPosition, profile.organization, profile.areaOfResearch];
    if (requiredFields.some(isBlank)) {
      this.setState({showAllFieldsRequiredError: true});
      return;
    }
  }

  validateAccountCreation() {
    const {
      showInstitution,
      profile: { givenName, familyName, contactEmail, areaOfResearch, degrees, username,
        address: {streetAddress1, city, country, state, zipCode}, institutionalAffiliations
      }
    } = this.state;

    let institution, nonAcademicAffiliation, role;
    if (institutionalAffiliations.length) {
      ({institution, nonAcademicAffiliation, role} = institutionalAffiliations[0]);
    }

    const presenceCheck = {
      presence: {
        allowEmpty: false
      }
    };

    const validationCheck = {
      username: {
        presence: presenceCheck,
        length: {
          minimum: 4 + serverConfigStore.getValue().gsuiteDomain.length,
          maximum: 64 + serverConfigStore.getValue().gsuiteDomain.length,
          tooShort: 'not valid',
          tooLong: 'not valid'
        },
        email: {
          message: ' not valid'
        }
      },
      givenName: presenceCheck,
      familyName: presenceCheck,
      contactEmail: {
        presence: presenceCheck,
        email: {
          message: 'invalid'
        }
      },
      streetAddress1: presenceCheck,
      city: presenceCheck,
      state: presenceCheck,
      zipCode: presenceCheck,
      country: presenceCheck,
      areaOfResearch: presenceCheck
    };

    showInstitution ? validationCheck['institution'] = presenceCheck :
      validationCheck['nonAcademicAffiliation'] = presenceCheck;

    if (showInstitution || nonAcademicAffiliation !== NonAcademicAffiliation.COMMUNITYSCIENTIST) {
      validationCheck['role'] = presenceCheck;
    }


    return validate({areaOfResearch, degrees, givenName, familyName, contactEmail, streetAddress1,
      city, state, country, institution, nonAcademicAffiliation, role, zipCode,
      username: username + '@' + serverConfigStore.getValue().gsuiteDomain}, validationCheck);
  }

  getInstitutionalAffiliationPropertyOrEmptyString(property: string) {
    return this.state.profile.institutionalAffiliations[0][property];
  }

  render() {
    const {
      profile: {
        givenName, familyName, currentPosition, organization,
        contactEmail, username, areaOfResearch, professionalUrl,
        address: {
          streetAddress1, streetAddress2, city, state, zipCode, country
        },
      },
    } = this.state;
    const enableNewAccountCreation = serverConfigStore.getValue().enableNewAccountCreation;
    const institutionalAffiliation: InstitutionalAffiliation =
      this.state.profile.institutionalAffiliations[0];

    const usernameLabelText =
      <div>New Username
        <TooltipTrigger side='top' content={<div>Usernames can contain only letters
          (a-z), numbers (0-9), dashes (-), underscores (_), apostrophes ('), and
          periods (.) (minimum of 3 characters and maximum of 64
          characters).<br/>Usernames cannot begin or end with a period (.) and may not
          contain more than one period (.) in a row.</div>}
                        style={{marginLeft: '0.5rem'}}>
          <InfoIcon style={{'height': '16px', 'paddingLeft': '2px'}}/>
        </TooltipTrigger>
      </div>;

    const errors = this.validateAccountCreation();

    return <div id='account-creation'
                style={{paddingTop: enableNewAccountCreation ? '1.5rem' :
                      '3rem', paddingRight: '3rem', paddingLeft: '3rem'}}>
      <div style={{fontSize: 28, fontWeight: 400, color: colors.primary}}>Create your account</div>
      {enableNewAccountCreation && <FlexRow>
        <FlexColumn style={{marginTop: '0.5rem'}}>
          <div style={{...styles.text, fontSize: 16, marginTop: '1rem'}}>
            Please complete Step 1 of 2
          </div>
          <div style={{...styles.text, fontSize: 12, marginTop: '0.7rem'}}>All fields required unless indicated as optional</div>
          <Section header={<div>Create an <i>All of Us</i> username</div>}>
            <div>
              <FlexRow>
                  <TextInputWithLabel value={username} inputId='username' inputName='username'
                                      placeholder='New Username' invalid={
                                        this.state.usernameConflictError || this.usernameInvalidError()}
                                      containerStyle={{width: '26rem'}} labelText={usernameLabelText}
                                    onChange={v => this.usernameChanged(v)}>
                  <div style={{...inputStyles.iconArea}}>
                    <ValidationIcon validSuccess={this.usernameValid}/>
                  </div>
                  <i style={{...styles.asideText, marginLeft: 4}}>@{serverConfigStore.getValue().gsuiteDomain}</i>
                </TextInputWithLabel>

              </FlexRow>
              {this.state.usernameConflictError &&
              <div style={{height: '1.5rem'}}>
                <ErrorDiv id='usernameConflictError'>
                  Username is already taken.
                </ErrorDiv></div>}
              {this.usernameInvalidError() &&
              <div style={{height: '1.5rem'}}><ErrorDiv id='usernameError'>
                {username} is not a valid username.
              </ErrorDiv></div>}
            </div>
          </Section>
          <Section header={<div>About you <i style={styles.publiclyDisplayedText}>Publicly displayed</i></div>}>
            <FlexColumn>
              <FlexRow style={{paddingBottom: '1rem'}}>
                <TextInputWithLabel value={givenName} inputId='givenName' inputName='givenName' placeholder='First Name'
                                    invalid={givenName.length > nameLength} labelText='First Name'
                                    onChange={value => this.updateProfileObject('givenName', value)} />
                {givenName.length > nameLength &&
                <ErrorMessage id='givenNameError'>
                  First Name must be {nameLength} characters or less.
                </ErrorMessage>}
                <TextInputWithLabel value={familyName} inputId='familyName' inputName='familyName' placeholder='Last Name'
                                    invalid={familyName.length > nameLength} containerStyle={styles.multiInputSpacing}
                                    onChange={v => this.updateProfileObject('familyName', v)}
                                    labelText='Last Name'/>
                {familyName.length > nameLength &&
                <ErrorMessage id='familyNameError'>
                  Last Name must be {nameLength} character or less.
                </ErrorMessage>}
              </FlexRow>
              <FlexRow style={{alignItems: 'center'}}>
                <TextInputWithLabel value={contactEmail} inputId='contactEmail' inputName='contactEmail'
                                    placeholder='Email Address'
                                    invalid={this.state.invalidEmail} labelText='Email Address'
                                    onChange={v => this.updateProfileObject('contactEmail', v)}/>
                {this.state.invalidEmail &&
                <ErrorDiv id='invalidEmailError'>
                  Contact Email is invalid
                </ErrorDiv>}
                <MultiSelectWithLabel placeholder={'Select one or more'} options={AccountCreationOptions.degree}
                                      containerStyle={styles.multiInputSpacing} value={this.state.profile.degrees}
                                      labelText='Your degrees (optional)'
                                      onChange={(e) => this.setState(fp.set(['profile', 'degrees'], e.value))}
                                      />
              </FlexRow>
            </FlexColumn>
          </Section>
          <Section header={<React.Fragment>
            <div>Your institutional mailing address</div>
            <div style={styles.asideText}>We will use your address if we need to send correspondence about the program;
              your information will not be shared or displayed publicly.</div>
          </React.Fragment>}>
            <FlexColumn style={{lineHeight: '1rem'}}>
              <FlexRow>
                <TextInputWithLabel dataTestId='streetAddress' inputName='streetAddress'
                                    placeholder='Street Address' value={streetAddress1} labelText='Street Address 1'
                                    onChange={value => this.updateAddress('streetAddress1', value)}/>
                <TextInputWithLabel dataTestId='streetAddress2' inputName='streetAddress2' placeholder='Street Address 2'
                                    value={streetAddress2} labelText='Street Address 2'
                                    containerStyle={styles.multiInputSpacing}
                                    onChange={value => this.updateAddress('streetAddress2', value)}/>
              </FlexRow>
              <FlexRow style={{marginTop: '0.75rem'}}>
                <TextInputWithLabel dataTestId='city' inputName='city' placeholder='City' value={city} labelText='City'
                                    onChange={value => this.updateAddress('city', value)}/>
                <TextInputWithLabel dataTestId='state' inputName='state' placeholder='State' value={state} labelText='State'
                                    containerStyle={styles.multiInputSpacing}
                                    onChange={value => this.updateAddress('state', value)}/>
              </FlexRow>
              <FlexRow style={{marginTop: '0.75rem'}}>
                <TextInputWithLabel dataTestId='zip' inputName='zip' placeholder='Zip code'
                                    value={zipCode} labelText='Zip Code'
                                    onChange={value => this.updateAddress('zipCode', value)}/>
                <TextInputWithLabel dataTestId='country' inputName='country' placeholder='Country' value={country}
                                    labelText='Country' containerStyle={styles.multiInputSpacing}
                                    onChange={value => this.updateAddress('country', value)}/>
              </FlexRow>
            </FlexColumn>
          </Section>
          <Section sectionHeaderStyles={{borderBottom: null}} header={<React.Fragment>
            <div>Please describe your research background, experience, and research interests</div>
            <div style={styles.asideText}>This information will be posted publicly on the <i>All of Us</i> Research Hub website
              to inform program participants. <span  style={{marginLeft: 2,
                fontSize: 12}}>(2000 character limit)</span>
              <i style={{...styles.publiclyDisplayedText, marginLeft: 2}}>
                Publicly displayed
              </i></div>
          </React.Fragment>}>
            <TextArea style={{height: '15rem', resize: 'none', width: '26rem', borderRadius: '3px 3px 0 0',
              borderColor: colorWithWhiteness(colors.dark, 0.5)}}
                      id='areaOfResearch'
                      name='areaOfResearch'
                      placeholder='Describe Your Current Research'
                      value={areaOfResearch}
                      onChange={v => this.updateProfileObject('areaOfResearch', v)}/>
            <FlexRow style={{justifyContent: 'flex-end', width: '26rem',
              backgroundColor: colorWithWhiteness(colors.primary, 0.85), fontSize: 12,
              color: colors.primary, padding: '0.25rem', borderRadius: '0 0 3px 3px',
              border: `1px solid ${colorWithWhiteness(colors.dark, 0.5)}`}}>
              {2000 - areaOfResearch.length} characters remaining
            </FlexRow>
          </Section>
          <Section header='Institutional Affiliation'>
            <label style={{color: colors.primary, fontSize: 16}}>
              Are you affiliated with an Academic Research Institution?
            </label>
            <div style={{paddingTop: '0.5rem'}}>
              <RadioButton id='show-institution-yes' data-test-id='show-institution-yes'
                           onChange={() => {this.clearInstitutionAffiliation();
                             this.setState({showInstitution: true}); }}
                           checked={this.state.showInstitution === true} style={{marginRight: '0.5rem'}}/>
              <label htmlFor='show-institution-yes' style={{paddingRight: '3rem', color: colors.primary}}>
                Yes
              </label>
              <RadioButton id='show-institution-no' data-test-id='show-institution-no'
                           onChange={() => {this.clearInstitutionAffiliation();
                             this.setState({showInstitution: false}); }}
                           checked={this.state.showInstitution === false} style={{marginRight: '0.5rem'}}/>
              <label htmlFor='show-institution-no' style={{color: colors.primary}}>No</label>
            </div>
          </Section>
          {this.state.showInstitution &&
          <FlexColumn style={{justifyContent: 'space-between'}}>
            <TextInput data-test-id='institution-name' style={{width: '16rem', marginBottom: '0.5rem',
              marginTop: '0.5rem'}}
              value={institutionalAffiliation.institution}
              placeholder='Institution Name'
              onChange={value => this.updateInstitutionAffiliation('institution', value)}
                       />
            <Dropdown data-test-id='institutionRole'
                      value={this.getInstitutionalAffiliationPropertyOrEmptyString('role')}
                      onChange={e => this.updateInstitutionAffiliation('role', e.value)}
                      placeholder='Which of the following describes your role'
                      style={{width: '16rem'}} options={AccountCreationOptions.roles}/>
          </FlexColumn>}
          {!this.state.showInstitution &&
          <FlexColumn style={{justifyContent: 'space-between'}}>
            <Dropdown data-test-id='affiliation'
                      style={{width: '18rem', marginBottom: '0.5rem', marginTop: '0.5rem'}}
                      value={this.getInstitutionalAffiliationPropertyOrEmptyString('nonAcademicAffiliation')}
                      options={AccountCreationOptions.nonAcademicAffiliations}
                      onChange={e => this.updateNonAcademicAffiliationRoles(e.value)}
                      placeholder='Which of the following better describes your affiliation?'/>
            {this.state.showNonAcademicAffiliationRole &&
            <Dropdown data-test-id='affiliationrole' placeholder='Which of the following describes your role'
                      options={this.state.rolesOptions}
                      value={this.getInstitutionalAffiliationPropertyOrEmptyString('role')}
                      onChange={e => this.selectNonAcademicAffiliationRoles(e.value)}
                      style={{width: '18rem'}}/>}
            {this.state.showNonAcademicAffiliationOther &&
            <TextInput value={this.getInstitutionalAffiliationPropertyOrEmptyString('other')}
                       onChange={value => this.updateInstitutionAffiliation('other', value)}
                       style={{marginTop: '1rem', width: '18rem'}}/>}
          </FlexColumn>}
          <Section header={<React.Fragment>
            <div>Please link to your professional profile or bio page below, if available</div>
            <div style={styles.asideText}>You could provide a link to your faculty bio page from your institution's
              website, your LinkedIn profile page, or another webpage featuring your work. This will
              allow <i>All of Us</i> researchers and participants to learn more about your work and publications.</div>
          </React.Fragment>}>
              <TextInputWithLabel dataTestId='professionalUrl' inputName='professionalUrl'
                                  placeholder='Professional Url' value={professionalUrl}
                                  labelText={<div>
                                    Paste Professional URL here <i style={{...styles.publiclyDisplayedText,
                                      marginLeft: 2}}>Optional and publicly displayed</i>
                                  </div>} containerStyle={{width: '26rem'}}
                                  onChange={value => this.updateProfileObject('professionalUrl', value)}/>
          </Section>
          <FormSection style={{paddingBottom: '1rem'}}>
            <TooltipTrigger content={errors && <React.Fragment>
              <div>Please review the following: </div>
              <ul>
                {Object.keys(errors).map((key) => <li key={errors[key][0]}>{errors[key][0]}</li>)}
              </ul>
            </React.Fragment>} disabled={!errors}>
              <Button disabled={this.state.usernameCheckInProgress || this.isUsernameValidationError || errors}
                      style={{'height': '2rem', 'width': '10rem'}}
                      onClick={() => this.props.onComplete(this.state.profile)}>
                Next
              </Button>
            </TooltipTrigger>
          </FormSection>
        </FlexColumn>
        <FlexColumn>
          <FlexColumn style={styles.asideContainer}>
            <div style={styles.asideHeader}>About your new username</div>
            <div style={styles.asideText}>We create a 'username'@{serverConfigStore.getValue().gsuiteDomain} Google
                account which you will use to login to the Workbench.</div>
            <div style={{...styles.asideHeader, marginTop: '1rem'}}>Why will some information be public?</div>
            <div style={styles.asideText}>The <AouTitle/> is committed to transparency with the Research
                participants on who can access their data, and the purpose of such access. Therefore, your name,
                institution and role, as well as your research background/interests and link to your professional
                profile will be displayed publicly on the Research Projects Directory on the <AouTitle/> website to
                inform the <i>All of Us</i> Research participants, and to comply with the 21st Century Cures Act. Some of the
                fields noted above may not be visible currently, but will be added in the future.</div>
          </FlexColumn>
          <FlexColumn style={{...styles.asideContainer, marginTop: '21.8rem', height: '15rem'}}>
            <div style={styles.asideHeader}><i>All of Us</i> participants are most interested in knowing:</div>
            <ul style={styles.asideList}>
              {researchPurposeList.map((value, index) => <li key={index} style={styles.asideText}>{value}</li>)}
            </ul>
          </FlexColumn>
        </FlexColumn>
      </FlexRow>}
      {/*The following will be deleted once enableAccountPages is set to true in prod*/}
      {!enableNewAccountCreation && <div>
        <FormSection>
          <TextInput id='givenName' name='givenName' autoFocus
                     placeholder='First name'
                     value={givenName}
                     invalid={String(givenName.length > 80)}
                     style={{width: '16rem'}}
                     onChange={v => this.updateProfileToBeDeleted('givenName', v)}/>
          {givenName.length > 80 &&
          <ErrorMessage id='givenNameError'>
            First Name must be 80 characters or less.
          </ErrorMessage>}
        </FormSection>
        <FormSection>
          <TextInput id='familyName' name='familyName' placeholder='Last name'
                     value={familyName}
                     invalid={String(familyName.length > 80)}
                     style={{width: '16rem'}}
                     onChange={v => this.updateProfileToBeDeleted('familyName', v)}/>
          {familyName.length > 80 &&
          <ErrorMessage id='familyNameError'>
            Last Name must be 80 character or less.
          </ErrorMessage>}
        </FormSection>
        <FormSection>
          <TextInput id='contactEmail' name='contactEmail'
                     placeholder='Email Address'
                     value={contactEmail}
                     style={{width: '16rem'}}
                     onChange={v => this.updateProfileToBeDeleted('contactEmail', v)}/>
          {this.state.invalidEmail &&
          <ErrorDiv id='invalidEmailError'>
            Contact Email Id is invalid
          </ErrorDiv>}
        </FormSection>
        <FormSection>
          <TextInput id='currentPosition' name='currentPosition'
                     placeholder='Your Current Position'
                     value={currentPosition}
                     invalid={currentPosition.length > 255}
                     style={{width: '16rem'}}
                     onChange={v => this.updateProfileToBeDeleted('currentPosition', v)}/>
          {currentPosition.length > 255 &&
          <ErrorMessage id='currentPositionError'>
            Current Position must be 255 characters or less.
          </ErrorMessage>}
        </FormSection>
        <FormSection>
          <TextInput id='organization' name='organization'
                     placeholder='Your Organization'
                     value={organization}
                     invalid={organization.length > 255}
                     style={{width: '16rem'}}
                     onChange={v => this.updateProfileToBeDeleted('organization', v)}/>
          {organization.length > 255 &&
          <ErrorMessage id='organizationError'>
            Organization must be 255 characters of less.
          </ErrorMessage>}
        </FormSection>
        <FormSection style={{display: 'flex'}}>
              <TextArea style={{height: '10em', resize: 'none', width: '16rem'}}
                        id='areaOfResearch'
                        name='areaOfResearch'
                        placeholder='Describe Your Current Research'
                        value={areaOfResearch}
                        onChange={v => this.updateProfileToBeDeleted('areaOfResearch', v)}/>
          <TooltipTrigger content={<span>You are required to describe your current research in
            order to help <i>All of Us</i> improve the Researcher Workbench.</span>}>
            <InfoIcon style={{
              'height': '22px',
              'marginTop': '2.2rem',
              'paddingLeft': '2px'
            }}/>
          </TooltipTrigger>
        </FormSection>
        <FormSection>
          <TextInput id='username' name='username' placeholder='New Username'
                     value={username}
                     onChange={v => this.usernameChanged(v)}
                     invalid={this.state.usernameConflictError || this.usernameInvalidError()}
                     style={{width: '16rem'}}/>
          <div style={inputStyles.iconArea}>
            <ValidationIcon validSuccess={this.usernameValid}/>
          </div>
          <TooltipTrigger content={<div>Usernames can contain only letters (a-z),
            numbers (0-9), dashes (-), underscores (_), apostrophes ('), and periods (.)
            (minimum of 3 characters and maximum of 64 characters).<br/>Usernames cannot
            begin or end with a period (.) and may not contain more than one period (.) in a row.
          </div>}>
            <InfoIcon style={{'height': '22px', 'paddingLeft': '2px'}}/>
          </TooltipTrigger>
          <div style={{height: '1.5rem'}}>
            {this.state.usernameConflictError &&
            <ErrorDiv id='usernameConflictError'>
              Username is already taken.
            </ErrorDiv>}
            {this.usernameInvalidError() &&
            <ErrorDiv id='usernameError'>
              Username is not a valid username.
            </ErrorDiv>}
          </div>
        </FormSection>
        <FormSection>
          <Button disabled={this.state.creatingAccount || this.state.usernameCheckInProgress ||
          this.isUsernameValidationError}
                  style={{'height': '2rem', 'width': '10rem'}}
                  onClick={() => this.createAccount()}>
            Next
          </Button>
        </FormSection>
      </div>}
      {!enableNewAccountCreation && this.state.showAllFieldsRequiredError &&
      <ErrorDiv>
        All fields are required.
      </ErrorDiv>}
    </div>;
  }
}
