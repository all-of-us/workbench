import {Button} from 'app/components/buttons';
import {FormSection} from 'app/components/forms';

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
  TextInput
} from 'app/components/inputs';

import {
  TooltipTrigger
} from 'app/components/popups';

import {
  profileApi
} from 'app/services/swagger-fetch-clients';

import {FlexColumn, FlexRow, flexStyle} from 'app/components/flex';
import {signedOutImages} from 'app/pages/login/signed-out-images';
import {AoUTitle} from 'app/pages/profile/data-use-agreement-styles';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  DataAccessLevel,
  Degree,
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

import {reactStyles} from 'app/utils';
import {serverConfigStore} from 'app/utils/navigation';
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
    borderBottom: `1px solid ${colors.primary}`,
    color: colors.primary,
    fontWeight: 600,
    fontSize: 18,
    marginBottom: '1rem',
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
  <span>How you hope to use <i>All of Us</i> data for your research.</span>,
  <span>Your research approach and the tools you use for answering your research questions (eg: Large datasets
     of phenotypes and genotypes, Community engagement and community-based participatory research methods, etc)</span>,
  <span>Your experience working with underrepresented populations as a scientist or outside of research, and how that
     experience may inform your work with <i>All of Us</i> data</span>
];

const nameLength = 80;

export const Section = (props) => {
  return <FormSection
      style={{...flexStyle.column, ...props.style}}>
    <label style={{...styles.sectionHeader, ...props.sectionHeaderStyles}}>
      {props.header}
    </label>
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
      <MultiSelect placeholder={props.placeholder}
                   value={props.value} onChange={props.onChange}
                   options={props.options} data-test-id={props.dataTestId}
                   style={{...styles.sectionInput, overflowY: 'none'}}/>
      {props.children}
    </FlexRow>
  </FlexColumn>;
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
            nonAcademicAffiliation: undefined,
            role: undefined
          }
        ],
        degrees: [] as Degree[]
      },
      usernameCheckInProgress: false,
      usernameConflictError: false,
      creatingAccount: false,
      showAllFieldsRequiredError: false,
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
  }

  componentDidMount() {
    if (this.props.profile.address) {
      const {institutionalAffiliations} = this.props.profile;
      if (institutionalAffiliations[0].institution) {
        this.setState({showInstitution: true});
      } else {
        this.setState({showInstitution: false});
        this.updateNonAcademicAffiliationRoles(institutionalAffiliations[0].nonAcademicAffiliation);
        this.selectNonAcademicAffiliationRoles(institutionalAffiliations[0].role);
      }
      this.setState({profile: this.props.profile});


    }
  }

  // This method will be deleted once we enable new account pages
  createAccount(): void {
    const {invitationKey, setProfile} = this.props;
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
        setProfile(savedProfile, {stepName: 'accountCreationSuccess', backgroundImages: signedOutImages.login}); })
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

  showFreeTextField(option) {
    return option === NonAcademicAffiliation.FREETEXT || option === IndustryRole.FREETEXT ||
        option === EducationalRole.FREETEXT;
  }

  clearInstitutionAffiliation() {
    this.updateInstitutionAffiliation('nonAcademicAffiliation', '');
    this.updateInstitutionAffiliation('role', '');
    this.updateInstitutionAffiliation('institution', '');
    this.updateInstitutionAffiliation('other', '');
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
      degrees: {
        length: {
          minimum: 1,
          tooShort: 'Please provide at least one degree option (if none, select \'none\'.)',
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
    const {institutionalAffiliations} = this.state.profile;
    return institutionalAffiliations &&
      institutionalAffiliations.length > 0 ? institutionalAffiliations[0][property] : '';
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
                <Error id='usernameConflictError'>
                  Username is already taken.
                </Error></div>}
              {this.usernameInvalidError() &&
              <div style={{height: '1.5rem'}}><Error id='usernameError'>
                {username} is not a valid username.
              </Error></div>}
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
                <Error id='invalidEmailError'>
                  Contact Email is invalid
                </Error>}
                <MultiSelectWithLabel placeholder={'You can select more than one'} options={AccountCreationOptions.degree}
                                      containerStyle={styles.multiInputSpacing} value={this.state.profile.degrees}
                                      labelText='Your degree'
                                      onChange={(e) => this.updateProfileObject('degrees', e.value)}/>
              </FlexRow>
            </FlexColumn>
          </Section>
          <Section header={<React.Fragment>
            <div>Your institutional mailing address</div>
            <div style={styles.asideText}>We use your address if we need to send correspondence about the program;
              your information will not be shared or displayed publicly</div>
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
                <TextInputWithLabel dataTestId='zip' inputName='zip' placeholder='Zip Code'
                                    value={zipCode} labelText='Zip Code'
                                    onChange={value => this.updateAddress('zipCode', value)}/>
                <TextInputWithLabel dataTestId='country' inputName='country' placeholder='Country' value={country}
                                    labelText='Country' containerStyle={styles.multiInputSpacing}
                                    onChange={value => this.updateAddress('country', value)}/>
              </FlexRow>
            </FlexColumn>
          </Section>
          <Section sectionHeaderStyles={{borderBottom: null}} header={<React.Fragment>
            <div>Please describe your research background, experience and research interests</div>
            <div style={styles.asideText}>This information will be posted publicly on the <i>All of Us</i> Research Hub Website
              to inform the <i>All of Us</i> Research Participants. <span  style={{marginLeft: 2,
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
              <RadioButton data-test-id='show-institution-yes'
                           onChange={() => {this.clearInstitutionAffiliation();
                             this.setState({showInstitution: true}); }}
                           checked={this.state.showInstitution} style={{marginRight: '0.5rem'}}/>
              <label style={{paddingRight: '3rem', color: colors.primary}}>
                Yes
              </label>
              <RadioButton data-test-id='show-institution-no'
                           onChange={() => {this.clearInstitutionAffiliation();
                             this.setState({showInstitution: false}); }}
                           checked={!this.state.showInstitution} style={{marginRight: '0.5rem'}}/>
              <label style={{color: colors.primary}}>No</label>
            </div>
          </Section>
          {this.state.showInstitution &&
          <FlexColumn style={{justifyContent: 'space-between'}}>
            <TextInput data-test-id='institutionname' style={{width: '16rem', marginBottom: '0.5rem',
              marginTop: '0.5rem'}}
              value={this.getInstitutionalAffiliationPropertyOrEmptyString('institution')}
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
            <div>Please your professional profile or bio page below, if available</div>
            <div style={styles.asideText}>You could provide link to your faculty bio page from your institution's
              website, your LinkedIn profile page, or another webpage featuring your work. This will
              allow <i>All of Us</i> Researchers and Participants to learn more about your work and publications.</div>
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
                      onClick={() => this.props.setProfile(this.state.profile, {stepName: 'accountCreationSurvey'})}>
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
            <div style={styles.asideText}>The <AoUTitle/> is committed to transparency with the Research
                participants on who can access their data, and the purpose of such access. Therefore, your name,
                institution and role, as well as your research background/interests and link to your professional
                profile will be displayed publicly on the Research Projects Directory on the <AoUTitle/> website to
                inform the <i>All of Us</i> Research participants, and to comply with the 21st Century Cures Act. Some of the
                fields noted above may not be visible currently, but will be added in the future.</div>
          </FlexColumn>
          <FlexColumn style={{...styles.asideContainer, marginTop: '21.8rem', height: '15rem'}}>
            <div style={styles.asideHeader}><i>All of Us</i> participants are most interested in knowing:</div>
            <ul style={styles.asideList}>
              {researchPurposeList.map(value => <li style={styles.asideText}>{value}</li>)}
            </ul>
          </FlexColumn>
        </FlexColumn>
      </FlexRow>}
      {/*The following will be deleted once enableAccountPages is set to true in prod*/}
      {!enableNewAccountCreation && <div>
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
      {!enableNewAccountCreation && this.state.showAllFieldsRequiredError &&
      <Error>
        All fields are required.
      </Error>}
    </div>;
  }
}
