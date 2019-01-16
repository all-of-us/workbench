import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {fullUrl, handleErrors} from 'app/utils/fetch';

import {
  CreateAccountRequest,
  DataAccessLevel,
  FetchArgs,
  Profile,
  ProfileApiFetchParamCreator,
} from 'generated/fetch/api';

import {
  Error,
  ErrorMessage,
  LongInput,
  styles as inputStyles
} from 'app/components/inputs';

import {
  InfoIcon,
  ValidationIcon
} from 'app/components/icons';

import {
  TooltipTrigger
} from 'app/components/popups';

import {Button} from 'app/components/buttons';
import { FormSection } from 'app/components/forms';
import {BoldHeader} from 'app/components/headers';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

export interface AccountCreationProps {
  invitationKey: string;
  setProfile: Function;
  onAccountCreation: Function;
}

export interface AccountCreationState {
  profile: Profile;
  usernameCheckInProgress: boolean;
  usernameConflictError: boolean;
  creatingAccount: boolean;
  showAllFieldsRequiredError: boolean;
}

export class AccountCreationReact extends
    React.Component<AccountCreationProps, AccountCreationState> {
  usernameCheckTimeout: NodeJS.Timer;
  accountCreated = false;

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
    this.createAccount = this.createAccount.bind(this);
    this.usernameValid = this.usernameValid.bind(this);
    this.usernameNotValid = this.usernameNotValid.bind(this);
  }


  createAccount(): void {
    const {invitationKey, setProfile, onAccountCreation} = this.props;
    if (this.state.usernameConflictError || this.usernameInvalidError()) {
      return;
    }
    this.setState({showAllFieldsRequiredError: false});
    const requiredFields =
      [this.state.profile.givenName, this.state.profile.familyName,
        this.state.profile.username, this.state.profile.contactEmail,
        this.state.profile.currentPosition, this.state.profile.organization,
        this.state.profile.areaOfResearch];
    if (requiredFields.some(isBlank)) {
      this.setState({showAllFieldsRequiredError: true});
      return;
    } else if (this.isUsernameValidationError()) {
      return;
    }
    const request: CreateAccountRequest = {
      profile: this.state.profile,
      invitationKey: invitationKey
    };
    this.setState({creatingAccount: true});
    const args: FetchArgs = ProfileApiFetchParamCreator().createAccount(request);
    fetch(fullUrl(args.url), args.options)
      .then(handleErrors)
      .then((response) => response.json)
      .then((profile) => {
          this.setState({profile: profile, creatingAccount: false});
          this.accountCreated = true;
          setProfile(profile);
          onAccountCreation();
        }
      )
      .catch(error => {
        console.log(error);
        this.setState({creatingAccount: false});
      });
  }

  usernameNotValid(): boolean {
    if (isBlank(this.state.profile.username) || this.state.usernameCheckInProgress) {
      return false;
    }
    return this.isUsernameValidationError();
  }

  usernameValid(): boolean {
    if (isBlank(this.state.profile.username) || this.state.usernameCheckInProgress) {
      return false;
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
    if (username.trim().length > 64) {
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
    if (this.state.profile.username === '') {
      return;
    }
    this.setState({usernameConflictError: false});
    // TODO: This should use a debounce, rather than manual setTimeout()s.
    clearTimeout(this.usernameCheckTimeout);
    this.setState({usernameCheckInProgress: true});
    this.usernameCheckTimeout = setTimeout(() => {
      if (!this.state.profile.username.trim()) {
        this.setState({usernameCheckInProgress: false});
        return;
      }
      const args: FetchArgs = ProfileApiFetchParamCreator()
        .isUsernameTaken(this.state.profile.username);
      fetch(fullUrl(args.url), args.options)
        .then(handleErrors)
        .then((response) => response.json())
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
    this.setState({profile: newProfile});
  }

  render() {
    return <React.Fragment>
      {!this.accountCreated &&
        <div id='account-creation'
             style={{'paddingTop': '3rem', 'paddingRight': '3rem', 'paddingLeft': '3rem'}}>
          <BoldHeader>Create your account</BoldHeader>
        <div>
          <FormSection>
            <LongInput type='text' id='givenName' name='givenName' autoFocus
                    placeholder='First Name'
                    value={this.state.profile.givenName}
                    style={(this.state.profile.givenName.length > 80) ?
                      inputStyles.unsuccessfulInput : inputStyles.successfulInput}
                    onChange={e => this.updateProfile('givenName', e.target.value)}/>
              {this.state.profile.givenName.length > 80 &&
                <ErrorMessage id='givenNameError'>
                  First Name must be 80 characters or less.
                </ErrorMessage>}
          </FormSection>
          <FormSection>
            <LongInput type='text' id='familyName' name='familyName' placeholder='Last Name'
                   value={this.state.profile.familyName}
                   style={(this.state.profile.familyName.length > 80) ?
                     inputStyles.unsuccessfulInput : inputStyles.successfulInput}
                   onChange={e => this.updateProfile('familyName', e.target.value)}/>
              {this.state.profile.familyName.length > 80 &&
                <ErrorMessage id='familyNameError'>
                  Last Name must be 80 character or less.
                </ErrorMessage>}
          </FormSection>
          <FormSection>
            <LongInput type='text' id='contactEmail' name='contactEmail'
                      placeholder='Email Address'
                      onChange={e => this.updateProfile('contactEmail', e.target.value)}/>
          </FormSection>
          <FormSection>
            <LongInput type='text' id='currentPosition' name='currentPosition'
                     placeholder='You Current Position'
                     value={this.state.profile.currentPosition}
                     style={(this.state.profile.currentPosition.length > 255) ?
                       inputStyles.unsuccessfulInput : inputStyles.successfulInput}
                     onChange={e => this.updateProfile('currentPosition', e.target.value)}/>
              {this.state.profile.currentPosition.length > 255 &&
                <ErrorMessage id='currentPositionError'>
                  Current Position must be 255 characters or less.
                </ErrorMessage>}
          </FormSection>
          <FormSection>
            <LongInput type='text' id='organization' name='organization'
                     placeholder='Your Organization'
                     value={this.state.profile.organization}
                     style={(this.state.profile.organization.length > 255) ?
                       inputStyles.unsuccessfulInput : inputStyles.successfulInput}
                     onChange={e => this.updateProfile('organization', e.target.value)}/>
              {this.state.profile.organization.length > 255 &&
                <ErrorMessage id='organizationError'>
                  Organization must be 255 characters of less.
                </ErrorMessage>}
          </FormSection>
          <FormSection style={{'display': 'flex', 'flexDirection': 'row'}}>
              <textarea style={{
                        ...inputStyles.formInput,
                        ...inputStyles.longInput,
                        'height': '10em',
                        'resize': 'none',
                        'width': '16rem'
                      }}
                        id='areaOfResearch'
                        name='areaOfResearch'
                        placeholder='Describe Your Current Research'
                        onChange={e => this.updateProfile('areaOfResearch', e.target.value)}/>
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
            <LongInput type='text' id='username' name='username' placeholder='New Username'
                       onChange={e => this.usernameChanged(e.target.value)}
                       style={(this.state.usernameConflictError || this.usernameInvalidError()) ?
                         inputStyles.unsuccessfulInput : inputStyles.successfulInput}/>
            <div style={inputStyles.iconArea}>
              <ValidationIcon validSuccess={this.usernameValid} notValid={this.usernameNotValid}/>
            </div>
            <TooltipTrigger content={<div>Usernames can contain only letters (a-z),
              numbers (0-9), dashes (-), underscores (_), apostrophes ('), and periods (.)
              (maximum of 64 characters).<br/>Usernames cannot begin or end with a period (.)
              and may not contain more than one period (.) in a row.</div>}>
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
                    onClick={this.createAccount}>
              Next
            </Button>
          </FormSection>
        </div>
        {this.state.showAllFieldsRequiredError &&
          <Error>
            All fields are required.
          </Error>}
        </div>}
    </React.Fragment>;
  }

}
