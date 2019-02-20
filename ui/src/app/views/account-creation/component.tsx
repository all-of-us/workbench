import {Button} from 'app/components/buttons';
import {FormSection} from 'app/components/forms';
import {BoldHeader} from 'app/components/headers';

import {
  InfoIcon,
  ValidationIcon
} from 'app/components/icons';

import { Error, ErrorMessage, styles as inputStyles, TextArea, TextInput } from 'app/components/inputs';

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

import * as fp from 'lodash/fp';
import * as React from 'react';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

export interface AccountCreationProps {
  invitationKey: string;
  setProfile: Function;
}

export interface AccountCreationState {
  profile: Profile;
  usernameCheckInProgress: boolean;
  usernameConflictError: boolean;
  creatingAccount: boolean;
  showAllFieldsRequiredError: boolean;
}

export class AccountCreation extends React.Component<AccountCreationProps, AccountCreationState> {
  private usernameCheckTimeout: NodeJS.Timer;

  constructor(props: AccountCreationProps) {
    super(props);
    this.state = {
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
      },
      usernameCheckInProgress: false,
      usernameConflictError: false,
      creatingAccount: false,
      showAllFieldsRequiredError: false
    };
  }

  createAccount(): void {
    const {invitationKey, setProfile} = this.props;
    const profile = this.state.profile;
    this.setState({showAllFieldsRequiredError: false});
    const requiredFields =
      [profile.givenName, profile.familyName, profile.username, profile.contactEmail,
        profile.currentPosition, profile.organization, profile.areaOfResearch];
    if (requiredFields.some(isBlank)) {
      this.setState({showAllFieldsRequiredError: true});
      return;
    } else if (this.isUsernameValidationError()) {
      return;
    }
    this.setState({creatingAccount: true});
    profileApi().createAccount({profile, invitationKey})
      .then((savedProfile) => {
        this.setState({profile: savedProfile, creatingAccount: false});
        setProfile(savedProfile);
      }
      )
      .catch(error => {
        console.log(error);
        this.setState({creatingAccount: false});
      });
  }

  usernameValid(): boolean {
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

  usernameChanged(value: string): void {
    this.updateProfile('username', value);
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

  updateProfile(attribute: string, value: string) {
    const newProfile = this.state.profile;
    newProfile[attribute] = value;
    this.setState(({profile}) => ({profile: fp.set(attribute, value, profile)}));
  }

  render() {
    const {
      profile: {
        givenName, familyName, currentPosition, organization,
        contactEmail, username, areaOfResearch
      }
    } = this.state;
    return <div id='account-creation'
                style={{'paddingTop': '3rem', 'paddingRight': '3rem', 'paddingLeft': '3rem'}}>
      <BoldHeader>Create your account</BoldHeader>
      <div>
        <FormSection>
          <TextInput id='givenName' name='givenName' autoFocus
                     placeholder='First Name'
                     value={givenName}
                     invalid={givenName.length > 80}
                       style={{width: '16rem'}}
                     onChange={v => this.updateProfile('givenName', v)}/>
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
                     onChange={v => this.updateProfile('familyName', v)}/>
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
                       onChange={v => this.updateProfile('contactEmail', v)}/>
        </FormSection>
        <FormSection>
          <TextInput id='currentPosition' name='currentPosition'
                     placeholder='Your Current Position'
                     value={currentPosition}
                     invalid={currentPosition.length > 255}
                       style={{width: '16rem'}}
                     onChange={v => this.updateProfile('currentPosition', v)}/>
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
                     onChange={v => this.updateProfile('organization', v)}/>
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
                        onChange={v => this.updateProfile('areaOfResearch', v)}/>
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
            <ValidationIcon validSuccess={this.usernameValid()}/>
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
          this.isUsernameValidationError()}
                  style={{'height': '2rem', 'width': '10rem'}}
                  onClick={() => this.createAccount()}>
            Next
          </Button>
        </FormSection>
      </div>
      {this.state.showAllFieldsRequiredError &&
      <Error>
          All fields are required.
      </Error>}
    </div>;
  }

}
