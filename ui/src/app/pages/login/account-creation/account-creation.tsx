import {Button} from 'app/components/buttons';
import {FormSection} from 'app/components/forms';
import {Header} from 'app/components/headers';

import {
  InfoIcon,
  ValidationIcon
} from 'app/components/icons';

import {
  Error,
  ErrorMessage,
  RadioButton,
  styles as inputStyles,
  TextArea,
  TextInput,
  ValidationError
} from 'app/components/inputs';

import {
  TooltipTrigger
} from 'app/components/popups';

import {
  profileApi
} from 'app/services/swagger-fetch-clients';

import {
  DataAccessLevel,
  Profile,
} from 'generated/fetch/api';

import colors from 'app/styles/colors';
import {summarizeErrors} from 'app/utils/index';
import {environment} from 'environments/environment';
import {
  AffiliationRole,
  EducationalRole,
  IndustryRole,
  InstitutionalAffiliation
} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';
import * as validate from 'validate.js';
import {AccountCreationOptions} from './account-creation-options';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}


export interface AccountCreationProps {
  profile: Profile;
  invitationKey: string;
  setProfile: Function;
}

export interface AccountCreationState {
  creatingAccount: boolean;
  errors: any;
  invalidEmail: boolean;
  rolesOptions: any;
  profile: Profile;
  showAllFieldsRequiredError: boolean;
  showInstitution: boolean;
  showAffiliationRole: boolean;
  showAffiliationOther: boolean;
  usernameCheckInProgress: boolean;
  usernameConflictError: boolean;
  institutionName: string;
  institutionRole: string;
  affiliation: string;
  affiliationRole: string;
  affiliationOther: string;
}

const styles = {
  sectionLabel: {
    height: '22px',
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: '16px',
    fontWeight: 600,
    lineHeight: '22px',
    paddingBottom: '1.5rem'
  },
  section: {
    width: '12rem'
  }
};

const nameLength = 80;

export const Section = (props) => {
  return <FormSection
      style={{display: 'flex', flexDirection: 'column', paddingTop: '1rem', ...props.style}}>
    <label style={styles.sectionLabel}>
      {props.header}
    </label>
    {props.children}
  </FormSection>;
};

export class AccountCreation extends React.Component<AccountCreationProps, AccountCreationState> {
  private usernameCheckTimeout: NodeJS.Timer;

  constructor(props: AccountCreationProps) {
    super(props);
    this.state = {
      errors: undefined,
      profile: {
        // Note: We abuse the "username" field here by omitting "@domain.org". After
        // profile creation, this field is populated with the full email address.
        username: '',
        dataAccessLevel: DataAccessLevel.Unregistered,
        givenName: '',
        familyName: '',
        contactEmail: '',
        currentPosition: '',
        organization: '',
        areaOfResearch: '',
        address: {
          streetAddress1: '',
          streetAddress2: '',
          city: '',
          state: '',
          country: '',
          zipCode: '',
        },
        institutionalAffiliations: [
          {
            institution: undefined,
            affiliation: undefined,
            role: undefined
          }
        ]
      },
      usernameCheckInProgress: false,
      usernameConflictError: false,
      creatingAccount: false,
      showAllFieldsRequiredError: false,
      showInstitution: true,
      showAffiliationRole: false,
      showAffiliationOther: false,
      invalidEmail: false,
      rolesOptions: [],
      institutionName: '',
      institutionRole: '',
      affiliation: '',
      affiliationRole: '',
      affiliationOther: ''
    };
  }

  componentDidMount() {
    if (this.props.profile.address) {
      this.setState({profile: this.props.profile});
    }
  }

