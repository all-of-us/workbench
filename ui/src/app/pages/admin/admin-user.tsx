import * as fp from 'lodash/fp';
import * as React from 'react';

import {Component} from '@angular/core';

import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {SmallHeader} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {TextInput, Toggle} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {institutionApi, profileApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  displayDateWithoutHours,
  reactStyles,
  ReactWrapperBase,
  withUrlParams
} from 'app/utils';

import {BulletAlignedUnorderedList} from 'app/components/lists';
import {TooltipTrigger} from 'app/components/popups';
import {getRoleOptions} from 'app/pages/login/account-creation/common';
import {navigate, serverConfigStore} from 'app/utils/navigation';
import {InstitutionalRole, Profile, PublicInstitutionDetails} from 'generated/fetch';
import {Dropdown} from 'primereact/dropdown';
import * as validate from 'validate.js';

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

const TextInputWithLabel = ({label, content, dataTestId, onChange = (value) => {}, disabled= false, inputStyle = {}}) => {
  return <FlexColumn data-test-id={dataTestId} style={{marginTop: '1rem'}}>
    <label style={styles.semiBold}>{label}</label>
    <TextInput
        value={content || ''} // react yells at me if this is null
        disabled={disabled}
        onChange={(value) => onChange(value)}
        style={{
          backgroundColor: disabled ? colorWithWhiteness(colors.primary, .95) : colors.white,
          opacity: '100%',
          width: '17.5rem',
          ...inputStyle
        }}
    />
  </FlexColumn>;
};

