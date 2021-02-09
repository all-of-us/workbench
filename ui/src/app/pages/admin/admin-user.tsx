import * as fp from 'lodash/fp';
import * as React from 'react';

import {Component} from '@angular/core';

import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {SmallHeader} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {TextInputWithLabel, Toggle} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {institutionApi, profileApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  displayDateWithoutHours,
  formatFreeCreditsUSD,
  isBlank,
  reactStyles,
  ReactWrapperBase,
  withUrlParams
} from 'app/utils';

import {BulletAlignedUnorderedList} from 'app/components/lists';
import {TooltipTrigger} from 'app/components/popups';
import {
  getRoleOptions,
  MasterDuaEmailMismatchErrorMessage,
  RestrictedDuaEmailMismatchErrorMessage,
  validateEmail
} from 'app/utils/institutions';
import {navigate, serverConfigStore} from 'app/utils/navigation';
import {
  AccountPropertyUpdate,
  CheckEmailResponse,
  DuaType,
  InstitutionalRole,
  Profile,
  PublicInstitutionDetails,
} from 'generated/fetch';
import {Dropdown} from 'primereact/dropdown';
import * as validate from 'validate.js';

const styles = reactStyles({
  label: {
    fontWeight: 600
  },
  backgroundColorDark: {
    backgroundColor: colorWithWhiteness(colors.primary, .95)
  },
  textInput: {
    width: '17.5rem',
    opacity: '100%',
  },
  textInputContainer: {
    marginTop: '1rem'
  }
});

const CREDIT_LIMIT_DEFAULT_MIN = 300;
const CREDIT_LIMIT_DEFAULT_MAX = 800;
const CREDIT_LIMIT_DEFAULT_STEP = 50;

