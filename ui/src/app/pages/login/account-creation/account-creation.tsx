import {Button} from 'app/components/buttons';
import {FormSection} from 'app/components/forms';

import {ClrIcon, InfoIcon, ValidationIcon} from 'app/components/icons';

import {
  Error as ErrorDiv,
  ErrorMessage,
  RadioButton,
  styles as inputStyles,
  TextArea,
  TextInput
} from 'app/components/inputs';

import {TooltipTrigger} from 'app/components/popups';

import {profileApi} from 'app/services/swagger-fetch-clients';

import {FlexColumn, FlexRow} from 'app/components/flex';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  EducationalRole,
  IndustryRole,
  NonAcademicAffiliation,
  Profile,
} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import {MultiSelect} from 'primereact/multiselect';
import * as React from 'react';
import * as validate from 'validate.js';

import {BulletAlignedUnorderedList} from 'app/components/lists';
import {PubliclyDisplayed} from 'app/icons/publicly-displayed-icon';
import {AccountCreationOptions} from 'app/pages/login/account-creation/account-creation-options';
import {
  commonStyles,
  Section,
  TextInputWithLabel,
  WhyWillSomeInformationBePublic,
} from 'app/pages/login/account-creation/common';
import {isBlank, reactStyles} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {serverConfigStore} from 'app/utils/navigation';