  // This method will be deleted once we enable new account pages
  createAccount(): void {
    const {invitationKey, setProfile} = this.props;
    const profile = this.state.profile;
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
        setProfile(savedProfile, 'accountCreationSuccess'); })
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

  // As of now we can add just one industry name role, this will change later
  updateInstitutionAffiliation(attribute: string, value) {
    const profile = this.state.profile;
    if (profile.institutionalAffiliations && profile.institutionalAffiliations.length > 0) {
      profile.institutionalAffiliations[0][attribute] = value;
    } else {
      const institutionalAffiliation = {} as InstitutionalAffiliation;
      institutionalAffiliation[attribute] = value;
      profile.institutionalAffiliations = [institutionalAffiliation];
    }
    this.setState({profile: profile});
  }

  showOtherText(option) {
    return option === AffiliationRole.FREETEXT || option === IndustryRole.FREETEXT ||
        option === EducationalRole.FREETEXT;
  }

  updateAffiliationRoles(affiliation) {
    this.updateInstitutionAffiliation('affiliation', affiliation);
    this.setState({showAffiliationRole: false, showAffiliationOther: false});
    if (affiliation === AffiliationRole.INDUSTRY) {
      this.setState({rolesOptions: AccountCreationOptions.industryRole, showAffiliationRole: true});
    } else if (affiliation === AffiliationRole.EDUCATIONALINSTITUTION) {
      this.setState({rolesOptions: AccountCreationOptions.eductionRole, showAffiliationRole: true});
    } else if (this.showOtherText(affiliation)) {
      this.setState({showAffiliationOther: true});
      return;
    }
    this.selectAffiliationRoles(this.state.affiliationRole);
  }

  selectAffiliationRoles(role) {
    if (this.showOtherText(role)) {
      this.setState({affiliationRole: role, showAffiliationOther: true});
    } else {
      this.setState({affiliationRole: role, showAffiliationOther: false});
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
      profile: {
        givenName, familyName, contactEmail, username,
        address: {
          streetAddress1, city, state, country
        }
      }
    } = this.state;
    const {institution, affiliation, role} = this.state.profile.institutionalAffiliations[0];

    const presenceCheck = {
      presence: {
        allowEmpty: false
      }
    };

    const validationCheck = {
      givenName: presenceCheck,
      familyName: presenceCheck,
      contactEmail: {
        presence: presenceCheck,
        format: {
          pattern: /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$/,
          message: 'Invalid email address'
        }
      },
      streetAddress1: presenceCheck,
      city: presenceCheck,
      state: presenceCheck,
      country: presenceCheck
    };

    showInstitution ? validationCheck['institution'] = presenceCheck :
      validationCheck['affiliation'] = presenceCheck;

    if (showInstitution || affiliation !== AffiliationRole.COMMUNITYSCIENTIST) {
      validationCheck['role'] = presenceCheck;
    }


    const errors = validate({}, validationCheck);
    this.setState({errors: errors}, () => {
      if (!this.state.errors) {
        this.props.setProfile(this.state.profile, 'accountCreationSurvey');
      }
    });
  }


  render() {
    const {
      profile: {
        givenName, familyName, currentPosition, organization,
        contactEmail, username, areaOfResearch,
        address: {
          streetAddress1, streetAddress2, city, state, zipCode, country
        },
        institutionalAffiliations
      },
    } = this.state;
    return <div id='account-creation'
                style={{paddingTop: environment.enableAccountPages ? '1.5rem' :
                      '3rem', paddingRight: '3rem', paddingLeft: '3rem'}}>
      <Header>Create your account</Header>
      {environment.enableAccountPages && <div style={{marginTop: '0.5rem'}}>
        <label style={{color: colors.primary, fontSize: 16}}>
          Please complete Step 1 of 2
        </label>
        {this.state.errors && <div className='error-messages'>
          <ValidationError>
            {summarizeErrors(this.state.errors)}
          </ValidationError>
        </div>}
        <Section header='Create an All of Us username'>
          <div>
            <TextInput id='username' name='username' placeholder='New Username'
                       value={username}
                       onChange={v => this.usernameChanged(v)}
                       invalid={this.state.usernameConflictError || this.usernameInvalidError()}
                       style={{...styles.section, marginRight: '0.5rem'}}/>
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
            {this.state.usernameConflictError &&
            <div style={{height: '1.5rem'}}>
              <Error id='usernameConflictError'>
                Username is already taken.
              </Error></div>}
            {this.usernameInvalidError() &&
            <div style={{height: '1.5rem'}}><Error id='usernameError'>
              Username is not a valid username.
            </Error></div>}
          </div>
        </Section>
        <Section header='About you'>
          <div style={{display: 'flex', flexDirection: 'column'}}>
            <div style={{paddingBottom: '0.5rem'}}>
              <TextInput id='givenName' name='givenName' autoFocus
                         placeholder='First Name'
                         value={givenName}
                         invalid={givenName.length > nameLength}
                         style={{...styles.section, marginRight: '2rem'}}
                         onChange={value => this.updateProfileObject('givenName', value)}/>
              {givenName.length > nameLength &&
              <ErrorMessage id='givenNameError'>
                First Name must be 80 characters or less.
              </ErrorMessage>}
              <TextInput id='familyName' name='familyName' placeholder='Last Name'
                         value={familyName}
                         invalid={familyName.length > nameLength}
                         style={styles.section}
                         onChange={v => this.updateProfileObject('familyName', v)}/>
              {familyName.length > nameLength &&
              <ErrorMessage id='familyNameError'>
                Last Name must be 80 character or less.
              </ErrorMessage>}
            </div>
            <TextInput id='contactEmail' name='contactEmail'
                       placeholder='Email Address'
                       value={contactEmail}
                       style={styles.section}
                       onChange={v => this.updateProfileObject('contactEmail', v)}/>
            {this.state.invalidEmail &&
            <Error id='invalidEmailError'>
              Contact Email Id is invalid
            </Error>}
          </div>
        </Section>
        <Section header='Your address'>
          <div
              style={{display: 'flex', flexDirection: 'row', flexWrap: 'wrap', lineHeight: '1rem'}}>
            <TextInput data-test-id='streetAddress' name='streetAddress'
                       placeholder='Street Address' value={streetAddress1}
                       onChange={value => this.updateAddress('streetAddress1', value)}
                       style={{...styles.section, marginRight: '2rem', marginBottom: '0.5rem'}}/>
            <TextInput data-test-id='state' name='state' placeholder='State' value={state}
                       onChange={value => this.updateAddress('state', value)}
                       style={{...styles.section, marginBottom: '0.5rem'}}/>
            <TextInput data-test-id='streetAddress2' name='streetAddress2' placeholder='Street Address 2'
                       value={streetAddress2}
                       style={{...styles.section, marginRight: '2rem', marginBottom: '0.5rem'}}
                       onChange={value => this.updateAddress('streetAddress2', value)}/>
            <TextInput data-test-id='zip' name='zip' placeholder='Zip Code' value={zipCode}
                       onChange={value => this.updateAddress('zipCode', value)}
                       style={{...styles.section, marginBottom: '0.5rem'}}/>
            <TextInput data-test-id='city' name='city' placeholder='City' value={city}
                       onChange={value => this.updateAddress('city', value)}
                       style={{...styles.section, marginRight: '2rem'}}/>
            <TextInput data-test-id='country' placeholder='Country' value={country} style={styles.section}
                       onChange={value => this.updateAddress('country', value)}/>
          </div>
        </Section>
        <Section header='Institutional Affiliation'>
          <label style={{color: colors.primary, fontSize: 16}}>
            Are you affiliated with an Academic Research Institution?
          </label>
          <div style={{paddingTop: '0.5rem'}}>
            <RadioButton data-test-id='show-institution-yes'
                         onChange={() => {this.updateInstitutionAffiliation('affiliation', '');
                           this.updateInstitutionAffiliation('role', '');
                           this.setState({showInstitution: true}); }}
                         checked={this.state.showInstitution} style={{marginRight: '0.5rem'}}/>
            <label style={{paddingRight: '3rem', color: colors.primary}}>
              Yes
            </label>
            <RadioButton data-test-id='show-institution-no'
                         onChange={() => {this.updateInstitutionAffiliation('institution', '');
                           this.updateInstitutionAffiliation('role', ''); this.setState({showInstitution: false}); }}
                         checked={!this.state.showInstitution} style={{marginRight: '0.5rem'}}/>
            <label style={{color: colors.primary}}>No</label>
          </div>
        </Section>
        {this.state.showInstitution &&
        <div style={{display: 'flex', flexDirection: 'column', justifyContent: 'space-between'}}>
          <TextInput data-test-id='institutionname' style={{width: '16rem', marginBottom: '0.5rem',
            marginTop: '0.5rem'}}
            value={institutionalAffiliations && institutionalAffiliations.length > 0 ?
                institutionalAffiliations[0].institution : ''}
            placeholder='Institution Name'
            onChange={value => this.updateInstitutionAffiliation('institution', value)}
                     ></TextInput>
          <Dropdown data-test-id='institutionRole' value={institutionalAffiliations && institutionalAffiliations.length > 0 ?
              institutionalAffiliations[0].role : ''}
                    onChange={e => this.updateInstitutionAffiliation('role', e.value)}
                    placeholder='Which of the following describes your role'
                    style={{width: '16rem'}} options={AccountCreationOptions.roles}/>
        </div>}
        {!this.state.showInstitution &&
        <div style={{display: 'flex', flexDirection: 'column', justifyContent: 'space-between'}}>
          <Dropdown data-test-id='affiliation'
                    style={{width: '18rem', marginBottom: '0.5rem', marginTop: '0.5rem'}}
                    value={institutionalAffiliations && institutionalAffiliations.length > 0 ?
                        institutionalAffiliations[0].affiliation : ''}
                    options={AccountCreationOptions.affiliations}
                    onChange={e => this.updateAffiliationRoles(e.value)}
                    placeholder='Which of the following better describes your affiliation?'/>
          {this.state.showAffiliationRole &&
          <Dropdown data-test-id='affiliationrole' placeholder='Which of the following describes your role'
                    options={this.state.rolesOptions} value={institutionalAffiliations && institutionalAffiliations.length > 0 ?
              institutionalAffiliations[0].role : ''}
                    onChange={e => this.selectAffiliationRoles(e.value)}
                    style={{width: '18rem'}}/>}
          {this.state.showAffiliationOther &&
          <TextInput value={institutionalAffiliations && institutionalAffiliations.length > 0 ?
              institutionalAffiliations[0].other : ''}
                     onChange={value => this.updateInstitutionAffiliation('other', value)}
                     style={{marginTop: '1rem', width: '18rem'}}/>}
        </div>}
        <FormSection style={{paddingBottom: '1rem'}}>
          <Button disabled={this.state.usernameCheckInProgress || this.isUsernameValidationError}
                  style={{'height': '2rem', 'width': '10rem'}}
                  onClick={() => this.validateAccountCreation()}>
            Next
          </Button>
        </FormSection>
      </div>}
      {/*The following will be deleted once enableAccountPages is set to true in prod*/}
      {!environment.enableAccountPages && <div>
        <FormSection>
          <TextInput id='givenName' name='givenName' autoFocus
                     placeholder='First Name'
                     value={givenName}
                     invalid={givenName.length > 80}
                     style={{width: '16rem'}}
                     onChange={v => this.updateProfileToBeDeleted('givenName', v)}/>
          {givenName.length > 80 &&
          <ErrorMessage id='givenNameError'>
            First Name must be 80 characters or less.
          </ErrorMessage>}
        </FormSection>
        <FormSection>
          <TextInput id='familyName' name='familyName' placeholder='Last Name'
                     value={familyName}
                     invalid={familyName.length > 80}
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
          <Error id='invalidEmailError'>
            Contact Email Id is invalid
          </Error>}
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
          <TooltipTrigger content='You are required to describe your current research in
                      order to help All of Us improve the Researcher Workbench.'>
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
            <Error id='usernameConflictError'>
              Username is already taken.
            </Error>}
            {this.usernameInvalidError() &&
            <Error id='usernameError'>
              Username is not a valid username.
            </Error>}
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
      {!environment.enableAccountPages && this.state.showAllFieldsRequiredError &&
      <Error>
        All fields are required.
      </Error>}
    </div>;
  }
}