const DropdownWithLabel = ({label, options, initialValue, onChange, disabled= false, dataTestId, dropdownStyle = {}}) => {
  return <FlexColumn data-test-id={dataTestId} style={{marginTop: '1rem'}}>
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

const ToggleWithLabelAndToggledText = ({label, initialValue, disabled, onToggle, dataTestId}) => {
  return <FlexColumn data-test-id={dataTestId} style={{width: '8rem', flex: '0 0 auto'}}>
    <label>{label}</label>
    <Toggle
        name={initialValue ? 'BYPASSED' : ''}
        checked={initialValue}
        disabled={disabled}
        onToggle={(checked) => onToggle(checked)}
        height={18}
        width={33}
    />
  </FlexColumn>;
};

interface Props {
  // From withUrlParams
  urlParams: {
    usernameWithoutGsuiteDomain: string
  };
}

interface State {
  institutionsLoadingError: string;
  loading: boolean;
  oldProfile: Profile;
  profileLoadingError: string;
  saveDisabled: boolean;
  updatedProfile: Profile;
  verifiedInstitutionOptions: Array<PublicInstitutionDetails>;
  verifiedInstitutionsByShortname: Map<String, PublicInstitutionDetails>;
}


const AdminUser = withUrlParams()(class extends React.Component<Props, State> {
  constructor(props) {
    super(props);

    this.state = {
      institutionsLoadingError: '',
      loading: true,
      oldProfile: null,
      profileLoadingError: '',
      saveDisabled: true,
      updatedProfile: null,
      verifiedInstitutionOptions: [],
      verifiedInstitutionsByShortname: new Map()
    };
  }

  async componentDidMount() {
    try {
      Promise.all([
        this.getUser(),
        this.getInstitutions()
      ]);
    } finally {
      this.setState({loading: false});
    }
  }

  async getUser() {
    const {gsuiteDomain} = serverConfigStore.getValue();
    try {
      const profile = await profileApi().getUserByUsername(this.props.urlParams.usernameWithoutGsuiteDomain + '@' + gsuiteDomain);
      this.setState({oldProfile: profile, updatedProfile: profile});
    } catch (error) {
      this.setState({profileLoadingError: 'Could not find user - please check spelling of username and try again'});
    }
  }

  async getInstitutions() {
    try {
      const institutionsResponse = await institutionApi().getPublicInstitutionDetails();
      const institutions = institutionsResponse.institutions;
      this.setState({
        verifiedInstitutionOptions: fp.sortBy( institution => institution.displayName, institutions),
        verifiedInstitutionsByShortname: institutions.reduce(
          (accumulator, institution) => accumulator.set(institution.shortName, institution),
          new Map<String, PublicInstitutionDetails>()
        )
      });
    } catch (error) {
      this.setState({institutionsLoadingError: 'Could not get list of verified institutions - please try again later'});
    }
  }

  getRoleOptionsForProfile() {
    const {updatedProfile: {verifiedInstitutionalAffiliation}, verifiedInstitutionOptions} = this.state;
    const institutionShortName = verifiedInstitutionalAffiliation ? verifiedInstitutionalAffiliation.institutionShortName : '';
    return getRoleOptions(verifiedInstitutionOptions, institutionShortName);
  }

  getInstitutionDropdownOptions() {
    const {verifiedInstitutionOptions} = this.state;
    return fp.map(
      institution => {
        return {label: institution.displayName, value: institution.shortName};
      },
      verifiedInstitutionOptions
    );
  }

  isSaveDisabled(errors) {
    const {oldProfile, updatedProfile} = this.state;
    return fp.isEqual(oldProfile, updatedProfile) || errors;
  }

  setVerifiedInstitutionOnProfile(institutionShortName: string) {
    const {verifiedInstitutionsByShortname} = this.state;
    this.setState(
      fp.set(
          ['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionShortName'],
        institutionShortName
      )
    );
    this.setState(
      fp.set(
          ['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionDisplayName'],
        verifiedInstitutionsByShortname.get(institutionShortName).displayName
        )
    );
    this.setState(fp.set(['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionRoleEnum'], undefined));
    this.setState(fp.set(['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionalRoleOtherText'], undefined));
  }

  setInstitutionalRoleOnProfile(institutionalRoleEnum: InstitutionalRole) {
    this.setState(fp.set(['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionalRoleEnum'], institutionalRoleEnum));
    this.setState(fp.set(['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionalRoleOtherText'], undefined));
  }

  updateVerifiedInstitutionalAffiliation() {
    const {updatedProfile} = this.state;
    this.setState({loading: true});
    profileApi().updateVerifiedInstitutionalAffiliation(updatedProfile.userId, updatedProfile.verifiedInstitutionalAffiliation).then(() => {
      this.setState({loading: false});
    });
  }

  validateVerifiedInstitutionalAffiliation() {
    const {updatedProfile} = this.state;
    if (updatedProfile) {
      if (updatedProfile.verifiedInstitutionalAffiliation) {
        return updatedProfile.verifiedInstitutionalAffiliation;
      }
      return false;
    }
    return false;
  }

  validateInstitutionShortname() {
    const {updatedProfile} = this.state;
    if (updatedProfile && updatedProfile.verifiedInstitutionalAffiliation) {
      return updatedProfile.verifiedInstitutionalAffiliation.institutionShortName;
    }
    return false;
  }

  validateInstitutionalRoleEnum() {
    const {updatedProfile} = this.state;
    if (updatedProfile && updatedProfile.verifiedInstitutionalAffiliation) {
      return updatedProfile.verifiedInstitutionalAffiliation.institutionalRoleEnum;
    }
    return false;
  }

  validateInstitutionalRoleOtherText() {
    const {updatedProfile} = this.state;
    if (updatedProfile && updatedProfile.verifiedInstitutionalAffiliation) {
      return updatedProfile.verifiedInstitutionalAffiliation.institutionalRoleEnum !== InstitutionalRole.OTHER
            || !!updatedProfile.verifiedInstitutionalAffiliation.institutionalRoleOtherText;
    }
    return false;
  }

  render() {
    const {
      institutionsLoadingError,
      profileLoadingError,
      updatedProfile,
      verifiedInstitutionOptions
    } = this.state;
    const errors = validate({
      'verifiedInstitutionalAffiliation': this.validateVerifiedInstitutionalAffiliation(),
      'institutionShortName': this.validateInstitutionShortname(),
      'institutionalRoleEnum': this.validateInstitutionalRoleEnum(),
      'institutionalRoleOtherText': this.validateInstitutionalRoleOtherText()
    }, {
      verifiedInstitutionalAffiliation: {truthiness: true},
      institutionShortName: {truthiness: true},
      institutionalRoleEnum: {truthiness: true},
      institutionalRoleOtherText: {truthiness: true}
    });
    return <FadeBox
        style={{
          margin: 'auto',
          paddingTop: '1rem',
          width: '96.25%',
          minWidth: '1232px',
          color: colors.primary
        }}
    >
      {institutionsLoadingError && <div>{institutionsLoadingError}</div>}
      {profileLoadingError && <div>{profileLoadingError}</div>}
      {updatedProfile && <FlexColumn>
        <FlexRow style={{alignItems: 'center'}}>
          <a onClick={() => navigate(['admin', 'users'])}>
            <ClrIcon
              shape='arrow'
              size={37}
              style={{
                backgroundColor: colorWithWhiteness(colors.accent, .85),
                color: colors.accent,
                borderRadius: '18px',
                transform: 'rotate(270deg)'
              }}
            />
          </a>
          <SmallHeader style={{marginTop: 0, marginLeft: '0.5rem'}}>
            User Profile Information
          </SmallHeader>
        </FlexRow>
        <FlexRow style={{width: '100%', marginTop: '1rem', alignItems: 'center', justifyContent: 'space-between'}}>
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
                name={updatedProfile.disabled ? 'Disabled' : 'Enabled'}
                checked={!updatedProfile.disabled}
                disabled={true}
                data-test-id='account-access-toggle'
                onToggle={() => this.setState({saveDisabled: false})}
                style={{marginLeft: 'auto', paddingBottom: '0px'}}
                height={18}
                width={33}
            />
          </FlexRow>
          <TooltipTrigger
              data-test-id='user-admin-errors-tooltip'
              content={
                errors && this.isSaveDisabled(errors) &&
                <BulletAlignedUnorderedList>
                  {errors.verifiedInstitutionalAffiliation && <li>Verified institutional affiliation can't be unset or left blank</li>}
                  {errors.institutionShortName && <li>You must choose an institution</li>}
                  {errors.institutionalRoleEnum && <li>You must select the user's role at the institution</li>}
                  {errors.institutionalRoleOtherText && <li>You must describe the user's role if you select Other</li>}
                </BulletAlignedUnorderedList>
              }
          >
            <Button
                type='primary'
                disabled={this.isSaveDisabled(errors)}
                onClick={() => this.updateVerifiedInstitutionalAffiliation()}
            >
              Save
            </Button>
          </TooltipTrigger>
        </FlexRow>
        <FlexRow>
          <FlexColumn style={{width: '33%', marginRight: '1rem'}}>
            <TextInputWithLabel
                label={'User name'}
                content={updatedProfile.givenName + ' ' + updatedProfile.familyName}
                dataTestId={'userFullName'}
                disabled={true}
            />
            <TextInputWithLabel
                label={'Registration state'}
                content={fp.capitalize(updatedProfile.dataAccessLevel.toString())}
                dataTestId={'registrationState'}
                disabled={true}
            />
            <TextInputWithLabel
                label={'Registration date'}
                content={
                  updatedProfile.firstRegistrationCompletionTime
                      ? displayDateWithoutHours(updatedProfile.firstRegistrationCompletionTime)
                      : ''
                }
                dataTestId={'firstRegistrationCompletionTime'}
                disabled={true}
            />
            <TextInputWithLabel
                label={'Username'}
                content={updatedProfile.username}
                dataTestId={'username'}
                disabled={true}
            />
            <TextInputWithLabel
                label={'Contact email'}
                content={updatedProfile.contactEmail}
                dataTestId={'contactEmail'}
                disabled={true}
            />
            <TextInputWithLabel
                label={'Free credits used'}
                content={updatedProfile.freeTierUsage}
                inputStyle={{width: '6.5rem'}}
                dataTestId={'freeTierUsage'}
                disabled={true}
            />
          </FlexColumn>
          <FlexColumn style={{width: '33%'}}>
            <DropdownWithLabel
                label={'Free credit limit'}
                options={freeCreditLimitOptions}
                onChange={() => this.setState({saveDisabled: false})}
                initialValue={updatedProfile.freeTierDollarQuota}
                dropdownStyle={{width: '3rem'}}
                disabled={true}
                dataTestId={'freeTierDollarQuota'}
            />
            {verifiedInstitutionOptions && <DropdownWithLabel
                label={'Verified institution'}
                options={this.getInstitutionDropdownOptions()}
                onChange={(event) => this.setVerifiedInstitutionOnProfile(event.value)}
                initialValue={
                  updatedProfile.verifiedInstitutionalAffiliation
                      ? updatedProfile.verifiedInstitutionalAffiliation.institutionShortName
                      : undefined
                }
                dataTestId={'verifiedInstitution'}
            />}
            {verifiedInstitutionOptions && updatedProfile.verifiedInstitutionalAffiliation && <DropdownWithLabel
                label={'Institutional role'}
                options={this.getRoleOptionsForProfile() || []}
                onChange={(event) => this.setInstitutionalRoleOnProfile(event.value)}
                initialValue={updatedProfile.verifiedInstitutionalAffiliation.institutionalRoleEnum
                    ? updatedProfile.verifiedInstitutionalAffiliation.institutionalRoleEnum
                    : undefined
                }
                dataTestId={'institutionalRole'}
                disabled={!updatedProfile.verifiedInstitutionalAffiliation.institutionShortName}
              />
            }
            {
              verifiedInstitutionOptions
              && updatedProfile.verifiedInstitutionalAffiliation
              && updatedProfile.verifiedInstitutionalAffiliation.institutionalRoleEnum === InstitutionalRole.OTHER
              && <TextInputWithLabel
                label={'Institutional role description'}
                content={updatedProfile.verifiedInstitutionalAffiliation.institutionalRoleOtherText}
                onChange={(value) => this.setState(fp.set(['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionalRoleOtherText'], value))}
                dataTestId={'institutionalRoleOtherText'}
              />
            }
            <div style={{marginTop: '1rem', width: '15rem'}}>
              <label style={{fontWeight: 600}}>Bypass access to:</label>
              <FlexRow style={{marginTop: '.5rem'}}>
                <ToggleWithLabelAndToggledText
                    label={'2-factor auth'}
                    initialValue={!!updatedProfile.twoFactorAuthBypassTime}
                    disabled={true}
                    onToggle={() => this.setState({saveDisabled: false})}
                    dataTestId={'twoFactorAuthBypassToggle'}
                />
                <ToggleWithLabelAndToggledText
                    label={'Compliance training'}
                    initialValue={!!updatedProfile.complianceTrainingBypassTime}
                    disabled={true}
                    onToggle={() => this.setState({saveDisabled: false})}
                    dataTestId={'complianceTrainingBypassToggle'}
                />
              </FlexRow>
              <FlexRow style={{marginTop: '1rem'}}>
                <ToggleWithLabelAndToggledText
                    label={'eRA Commons'}
                    initialValue={!!updatedProfile.eraCommonsBypassTime}
                    disabled={true}
                    onToggle={(checked) => checked}
                    dataTestId={'eraCommonsBypassToggle'}
                />
                <ToggleWithLabelAndToggledText
                    label={'Data User Code of Conduct'}
                    initialValue={!!updatedProfile.dataUseAgreementBypassTime}
                    disabled={true}
                    onToggle={() => this.setState({saveDisabled: false})}
                    dataTestId={'dataUseAgreementBypassToggle'}
                />
              </FlexRow>
            </div>
          </FlexColumn>
        </FlexRow>
      </FlexColumn>}
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