const styles = reactStyles({
  ...commonStyles,
  multiInputSpacing: {
    marginLeft: '2rem'
  },
  publiclyDisplayedText: {
    fontSize: 12,
    fontWeight: 400
  },
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
  onPreviousClick: (profile: Profile) => void;
}

export interface AccountCreationState {
  creatingAccount: boolean;
  errors: any;
  invalidEmail: boolean;
  profile: Profile;
  showAllFieldsRequiredError: boolean;
  usernameCheckInProgress: boolean;
  usernameConflictError: boolean;
  // TODO(RW-4361): remove all after this point, after we switch to verified institutional affiliation
  rolesOptions: any;
  showInstitution: boolean;
  showNonAcademicAffiliationRole: boolean;
  showNonAcademicAffiliationOther: boolean;
  showMostInterestedInKnowingBlurb: boolean;
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
    // TODO(RW-4361): remove after we switch to verified institutional affiliation
    if (!serverConfigStore.getValue().requireInstitutionalVerification &&
      props.profile.institutionalAffiliations.length !== 1) {
      throw new Error('Profile must be pre-allocated with 1 institutional affiliation.');
    }
    super(props);
    this.state = this.createInitialState();
  }

  async componentDidMount() {
    // TODO(RW-4361): Remove after we switch to verified institutional affiliation
    if (!serverConfigStore.getValue().requireInstitutionalVerification) {
      this.updateNonAcademicAffiliationRoles(
        this.state.profile.institutionalAffiliations[0].nonAcademicAffiliation);
      this.selectNonAcademicAffiliationRoles(
        this.state.profile.institutionalAffiliations[0].role);
    }
  }

  createInitialState(): AccountCreationState {
    const state: AccountCreationState = {
      errors: undefined,
      profile: this.props.profile,
      usernameCheckInProgress: false,
      usernameConflictError: false,
      creatingAccount: false,
      showAllFieldsRequiredError: false,
      invalidEmail: false,
      // TODO(RW-4361): remove all after this point, after we switch to verified institutional affiliation
      rolesOptions: [],
      institutionName: '',
      institutionRole: '',
      nonAcademicAffiliation: '',
      nonAcademicAffiliationRole: '',
      nonAcademicAffiliationOther: '',
      // showInstitution defaults to true, since we expect most users coming in will be academics.
      showInstitution: true,
      showNonAcademicAffiliationRole: false,
      showNonAcademicAffiliationOther: false,
      showMostInterestedInKnowingBlurb: false,
    };

    // TODO(RW-4361): remove after we switch to verified institutional affiliation
    if (!serverConfigStore.getValue().requireInstitutionalVerification) {
      const institutionalAffiliation = this.props.profile.institutionalAffiliations[0];
      if (institutionalAffiliation.institution) {
        state.showInstitution = true;
      }
    }

    return state;
  }

  // Returns whether the current username is considered valid. Undefined is returned when the
  // username is empty, or if a username check is in progress.
  isUsernameValid(): (boolean|undefined) {
    if (isBlank(this.state.profile.username) || this.state.usernameCheckInProgress) {
      return undefined;
    }
    return !this.isUsernameValidationError();
  }

  isUsernameValidationError(): boolean {
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

  usernameChanged(username: string): void {
    this.updateProfileObject('username', username);
    if (isBlank(username)) {
      return;
    }
    // TODO: This should use a debounce, rather than manual setTimeout()s.
    clearTimeout(this.usernameCheckTimeout);
    this.setState({usernameCheckInProgress: true, usernameConflictError: false});
    this.usernameCheckTimeout = setTimeout(() => {
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

  updateProfileObject(attribute: string, value) {
    this.setState(fp.set(['profile', attribute], value));
  }

  updateContactEmail(email: string) {
    this.setState({invalidEmail: false});
    this.setState(fp.set(['profile', 'contactEmail'], email));
  }

  updateAddress(attribute: string , value) {
    this.setState(fp.set(['profile', 'address', attribute], value));
  }

  updateInstitutionAffiliation(attribute: string, value) {
    this.setState(fp.set(['profile', 'institutionalAffiliations', '0', attribute], value));
  }

  clearInstitutionAffiliation() {
    this.setState(fp.set(['profile', 'institutionalAffiliations', '0'], {
      nonAcademicAffiliation: null,
      role: '',
      institution: '',
      other: ''
    }));
  }

  // TODO remove after we switch to verified institutional affiliation
  showInstitutionAffiliationFreeTextField(option) {
    return option === NonAcademicAffiliation.FREETEXT ||
      option === IndustryRole.FREETEXT ||
      option === EducationalRole.FREETEXT;
  }

  // TODO(RW-4361): remove after we switch to verified institutional affiliation
  updateNonAcademicAffiliationRoles(nonAcademicAffiliation) {
    this.updateInstitutionAffiliation('nonAcademicAffiliation', nonAcademicAffiliation);
    this.setState({showNonAcademicAffiliationRole: false, showNonAcademicAffiliationOther: false});
    if (nonAcademicAffiliation === NonAcademicAffiliation.INDUSTRY) {
      this.setState({rolesOptions: AccountCreationOptions.industryRole,
        showNonAcademicAffiliationRole: true});
    } else if (nonAcademicAffiliation === NonAcademicAffiliation.EDUCATIONALINSTITUTION) {
      this.setState({rolesOptions: AccountCreationOptions.educationRole, showNonAcademicAffiliationRole: true});
    } else if (this.showInstitutionAffiliationFreeTextField(nonAcademicAffiliation)) {
      this.setState({showNonAcademicAffiliationOther: true});
      return;
    }
    this.selectNonAcademicAffiliationRoles(this.state.nonAcademicAffiliationRole);
  }

  // TODO(RW-4361): remove after we switch to verified institutional affiliation
  selectNonAcademicAffiliationRoles(role) {
    if (this.showInstitutionAffiliationFreeTextField(role)) {
      this.setState({nonAcademicAffiliationRole: role, showNonAcademicAffiliationOther: true});
    } else {
      this.setState({nonAcademicAffiliationRole: role, showNonAcademicAffiliationOther: false});
    }
    this.updateInstitutionAffiliation('role', role);
  }

  validate(): {[key: string]: string} {
    const {showInstitution} = this.state;
    const {gsuiteDomain, requireInstitutionalVerification} = serverConfigStore.getValue();

    const validationCheck = {
      'username': {
        presence: {
          allowEmpty: false,
          message: '^Username cannot be blank'
        },
        length: {
          minimum: 4,
          maximum: 64,
        },
      },
      'givenName': {
        presence: {
          allowEmpty: false,
          message: '^First name cannot be blank'
        }
      },
      'familyName': {
        presence: {
          allowEmpty: false,
          message: '^Last name cannot be blank'
        }
      },
      'areaOfResearch': {
        presence: {
          allowEmpty: false,
          message: '^Research description cannot be blank'
        },
        length: {
          maximum: 2000,
          message: '^Research description must be 2000 characters or fewer'
        }
      },
      'address.streetAddress1': {
        presence: {
          allowEmpty: false,
          message: '^Street address cannot be blank'
        }
      },
      'address.city': {
        presence: {
          allowEmpty: false,
          message: '^City cannot be blank'
        }
      },
      'address.state': {
        presence: {
          allowEmpty: false,
          message: '^State cannot be blank'
        }
      },
      'address.zipCode': {
        presence: {
          allowEmpty: false,
          message: '^Zip code cannot be blank'
        }
      },
      'address.country': {
        presence: {
          allowEmpty: false,
          message: '^Country cannot be blank'
        }
      },
    };

    // The validation data for this form is *almost* the raw Profile, except for the additional
    // 'usernameWithEmail' field we're adding, to be able to separate our validation on the
    // username itself from validation of the full email address. For this reason, we need to cast
    // the profile object to 'any'.
    let validationData = {...this.state.profile} as any;
    validationData.usernameWithEmail = validationData.username + '@' + gsuiteDomain;

    if (!isBlank(validationData.username)) {
      validationCheck['usernameWithEmail'] = {
        email: {
          message: '^Username contains invalid characters'
        }
      };
    }

    // TODO(RW-4361): remove these checks after we switch to verified institutional affiliation
    if (!requireInstitutionalVerification) {
      validationData = {...validationData, ...this.state.profile.institutionalAffiliations[0]};

      validationCheck['contactEmail'] = {
        presence: {
          allowEmpty: false,
          message: '^Contact email cannot be blank'
        },
        email: {
          message: '^Contact email is invalid'
        }
      };

      if (showInstitution) {
        validationCheck['institution'] = {
          presence: {
            allowEmpty: false,
            message: '^Institution cannot be blank'
          }
        };
      } else {
        validationCheck['nonAcademicAffiliation'] = {
          presence: {
            allowEmpty: false,
            message: '^Affiliation cannot be blank'
          }
        };
      }

      if (showInstitution ||
        this.state.profile.institutionalAffiliations[0].nonAcademicAffiliation !== NonAcademicAffiliation.COMMUNITYSCIENTIST) {
        validationCheck['role'] = {
          presence: {
            allowEmpty: false,
            message: '^Role cannot be blank'
          }
        };
      }
    }
    return validate(validationData, validationCheck);
  }

  render() {
    const {
      profile: {
        givenName, familyName,
        contactEmail, username, areaOfResearch, professionalUrl,
        address: {
          streetAddress1, streetAddress2, city, state, zipCode, country
        },
      },
    } = this.state;
    const {gsuiteDomain, requireInstitutionalVerification} = serverConfigStore.getValue();

    const usernameLabelText =
      <div>New Username
        <TooltipTrigger side='top' content={<div>Usernames can contain only: <ul style={{marginLeft: '0.1rem'}}>
          <li>letters (a-z)</li>
          <li>numbers (0-9)</li>
          <li>dashes (-)</li>
          <li>underscores (_)</li>
          <li>apostrophes (')</li>
          <li>periods (.)</li>
          <li>minimum of 3 characters</li>
          <li>maximum of 64 characters</li>
        </ul>
          <br/>Usernames cannot begin or end with a period (.) and may not
          contain more than one period (.) in a row.</div>}
                        style={{marginLeft: '0.5rem'}}>
          <InfoIcon style={{'height': '16px', 'paddingLeft': '2px'}}/>
        </TooltipTrigger>
      </div>;

    const areaOfResearchCharactersRemaining = 2000 - areaOfResearch.length;

    const errors = this.validate();

    return <div id='account-creation'
                style={{paddingTop: '1.5rem', paddingRight: '3rem', paddingLeft: '1rem'}}>
      <div style={{fontSize: 28, fontWeight: 400, color: colors.primary}}>Create your account</div>
      <FlexRow>
        <FlexColumn style={{marginRight: '2rem'}}>
          <div style={{...styles.text, fontSize: 16, marginTop: '1rem'}}>
            Please complete Step {requireInstitutionalVerification ? '2 of 3' : '1 of 2'}
          </div>
          <div style={{...styles.text, fontSize: 12, marginTop: '0.3rem'}}>
            All fields required unless indicated as optional</div>
          <Section header={<div>Create an <i>All of Us</i> username</div>} style={{marginTop: '1.25rem'}}>
            <div>
              <FlexRow>
                <FlexColumn>
                  <div style={styles.asideText}>
                    We create a 'username'@{gsuiteDomain} Google account which you will use to
                    login to the Workbench. This is a separate account and not related to any
                    personal or professional Google accounts you may have.</div>
                  <TextInputWithLabel
                      value={username}
                      inputId='username'
                      inputName='username'
                      placeholder='New Username'
                      invalid={this.state.usernameConflictError || this.usernameInvalidError()}
                      containerStyle={{width: '26rem', marginTop: '1.25rem'}}
                      labelText={usernameLabelText}
                      onChange={v => this.usernameChanged(v)}
                  >
                    <div style={inputStyles.iconArea}>
                      <ValidationIcon validSuccess={this.isUsernameValid()}/>
                    </div>
                    <i style={{...styles.asideText, marginLeft: 4}}>@{gsuiteDomain}</i>
                  </TextInputWithLabel>
                </FlexColumn>

              </FlexRow>
              {this.state.usernameConflictError &&
              <div style={{height: '1.5rem'}}>
                <ErrorDiv id='usernameConflictError'>
                  Username is already taken.
                </ErrorDiv></div>}
              {this.usernameInvalidError() &&
                <div style={{height: '1.5rem'}}><ErrorDiv id='usernameError'>
                  {username} is not a valid username.
                </ErrorDiv></div>
              }
            </div>
          </Section>
          <Section
            header={
              <FlexRow style={{alignItems: 'center'}}>
                <div>About you</div>
                <PubliclyDisplayed style={{marginLeft: '1rem'}}/>
              </FlexRow>
            }
            style={{marginTop: '2rem'}}
          >
            <FlexColumn>
              <FlexRow style={{marginTop: '.25rem'}}>
                <TextInputWithLabel value={givenName}
                                    inputId='givenName'
                                    inputName='givenName'
                                    placeholder='First Name'
                                    invalid={givenName.length > nameLength}
                                    labelText='First Name'
                                    onChange={value => this.updateProfileObject('givenName', value)} />
                {givenName.length > nameLength &&
                <ErrorMessage id='givenNameError'>
                  First Name must be {nameLength} characters or less.
                </ErrorMessage>}
                <TextInputWithLabel value={familyName}
                                    inputId='familyName'
                                    inputName='familyName'
                                    placeholder='Last Name'
                                    invalid={familyName.length > nameLength}
                                    containerStyle={styles.multiInputSpacing}
                                    onChange={v => this.updateProfileObject('familyName', v)}
                                    labelText='Last Name'/>
                {familyName.length > nameLength &&
                <ErrorMessage id='familyNameError'>
                  Last Name must be {nameLength} character or less.
                </ErrorMessage>}
              </FlexRow>
              {!requireInstitutionalVerification &&
                <FlexRow style={{alignItems: 'left', marginTop: '1rem'}}>
                  <TextInputWithLabel value={contactEmail}
                                      inputId='contactEmail'
                                      inputName='contactEmail'
                                      placeholder='Email Address'
                                      labelText='Email Address'
                                      onChange={v => this.updateProfileObject('contactEmail', v)}/>
                  <MultiSelectWithLabel placeholder={'Select one or more'}
                                        options={AccountCreationOptions.degree}
                                        containerStyle={styles.multiInputSpacing}
                                        value={this.state.profile.degrees}
                                        labelText='Your degrees (optional)'
                                        onChange={(e) => this.setState(fp.set(['profile', 'degrees'], e.value))}
                                        />
                </FlexRow>
              }
              {requireInstitutionalVerification &&
                <div style={{marginTop: '1rem'}}>
                  <MultiSelectWithLabel placeholder={'Select one or more'}
                                        options={AccountCreationOptions.degree}
                                        value={this.state.profile.degrees}
                                        labelText='Your degrees (optional)'
                                        onChange={(e) => this.setState(fp.set(['profile', 'degrees'], e.value))}
                  />
                </div>
              }
            </FlexColumn>
          </Section>
          <Section
            header={<React.Fragment>
                <div>Your institutional mailing address</div>
                <div style={styles.asideText}>We will use your address if we need to send correspondence about the program.</div>
              </React.Fragment>
            }
            style={{marginTop: '2rem'}}
          >
            <FlexColumn style={{lineHeight: '1rem'}}>
              <FlexRow style={{marginTop: '1rem'}}>
                <TextInputWithLabel dataTestId='streetAddress' inputName='streetAddress'
                                    placeholder='Street Address' value={streetAddress1} labelText='Street Address 1'
                                    onChange={value => this.updateAddress('streetAddress1', value)}/>
                <TextInputWithLabel dataTestId='streetAddress2' inputName='streetAddress2' placeholder='Street Address 2'
                                    value={streetAddress2} labelText='Street Address 2'
                                    containerStyle={styles.multiInputSpacing}
                                    onChange={value => this.updateAddress('streetAddress2', value)}/>
              </FlexRow>
              <FlexRow style={{marginTop: '1rem'}}>
                <TextInputWithLabel dataTestId='city' inputName='city' placeholder='City' value={city} labelText='City'
                                    onChange={value => this.updateAddress('city', value)}/>
                <TextInputWithLabel dataTestId='state' inputName='state' placeholder='State' value={state} labelText='State'
                                    containerStyle={styles.multiInputSpacing}
                                    onChange={value => this.updateAddress('state', value)}/>
              </FlexRow>
              <FlexRow style={{marginTop: '1rem'}}>
                <TextInputWithLabel dataTestId='zip' inputName='zip' placeholder='Zip code'
                                    value={zipCode} labelText='Zip code'
                                    onChange={value => this.updateAddress('zipCode', value)}/>
                <TextInputWithLabel dataTestId='country' inputName='country' placeholder='Country' value={country}
                                    labelText='Country' containerStyle={styles.multiInputSpacing}
                                    onChange={value => this.updateAddress('country', value)}/>
              </FlexRow>
            </FlexColumn>
          </Section>
          <Section
            header={<React.Fragment>
              <FlexRow style={{alignItems: 'flex-start'}}>
                <div style={{maxWidth: '60%'}}>Your research background, experience, and research interests</div>
                <PubliclyDisplayed style={{marginLeft: '1rem'}}/>
              </FlexRow>
              <div
                style={{...styles.asideText, marginTop: '.125px'}}>
                  This information will be posted publicly on the <i>All of Us</i> Research Hub website to inform program participants.
                  <span style={{marginLeft: 2, fontSize: 12}}>(2000 character limit)</span>
              </div>
              <div style={{marginTop: '.5rem'}}>
                <FlexRow style={{color: colors.accent, alignItems: 'center'}}>
                  <div
                      style={{cursor: 'pointer', fontSize: 14}}
                      onClick={() => this.setState(
                        (previousState) => ({showMostInterestedInKnowingBlurb: !previousState.showMostInterestedInKnowingBlurb})
                      )}
                  >
                    <i>All of Us</i> participants are most interested in knowing:
                  </div>
                  <ClrIcon shape='angle' style={{
                    transform: this.state.showMostInterestedInKnowingBlurb ? 'rotate(180deg)' : 'rotate(90deg)'
                  }}/>
                </FlexRow>
                {this.state.showMostInterestedInKnowingBlurb &&
                  <ul style={{...styles.asideList, marginLeft: '0.75rem'}}>
                    {researchPurposeList.map((value, index) => <li key={index} style={styles.asideText}>{value}</li>)}
                  </ul>
                }
              </div>
            </React.Fragment>}
            style={{marginTop: '2rem'}}
            sectionHeaderStyles={{borderBottom: null}}
          >
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
              {
                areaOfResearchCharactersRemaining > 0 && <div data-test-id='charRemaining'>
                  {areaOfResearchCharactersRemaining} characters remaining</div>
              }
              {
                areaOfResearchCharactersRemaining === 0 && <div data-test-id='charRemaining' style={{color: colors.danger}}>
                  {areaOfResearchCharactersRemaining} characters remaining</div>
              }
              {
                areaOfResearchCharactersRemaining < 0 && <div data-test-id='charOver' style={{color: colors.danger}}>
                  {-areaOfResearchCharactersRemaining} characters over</div>
              }
            </FlexRow>
          </Section>
          {/* TODO(RW-4361): remove after we switch to verified institutional affiliation */}
          {!requireInstitutionalVerification && <React.Fragment>
            <Section header='Institutional Affiliation'>
              <label style={{color: colors.primary, fontSize: 16}}>
                Are you affiliated with an Academic Research Institution?
              </label>
              <div style={{paddingTop: '0.5rem'}}>
                <RadioButton id='show-institution-yes'
                             data-test-id='show-institution-yes'
                             onChange={() => {
                               this.clearInstitutionAffiliation();
                               this.setState({showInstitution: true});
                             }}
                             checked={this.state.showInstitution === true}
                             style={{marginRight: '0.5rem'}}/>
                <label htmlFor='show-institution-yes' style={{paddingRight: '3rem', color: colors.primary}}>
                  Yes
                </label>
                <RadioButton id='show-institution-no'
                             data-test-id='show-institution-no'
                             onChange={() => {
                               this.clearInstitutionAffiliation();
                               this.setState({showInstitution: false});
                             }}
                             checked={this.state.showInstitution === false} style={{marginRight: '0.5rem'}}/>
                <label htmlFor='show-institution-no' style={{color: colors.primary}}>No</label>
              </div>
            </Section>
            {this.state.showInstitution &&
            <FlexColumn style={{justifyContent: 'space-between'}}>
              <TextInput data-test-id='institution-name'
                         style={{width: '16rem', marginBottom: '0.5rem', marginTop: '0.5rem'}}
                         value={this.state.profile.institutionalAffiliations[0].institution}
                         placeholder='Institution Name'
                         onChange={value => this.updateInstitutionAffiliation('institution', value)}
              />
              <Dropdown data-test-id='institutionRole'
                        value={this.state.profile.institutionalAffiliations[0].role}
                        onChange={e => this.updateInstitutionAffiliation('role', e.value)}
                        placeholder='Which of the following describes your role'
                        style={{width: '16rem'}} options={AccountCreationOptions.roles}/>
            </FlexColumn>}
            {!this.state.showInstitution &&
            <FlexColumn style={{justifyContent: 'space-between'}}>
              <Dropdown data-test-id='affiliation'
                        style={{width: '18rem', marginBottom: '0.5rem', marginTop: '0.5rem'}}
                        value={this.state.profile.institutionalAffiliations[0].nonAcademicAffiliation}
                        options={AccountCreationOptions.nonAcademicAffiliations}
                        onChange={e => this.updateNonAcademicAffiliationRoles(e.value)}
                        placeholder='Which of the following better describes your affiliation?'/>
              {this.state.showNonAcademicAffiliationRole &&
              <Dropdown data-test-id='affiliationrole'
                        placeholder='Which of the following describes your role'
                        options={this.state.rolesOptions}
                        value={this.state.profile.institutionalAffiliations[0].role}
                        onChange={e => this.selectNonAcademicAffiliationRoles(e.value)}
                        style={{width: '18rem'}}/>}
              {this.state.showNonAcademicAffiliationOther &&
              <TextInput value={this.state.profile.institutionalAffiliations[0].other}
                         onChange={value => this.updateInstitutionAffiliation('other', value)}
                         style={{marginTop: '1rem', width: '18rem'}}/>}
            </FlexColumn>}
          </React.Fragment>}
          <Section
            header={<React.Fragment>
              <FlexRow style={{alignItems: 'flex-start'}}>
                <div style={{maxWidth: '60%'}}>Your professional profile or bio page below, if available</div>
                <PubliclyDisplayed style={{marginLeft: '1rem'}}/>
              </FlexRow>
              <div style={{...styles.asideText, marginTop: '.125rem'}}>(Optional)</div>
              <div style={{...styles.asideText, marginTop: '.5rem', marginBottom: '.5rem'}}>
                You could provide a link to your faculty bio page from your institution's website,
                your LinkedIn profile page, or another webpage featuring your work. This will
                allow <i>All of Us</i> researchers and participants to learn more about your work and
                publications.
              </div>
            </React.Fragment>}
            style={{marginTop: '2rem'}}
          >
            <TextInputWithLabel
              dataTestId='professionalUrl'
              inputName='professionalUrl'
              placeholder='Professional Url'
              value={professionalUrl}
              labelText={<div>Paste Professional URL here</div>}
              containerStyle={{width: '26rem', marginTop: '.25rem'}}
              onChange={value => this.updateProfileObject('professionalUrl', value)}
            />
          </Section>
          <FormSection style={{marginTop: '4rem', paddingBottom: '1rem'}}>
            <Button type='secondary' style={{marginRight: '1rem'}}
                    onClick={() => this.props.onPreviousClick(this.state.profile)}>
              Previous
            </Button>
            <TooltipTrigger content={errors && <React.Fragment>
              <div>Please review the following: </div>
              <BulletAlignedUnorderedList>
                {Object.keys(errors).map((key) => <li key={errors[key][0]}>{errors[key][0]}</li>)}
              </BulletAlignedUnorderedList>
            </React.Fragment>} disabled={!errors}>
              <Button disabled={this.state.usernameCheckInProgress ||
                                this.isUsernameValidationError() ||
                                Boolean(errors)}
                      style={{'height': '2rem', 'width': '10rem'}}
                      onClick={() => {
                        AnalyticsTracker.Registration.CreateAccountPage();
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
