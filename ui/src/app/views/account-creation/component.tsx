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
  errorMap: Map<string, boolean> = new Map<string, boolean>();
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
    this.errorMap.set('givenName', false);
    this.errorMap.set('familyName', false);
    this.errorMap.set('position', false);
    this.errorMap.set('organization', false);
    this.errorMap.set('username', false);
    this.updateField = this.updateField.bind(this);
    this.createAccount = this.createAccount.bind(this);
    this.usernameChanged = this.usernameChanged.bind(this);
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

  usernameChanged(event: React.FormEvent<HTMLInputElement>): void {
    this.updateProfile(event.currentTarget.id, event.currentTarget.value);
    if (this.state.profile.username === '') {
      return;
    }
    this.errorMap['username'] = false;
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
          this.errorMap['username'] = (body.isTaken || this.usernameInvalidError());
          this.setState({usernameCheckInProgress: false, usernameConflictError: body.isTaken});
        })
        .catch((error) => {
          console.log(error);
          this.setState({usernameCheckInProgress: false});
        });
    }, 300);
  }

  updateField(
    event: React.FormEvent<HTMLInputElement> | React.ChangeEvent<HTMLTextAreaElement>): void {
    const value  = event.currentTarget.value;
    const attribute = event.currentTarget.id;
    this.updateProfile(attribute, value);
    if (['givenName', 'familyName'].includes(attribute)) {
      this.errorMap[attribute] = value.length > 80;
    } else if (['currentPosition', 'organization'].includes(attribute)) {
      this.errorMap[attribute] = value.length > 255;
    }
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
                    style={this.errorMap['givenName'] ?
                      inputStyles.unsuccessfulInput : inputStyles.successfulInput}
                    onChange={this.updateField}/>
              {this.errorMap['givenName'] &&
                <ErrorMessage>
                  First Name must be 80 characters or less.
                </ErrorMessage>}
          </FormSection>
          <FormSection>
            <LongInput type='text' id='familyName' name='familyName' placeholder='Last Name'
                   value={this.state.profile.familyName}
                   style={this.errorMap['familyName'] ?
                     inputStyles.unsuccessfulInput : inputStyles.successfulInput}
                   onChange={this.updateField}/>
              {this.errorMap['familyName'] &&
                <ErrorMessage>
                  Last Name must be 80 character or less.
                </ErrorMessage>}
          </FormSection>
          <FormSection>
            <LongInput type='text' id='contactEmail' name='contactEmail'
                      placeholder='Email Address'
                      onChange={this.updateField}/>
          </FormSection>
          <FormSection>
            <LongInput type='text' id='currentPosition' name='currentPosition'
                     placeholder='You Current Position'
                     value={this.state.profile.currentPosition}
                     style={this.errorMap['currentPosition'] ?
                       inputStyles.unsuccessfulInput : inputStyles.successfulInput}
                     onChange={this.updateField}/>
              {this.errorMap['currentPosition'] &&
                <ErrorMessage>
                  Current Position must be 255 characters or less.
                </ErrorMessage>}
          </FormSection>
          <FormSection>
            <LongInput type='text' id='organization' name='organization'
                     placeholder='Your Organziation'
                     value={this.state.profile.organization}
                     style={this.errorMap['organization'] ?
                       inputStyles.unsuccessfulInput : inputStyles.successfulInput}
                     onChange={this.updateField}/>
              {this.errorMap['currentPosition'] &&
                <ErrorMessage>
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
                        onChange={this.updateField}/>
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
                       onChange={this.usernameChanged.bind(this)}
                       style={this.errorMap['username'] ?
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
                <Error>
                  Username is already taken.
                </Error>}
              {this.usernameInvalidError() &&
                <Error>
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
