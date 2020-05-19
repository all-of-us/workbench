import * as fp from 'lodash/fp';
import * as React from 'react';

import {Component} from '@angular/core';

import {FlexColumn, FlexRow} from 'app/components/flex';
import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {SmallHeader} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {TextInput, Toggle} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {displayDateWithoutHours, reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {institutionApi} from 'app/services/swagger-fetch-clients';

import {Profile} from 'generated/fetch';
import {Dropdown} from 'primereact/dropdown';

const styles = reactStyles({
  semiBold: {
    fontWeight: 600
  }
});

const freeCreditLimitOptions = [
  {label: '$300', value: 300},
  {label: '$350', value: 350},
  {label: '$400', value: 400},
  {label: '$450', value: 450},
  {label: '$500', value: 500},
  {label: '$550', value: 550},
  {label: '$600', value: 600},
  {label: '$650', value: 650},
  {label: '$700', value: 700},
  {label: '$750', value: 750},
  {label: '$800', value: 800}
];

const ReadonlyInputWithLabel = ({label, content, inputStyle = {}}) => {
  return <FlexColumn style={{marginTop: '1rem'}}>
    <label style={styles.semiBold}>{label}</label>
    <TextInput
        value={content}
        disabled
        style={{
          backgroundColor: colorWithWhiteness(colors.primary, .95),
          opacity: '100%',
          width: '17.5rem',
          ...inputStyle
        }}
    />
  </FlexColumn>;
};

const DropdownWithLabel = ({label, options, initialValue, onChange, disabled, dropdownStyle = {}}) => {
  return <FlexColumn style={{marginTop: '1rem'}}>
    <label style={styles.semiBold}>{label}</label>
    <Dropdown
        style={{
          minWidth: '70px',
          width: '14rem',
          ...dropdownStyle
        }}
        options={options}
        onChange={(e) => onChange(e)}
        value={initialValue}
        disabled={disabled}
    />
  </FlexColumn>;
};

const ToggleWithLabelAndToggledText = ({label, initialValue, disabled, onChange}) => {
  return <FlexColumn style={{width: '8rem', flex: '0 0 auto'}}>
    <label>{label}</label>
    <Toggle
        name={initialValue ? 'BYPASSED' : ''}
        initialValue={initialValue}
        disabled={disabled}
        onToggle={(checked) => onChange(checked)}
        height={18}
        width={33}
    />
  </FlexColumn>;
};

interface Props {
  // From withUserProfile
  profileState: {
    profile: Profile,
    reload: Function,
    updateCache: Function
  };
}

interface State {
  loading: boolean;
  profile: Profile;
  verifiedInstitutionOptions: Array<{label: string, value: {displayName: string, shortName: string}}>;
}


const AdminUser = withUserProfile()(class extends React.Component<Props, State> {
  constructor(props) {
    super(props);

    this.state = {
      loading: false,
      profile: this.props.profileState.profile,
      verifiedInstitutionOptions: []
    };
  }

  async componentDidMount() {
    const institutionsResponse = await institutionApi().getInstitutions();
    const options = fp.map(
      institution => {
        return {
          'label': institution.displayName ? institution.displayName : institution.shortName,
          'value': {displayName: institution.displayName, shortName: institution.shortName}
        };
      },
      institutionsResponse.institutions
    );
    this.setState({verifiedInstitutionOptions: options});
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
                width: '17.5rem'
              }}
          >
            <label style={{fontWeight: 600}}>
              Account access
            </label>
            <Toggle
                name={profile.disabled ? 'Disabled' : 'Enabled'}
                initialValue={!profile.disabled}
                disabled={true}
                data-test-id='account-access-toggle'
                onToggle={(value) => value}
                style={{marginLeft: 'auto', paddingBottom: '0px'}}
                height={18}
                width={33}
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
          <FlexColumn style={{width: '33%', marginRight: '1rem'}}>
            <ReadonlyInputWithLabel
                label={'User name'}
                content={profile.givenName + ' ' + profile.familyName}
            />
            <ReadonlyInputWithLabel
                label={'Registration state'}
                content={fp.capitalize(profile.dataAccessLevel.toString())}
            />
            <ReadonlyInputWithLabel
                label={'Registration date'}
                content={profile.firstRegistrationCompletionTime ? displayDateWithoutHours(profile.firstRegistrationCompletionTime) : ''}
            />
            <ReadonlyInputWithLabel
                label={'Username'}
                content={profile.username}
            />
            <ReadonlyInputWithLabel
                label={'Contact email'}
                content={profile.contactEmail}
            />
            <ReadonlyInputWithLabel
                label={'Free credits used'}
                content={profile.freeTierUsage}
                inputStyle={{width: '6.5rem'}}
            />
            <ReadonlyInputWithLabel
                label={'Beta access time requested'}
                content={profile.betaAccessRequestTime ? displayDateWithoutHours(profile.betaAccessRequestTime) : ''}
            />
          </FlexColumn>
          <FlexColumn style={{width: '33%'}}>
            <DropdownWithLabel
                label={'Free credit limit'}
                options={freeCreditLimitOptions}
                onChange={(value) => value}
                initialValue={profile.freeTierDollarQuota}
                dropdownStyle={{width: '3rem'}}
                disabled={true}
            />
            <DropdownWithLabel
                label={'Verified institution'}
                options={this.state.verifiedInstitutionOptions}
                onChange={(value) => value}
                initialValue={
                  profile.verifiedInstitutionalAffiliation
                      ? profile.verifiedInstitutionalAffiliation.institutionShortName
                      : undefined
                }
                disabled={true}
            />
            <div style={{marginTop: '1rem', width: '15rem'}}>
              <label style={{fontWeight: 600}}>Bypass access to:</label>
              <FlexRow style={{marginTop: '.5rem'}}>
                <ToggleWithLabelAndToggledText
                    label={'2-factor auth'}
                    initialValue={!!profile.twoFactorAuthBypassTime}
                    disabled={true}
                    onChange={(checked) => checked}
                />
                <ToggleWithLabelAndToggledText
                    label={'Compliance training'}
                    initialValue={!!profile.complianceTrainingBypassTime}
                    disabled={true}
                    onChange={(checked) => checked}
                />
              </FlexRow>
              <FlexRow style={{marginTop: '1rem'}}>
                <ToggleWithLabelAndToggledText
                    label={'eRA Commons'}
                    initialValue={!!profile.eraCommonsBypassTime}
                    disabled={true}
                    onChange={(checked) => checked}
                />
                <ToggleWithLabelAndToggledText
                    label={'Data User Code of Conduct'}
                    initialValue={!!profile.dataUseAgreementBypassTime}
                    disabled={true}
                    onChange={(checked) => checked}
                />
              </FlexRow>
            </div>
          </FlexColumn>
        </FlexRow>
      </FlexColumn>
      {this.state.loading && <SpinnerOverlay/>}
    </FadeBox>;
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