const DropdownWithLabel = ({label, options, initialValue, onChange, disabled= false, dataTestId, dropdownStyle = {}}) => {
  return <FlexColumn data-test-id={dataTestId} style={{marginTop: '1rem'}}>
    <label style={styles.label}>{label}</label>
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

const EmailValidationErrorMessage = ({emailValidationResponse, updatedProfile, verifiedInstitutionOptions}) => {
  if (updatedProfile && updatedProfile.verifiedInstitutionalAffiliation) {
    if (emailValidationResponse.isValidMember) {
      return null;
    } else {
      const {verifiedInstitutionalAffiliation} = updatedProfile;
      const selectedInstitution = fp.find(
        institution => institution.shortName === verifiedInstitutionalAffiliation.institutionShortName,
        verifiedInstitutionOptions
      );
      if (selectedInstitution.duaTypeEnum === DuaType.RESTRICTED) {
        // Institution has signed Restricted agreement and the email is not in allowed emails list
        return <RestrictedDuaEmailMismatchErrorMessage/>;
      } else {
        // Institution has MASTER or NULL agreement and the domain is not in the allowed list
        return <MasterDuaEmailMismatchErrorMessage/>;
      }
    }
  }
  return null;
};

interface FreeCreditsProps {
  isAboveLimit: boolean;
  usage: string;
}

const FreeCreditsUsage = ({isAboveLimit, usage}: FreeCreditsProps) => {
  const inputStyle = isAboveLimit ?
  {...styles.textInput,
    backgroundColor: colorWithWhiteness(colors.danger, .95),
    borderColor: colors.danger,
    color: colors.danger,
  } :
  {...styles.textInput,
    ...styles.backgroundColorDark,
    color: colors.disabled,
  };

  return <React.Fragment>
    <TextInputWithLabel
      labelText='Free credits used'
      value={usage}
      inputId='freeTierUsage'
      disabled={true}
      inputStyle={inputStyle}
      containerStyle={styles.textInputContainer}
    />
    {isAboveLimit && <div style={{color: colors.danger}}>Update free credit limit</div>}
  </React.Fragment>;
};

interface Props {
  // From withUrlParams
  urlParams: {
    usernameWithoutGsuiteDomain: string
  };
}

interface State {
  emailValidationError: string;
  emailValidationResponse: CheckEmailResponse;
  institutionsLoadingError: string;
  loading: boolean;
  oldProfile: Profile;
  profileLoadingError: string;
  updatedProfile: Profile;
  verifiedInstitutionOptions: Array<PublicInstitutionDetails>;
}


const AdminUser = withUrlParams()(class extends React.Component<Props, State> {

  private aborter: AbortController;

  constructor(props) {
    super(props);

    this.state = {
      emailValidationError: '',
      emailValidationResponse: null,
      institutionsLoadingError: '',
      loading: true,
      oldProfile: null,
      profileLoadingError: '',
      updatedProfile: null,
      verifiedInstitutionOptions: [],
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

  componentWillUnmount(): void {
    if (this.aborter) {
      this.aborter.abort();
    }
  }

  async validateEmail() {
    const {
      updatedProfile: {
        contactEmail,
        verifiedInstitutionalAffiliation: {institutionShortName}
      }
    } = this.state;

    await this.setState({loading: true});
    // Cancel any outstanding API calls.
    if (this.aborter) {
      this.aborter.abort();
    }
    this.aborter = new AbortController();
    this.setState({emailValidationResponse: null});

    // Early-exit with no result if either input is blank.
    if (!institutionShortName || isBlank(contactEmail)) {
      return;
    }

    try {
      const result = await validateEmail(contactEmail, institutionShortName, this.aborter);
      this.setState({
        emailValidationError: '',
        emailValidationResponse: result
      });
    } catch (e) {
      this.setState({
        emailValidationError: 'Error validating user email against institution - please refresh page and try again',
        emailValidationResponse: null,
      });
    }
    await this.setState({loading: false});
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

  getFreeCreditLimitOptions() {
    const {oldProfile: {freeTierDollarQuota}} = this.state;

    const defaultsPlusMaybeOverride = new Set(
      // gotcha: argument order for rangeStep is (step, start, end)
      // IntelliJ incorrectly believes takes the order is (start, end, step)
      fp.rangeStep(CREDIT_LIMIT_DEFAULT_STEP, CREDIT_LIMIT_DEFAULT_MIN, CREDIT_LIMIT_DEFAULT_MAX + 1))
      .add(freeTierDollarQuota);

    // gotcha: JS sorts numbers lexicographically by default
    const numericallySorted = Array.from(defaultsPlusMaybeOverride).sort((a, b) => a - b);

    return fp.map((limit) => ({label: formatFreeCreditsUSD(limit), value: limit}), numericallySorted);
  }

  getFreeCreditUsage(): string {
    const {updatedProfile: {freeTierDollarQuota, freeTierUsage}} = this.state;
    return `${formatFreeCreditsUSD(freeTierUsage)} used of ${formatFreeCreditsUSD(freeTierDollarQuota)} limit`;
  }

  usageIsAboveLimit(): boolean {
    const {updatedProfile: {freeTierDollarQuota, freeTierUsage}} = this.state;
    return freeTierDollarQuota < freeTierUsage;
  }

  async getInstitutions() {
    try {
      const institutionsResponse = await institutionApi().getPublicInstitutionDetails();
      const institutions = institutionsResponse.institutions;
      this.setState({verifiedInstitutionOptions: fp.sortBy( 'displayName', institutions)});
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
    return fp.map(({displayName, shortName}) => ({label: displayName, value: shortName}), verifiedInstitutionOptions);
  }

  isSaveDisabled(errors) {
    const {oldProfile, updatedProfile} = this.state;
    return fp.isEqual(oldProfile, updatedProfile) || errors;
  }

  async setVerifiedInstitutionOnProfile(institutionShortName: string) {
    const {verifiedInstitutionOptions} = this.state;
    await this.setState(fp.flow(
      fp.set(['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionShortName'], institutionShortName),
      fp.set(
          ['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionDisplayName'],
        verifiedInstitutionOptions.find(
              institution => institution.shortName === institutionShortName,
              verifiedInstitutionOptions
          ).displayName
      ),
      fp.set(['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionRoleEnum'], undefined),
      fp.set(['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionalRoleOtherText'], undefined)
      ));
    await this.validateEmail();
  }

  async setContactEmail(contactEmail: string) {
    await this.setState(fp.set(['updatedProfile', 'contactEmail'], contactEmail));
    await this.validateEmail();
  }

  setFreeTierCreditDollarLimit(newLimit: number) {
    this.setState(fp.set(['updatedProfile', 'freeTierDollarQuota'], newLimit));
  }

  setInstitutionalRoleOnProfile(institutionalRoleEnum: InstitutionalRole) {
    this.setState(fp.flow(
      fp.set(['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionalRoleEnum'], institutionalRoleEnum),
      fp.set(['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionalRoleOtherText'], undefined)
    ));
  }

  // returns the updated profile value only if it has changed
  updatedProfileValue(attribute: string) {
    const oldValue = fp.get(['oldProfile' , attribute], this.state);
    const updatedValue = fp.get(['updatedProfile' , attribute], this.state);
    if (!fp.isEqual(oldValue, updatedValue)) {
      return updatedValue;
    } else {
      return null;
    }
  }

  updateAccountProperties() {
    const {updatedProfile} = this.state;
    const {username} = updatedProfile;
    const request: AccountPropertyUpdate = {
      username,
      freeCreditsLimit: this.updatedProfileValue('freeTierDollarQuota'),
      contactEmail: this.updatedProfileValue('contactEmail'),
      affiliation: this.updatedProfileValue('verifiedInstitutionalAffiliation'),
      accessBypassRequests: [],  // coming soon: RW-4958
    };

    this.setState({loading: true});
    profileApi().updateAccountProperties(request).then((response) => {
      this.setState({oldProfile: response, updatedProfile: response, loading: false});
    });
  }

  validateCheckEmailResponse() {
    const {emailValidationResponse, emailValidationError} = this.state;

    // if we have never called validateEmail()
    if (!emailValidationResponse && !emailValidationError) {
      return true;
    }

    if (emailValidationResponse) {
      return emailValidationResponse.isValidMember;
    }
    return false;
  }

  validateVerifiedInstitutionalAffiliation() {
    const {updatedProfile} = this.state;
    if (updatedProfile && updatedProfile.verifiedInstitutionalAffiliation) {
      return updatedProfile.verifiedInstitutionalAffiliation;
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
      emailValidationError,
      emailValidationResponse,
      institutionsLoadingError,
      profileLoadingError,
      updatedProfile,
      verifiedInstitutionOptions
    } = this.state;
    const errors = validate({
      'verifiedInstitutionalAffiliation': this.validateVerifiedInstitutionalAffiliation(),
      'institutionShortName': this.validateInstitutionShortname(),
      'institutionalRoleEnum': this.validateInstitutionalRoleEnum(),
      'institutionalRoleOtherText': this.validateInstitutionalRoleOtherText(),
      'institutionMembership': this.validateCheckEmailResponse(),
    }, {
      verifiedInstitutionalAffiliation: {truthiness: true},
      institutionShortName: {truthiness: true},
      institutionalRoleEnum: {truthiness: true},
      institutionalRoleOtherText: {truthiness: true},
      institutionMembership: {truthiness: true}
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
      {emailValidationError && <div>{emailValidationError}</div>}
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
                onToggle={() => {}}
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
                  {errors.institutionMembership && <li>The user's contact email does not match the selected institution</li>}
                </BulletAlignedUnorderedList>
              }
          >
            <Button
                type='primary'
                disabled={this.isSaveDisabled(errors)}
                onClick={() => this.updateAccountProperties()}
            >
              Save
            </Button>
          </TooltipTrigger>
        </FlexRow>
        <FlexRow>
          <FlexColumn style={{width: '33%', marginRight: '1rem'}}>
            <TextInputWithLabel
                labelText={'User name'}
                placeholder={updatedProfile.givenName + ' ' + updatedProfile.familyName}
                inputId={'userFullName'}
                disabled={true}
                inputStyle={{...styles.textInput, ...styles.backgroundColorDark}}
                containerStyle={styles.textInputContainer}
            />
            <TextInputWithLabel
                labelText={'Registration state'}
                placeholder={fp.capitalize(updatedProfile.dataAccessLevel.toString())}
                inputId={'registrationState'}
                disabled={true}
                inputStyle={{...styles.textInput, ...styles.backgroundColorDark}}
                containerStyle={styles.textInputContainer}
            />
            <TextInputWithLabel
                labelText={'Registration date'}
                placeholder={
                  updatedProfile.firstRegistrationCompletionTime
                      ? displayDateWithoutHours(updatedProfile.firstRegistrationCompletionTime)
                      : ''
                }
                inputId={'firstRegistrationCompletionTime'}
                disabled={true}
                inputStyle={{...styles.textInput, ...styles.backgroundColorDark}}
                containerStyle={styles.textInputContainer}
            />
            <TextInputWithLabel
                labelText={'Username'}
                placeholder={updatedProfile.username}
                inputId={'username'}
                disabled={true}
                inputStyle={{...styles.textInput, ...styles.backgroundColorDark}}
                containerStyle={styles.textInputContainer}
            />
            <TextInputWithLabel
                labelText={'Contact email'}
                value={updatedProfile.contactEmail}
                inputId={'contactEmail'}
                inputStyle={{...styles.textInput, ...styles.backgroundColorDark}}
                containerStyle={styles.textInputContainer}
                onChange={email => this.setContactEmail(email)}
            />
            <FreeCreditsUsage
              isAboveLimit={this.usageIsAboveLimit()}
              usage={this.getFreeCreditUsage()}
            />
          </FlexColumn>
          <FlexColumn style={{width: '33%'}}>
            <DropdownWithLabel
                label={'Free credit limit'}
                options={this.getFreeCreditLimitOptions()}
                onChange={async(event) => this.setFreeTierCreditDollarLimit(event.value)}
                initialValue={updatedProfile.freeTierDollarQuota}
                dropdownStyle={{width: '4.5rem'}}
                dataTestId={'freeTierDollarQuota'}
            />
            {verifiedInstitutionOptions && <DropdownWithLabel
                label={'Verified institution'}
                options={this.getInstitutionDropdownOptions()}
                onChange={async(event) => this.setVerifiedInstitutionOnProfile(event.value)}
                initialValue={
                  updatedProfile.verifiedInstitutionalAffiliation
                      ? updatedProfile.verifiedInstitutionalAffiliation.institutionShortName
                      : undefined
                }
                dataTestId={'verifiedInstitution'}
            />}
            {emailValidationResponse && !emailValidationResponse.isValidMember && <EmailValidationErrorMessage
              emailValidationResponse={emailValidationResponse}
              updatedProfile={updatedProfile}
              verifiedInstitutionOptions={verifiedInstitutionOptions}
            />}
            {verifiedInstitutionOptions
              && updatedProfile.verifiedInstitutionalAffiliation
              && <DropdownWithLabel
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
                labelText={'Institutional role description'}
                placeholder={updatedProfile.verifiedInstitutionalAffiliation.institutionalRoleOtherText}
                onChange={(value) => this.setState(fp.set(['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionalRoleOtherText'], value))}
                dataTestId={'institutionalRoleOtherText'}
                inputStyle={styles.textInput}
                containerStyle={styles.textInputContainer}
              />
            }
            <div style={{marginTop: '1rem', width: '15rem'}}>
              <label style={{fontWeight: 600}}>Bypass access to:</label>
              <FlexRow style={{marginTop: '.5rem'}}>
                <ToggleWithLabelAndToggledText
                    label={'2-factor auth'}
                    initialValue={!!updatedProfile.twoFactorAuthBypassTime}
                    disabled={true}
                    onToggle={() => {}}
                    dataTestId={'twoFactorAuthBypassToggle'}
                />
                <ToggleWithLabelAndToggledText
                    label={'Compliance training'}
                    initialValue={!!updatedProfile.complianceTrainingBypassTime}
                    disabled={true}
                    onToggle={() => {}}
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
                    onToggle={() => {}}
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
