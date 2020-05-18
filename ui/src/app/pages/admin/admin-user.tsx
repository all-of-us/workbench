import * as React from 'react';
import * as fp from 'lodash/fp'

import {Component} from '@angular/core';

import {FlexColumn, FlexRow} from 'app/components/flex';

import {SmallHeader} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {displayDateWithoutHours, renderUSD, withUserProfile} from 'app/utils';
import {ReactWrapperBase} from 'app/utils';
import {FadeBox} from "app/components/containers";
import {Button} from "app/components/buttons";
import {TextInput, Toggle} from "app/components/inputs";
import {authDomainApi, profileApi} from "app/services/swagger-fetch-clients";
import {SpinnerOverlay} from "app/components/spinners";

import {Profile} from 'generated/fetch';

const DisabledInputWithLabel = ({label, content, size = 'large'}) => {
  return <div style={{marginTop: '1rem', width: size === 'small' ? '150px' : '420px'}}>
    <label style={{fontWeight: 600}}>{label}</label>
    <TextInput
        value={content}
        disabled
        style={{
          backgroundColor: colorWithWhiteness(colors.primary, .95),
          opacity: '100%'
        }}
    />
  </div>
};

interface Props {
  // From withUserProfile
  profileState: {
    profile: Profile,
    reload: Function,
    updateCache: Function
  }
}

interface State {
  loading: boolean;
  profile: Profile;
}


const AdminUser = withUserProfile()(class extends React.Component<Props, State> {
  constructor(props) {
    super(props);

    this.state = {
      loading: false,
      profile: this.props.profileState.profile
    }
  }

  toggleUserDisabledStatus() {
    // This function left as an exercise to the next developer.
    return null;
  }

  render() {
    const {profile} = this.state;
    return <FadeBox
        style={{
          margin: 'auto',
          paddingTop: '1rem',
          width: '96.25%',
          minWidth: '1232px',
          color: colors.primary
        }}
    >
      <FlexColumn>
        <FlexRow style={{alignItems: 'center'}}>
          <ClrIcon shape='arrow' size={37} style={{
            backgroundColor: colorWithWhiteness(colors.accent, .85),
            color: colors.accent,
            borderRadius: '18px',
            transform: 'rotate(270deg)'
          }}/>
          <SmallHeader style={{marginTop: 0, marginLeft: '0.5rem'}}>
            User Profile Information
          </SmallHeader>
        </FlexRow>
        <FlexRow style={{width: '100%', marginTop: '1rem', alignItems: 'center'}}>
          <FlexRow
              style={{
                alignItems: 'center',
                backgroundColor: colorWithWhiteness(colors.primary, .85),
                borderRadius: '5px',
                padding: '0 .5rem',
                height: '1.625rem',
                width: '33%'
              }}
          >
            <label style={{fontWeight: 600}}>
              Account access
            </label>
            <Toggle
                name={profile.disabled ? 'Disabled' : 'Enabled'}
                enabled={!profile.disabled}
                data-test-id='account-access-toggle'
                onToggle={() => this.toggleUserDisabledStatus()}
                style={{marginLeft: 'auto', paddingBottom: '0px'}}
            />
          </FlexRow>
          <Button type='link' style={{marginLeft: 'auto'}}>
            Cancel
          </Button>
          <Button type='primary'>
            Save
          </Button>
        </FlexRow>
        <FlexRow>
          <FlexColumn style={{width: '33%'}}>
            <DisabledInputWithLabel label={'User name'} content={profile.givenName + ' ' + profile.familyName}/>
            <DisabledInputWithLabel label={'Registration state'} content={fp.capitalize(profile.dataAccessLevel.toString())}/>
            <DisabledInputWithLabel label={'Registration date'} content={displayDateWithoutHours(profile.firstSignInTime)}/>
            <DisabledInputWithLabel label={'Username'} content={profile.username}/>
            <DisabledInputWithLabel label={'Contact email'} content={profile.contactEmail}/>
            <DisabledInputWithLabel label={'Free credits used'} content={profile.freeTierDollarQuota - profile.freeTierUsage} size={'small'}/>
            <DisabledInputWithLabel label={'Beta access time requested'} content={displayDateWithoutHours(profile.betaAccessRequestTime)}/>
          </FlexColumn>
          <FlexColumn>

          </FlexColumn>
        </FlexRow>
      </FlexColumn>
      {this.state.loading && <SpinnerOverlay/>}
    </FadeBox>
  }
});

@Component({
  template: '<div #root></div>'
})
export class AdminUserComponent extends ReactWrapperBase {
  constructor() {
    super(AdminUser, []);
  }
}
