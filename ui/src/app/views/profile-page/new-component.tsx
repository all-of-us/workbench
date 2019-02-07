import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {validate} from 'validate.js';

import {Button} from 'app/components/buttons';
import {TextArea, TextInput, ValidationError} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {Profile, ProfileApi} from 'generated/fetch';


declare const gapi: any;

const styles = reactStyles({
  h1: {
    color: colors.purple[0],
    fontSize: 20,
    fontWeight: 500,
    lineHeight: '24px'
  },
  inputLabel: {
    color: colors.purple[0],
    fontSize: 14, lineHeight: '18px',
    marginBottom: 6
  },
  inputStyle: {
    width: 250,
    marginRight: 20
  },
  longInputStyle: {
    height: 175, width: 420
  },
  box: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 21
  }
});

const labelSubstitutions = {
  givenName: 'firstName',
  familyName: 'lastName',
  currentPosition: 'currentPosition',
  organization: 'organization',
  areaOfResearch: 'currentResearch'
};
const required = {presence: {allowEmpty: false}};
const notTooLong = maxLength => ({
  length: {
    maximum: maxLength,
    tooLong: 'must be %{count} characters or less'
  }
});
const validators = {
  firstName: {...required, ...notTooLong(80)},
  lastName: {...required, ...notTooLong(80)},
  currentPosition: {...required, ...notTooLong(255)},
  organization: {...required, ...notTooLong(255)},
  currentResearch: {...required},
};

export const ProfilePageReact = withUserProfile()
(class ProfilePageReactComponent extends React.Component<
  { profile: Profile },
  { profileEdits: Profile, updating: boolean }
> {
  constructor(props) {
    super(props);

    this.state = {
      profileEdits: props.profile || {},
      updating: false
    };
  }

  componentDidUpdate(prevProps) {
    const {profile} = this.props;
    if (!prevProps.profile && profile) {
      this.setState({profileEdits: profile});
    }
  }

  saveProfile() {
    this.setState({updating: true})
    profileApi().updateProfile(this.state.profileEdits);
  }

  render() {
    const {profile} = this.props;
    const {profileEdits, updating} = this.state;
    const {givenName, familyName, currentPosition, organization, areaOfResearch} = profileEdits;
    const errors = validate({
      firstName: givenName,
      lastName: familyName,
      currentPosition,
      organization,
      currentResearch: areaOfResearch
    }, validators);

    const profileImageUrl =
      gapi.auth2.getAuthInstance().currentUser.get().getBasicProfile().getImageUrl();

    const makeProfileInput = ({title, valueKey, isLong = false, ...props}) => {
      const errorText = profile && errors && errors[labelSubstitutions[valueKey]];

      const inputProps = {
        value: profileEdits[valueKey] || '',
        onChange: v => this.setState(fp.set(['profileEdits', valueKey], v)),
        invalid: !!errorText,
        ...props
      };

      return <div style={{marginBottom: isLong ? 40 : 20}}>
        <div style={styles.inputLabel}>{title}</div>
        {isLong ?
          <TextArea
            style={styles.longInputStyle}
            {...inputProps}
          /> :
          <TextInput
            style={styles.inputStyle}
            {...inputProps}
          />}
        <ValidationError>{errorText}</ValidationError>
      </div>;
    };

    console.log(JSON.stringify(profile))

    return <div style={{margin: '35px 35px 100px 45px'}}>
      {(!profile || updating) && <SpinnerOverlay/>}
        <div style={{...styles.h1, marginBottom: 30}}>Profile</div>
        <div style={{display: 'flex'}}>
          <div style={{flex: 'none'}}>
            <img src={profileImageUrl} alt='Profile image' style={{
              borderRadius: '100%',
              // width: '50%'
            }}/>
          </div>
          <div style={{flex: '1 0 520px', padding: '0 26px'}}>
            <div style={{display: 'flex'}}>
              {makeProfileInput({
                title: 'First Name',
                valueKey: 'givenName'
              })}
              {makeProfileInput({
                title: 'Last Name',
                valueKey: 'familyName'
              })}
            </div>
            {makeProfileInput({
              title: 'Contact Email',
              valueKey: 'contactEmail',
              disabled: true
            })}
            <div style={styles.inputLabel}>Username</div>
            <div style={{
              paddingLeft: '0.5rem', marginBottom: 20,
              height: '1.5rem',
              color: '#000'
            }}
            >
              {profile && profile.username}
            </div>
            {makeProfileInput({
              title: 'Your Current Position',
              valueKey: 'currentPosition'
            })}
            {makeProfileInput({
              title: 'Your Organization',
              valueKey: 'organization'
            })}
            {makeProfileInput({
              title: 'Current Research Work',
              valueKey: 'areaOfResearch',
              isLong: true
            })}
            {makeProfileInput({
              title: 'About You',
              valueKey: 'aboutYou',
              isLong: true
            })}
            <div style={{marginTop: 100}}>
              <Button
                type='text'
                onClick={() => this.setState({profileEdits: profile})}
              >
                Discard Changes
              </Button>
              <Button
                type='bluePrimary'
                style={{marginLeft: 40}}
                onClick={() => this.saveProfile()}
              >
                Save Profile
              </Button>
            </div>
          </div>
          <div style={{flex: '0 0 420px'}}>
            <div style={styles.box}>
              <div style={styles.h1}>All of Us Training</div>
            </div>
          </div>
        </div>
      </div>;
  }
});

@Component({
  selector: 'app-react-profile',
  template: '<div #root></div>'
})
export class ReactProfileComponent extends ReactWrapperBase {
  constructor() {
    super(ProfilePageReact, []);
  }
}
