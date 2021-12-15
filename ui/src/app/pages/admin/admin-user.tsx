import * as fp from 'lodash/fp';
import * as React from 'react';
import {RouteComponentProps, withRouter} from 'react-router-dom';
import validate from 'validate.js';

import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {SmallHeader} from 'app/components/headers';
import {TextInputWithLabel, Toggle} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {userAdminApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  hasNewValidProps,
  isBlank,
  reactStyles,
} from 'app/utils';
import {BulletAlignedUnorderedList} from 'app/components/lists';
import {TooltipTrigger} from 'app/components/popups';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {
  EmailAddressMismatchErrorMessage,
  EmailDomainMismatchErrorMessage,
  getRegisteredTierConfig,
  validateEmail
} from 'app/utils/institutions';
import {MatchParams, serverConfigStore} from 'app/utils/stores';
import {
  AccessModule,
  AccessModuleStatus,
  AccountPropertyUpdate,
  CheckEmailResponse,
  InstitutionalRole,
  InstitutionMembershipRequirement,
  Profile,
  PublicInstitutionDetails,
} from 'generated/fetch';
import {accessRenewalModules, computeDisplayDates, getAccessModuleConfig} from 'app/utils/access-utils';
import {hasRegisteredTierAccess} from 'app/utils/access-tiers';
import { EgressEventsTable } from './egress-events-table';
import {
  adminGetProfile,
  UserAdminTableLink,
  commonStyles,
  getFreeCreditUsage,
  FreeCreditsDropdown,
  InstitutionDropdown,
  InstitutionalRoleDropdown,
  InstitutionalRoleOtherTextInput,
  getPublicInstitutionDetails,
  ContactEmailTextInput,
} from './admin-user-common';

const styles = reactStyles({
  ...commonStyles,
  backgroundColorDark: {
    backgroundColor: colorWithWhiteness(colors.primary, .95)
  },
  label: {
    color: colors.primary,
    fontSize: '14px',
    fontWeight: 600,
    paddingTop: '1em',
    paddingLeft: 0,
  },
  textInput: {
    width: '17.5rem',
    opacity: '100%',
    marginLeft: 0,
  },
  textInputContainer: {
    marginTop: '1rem'
  },
})
const getUserStatus = (profile: Profile) => {
  return (hasRegisteredTierAccess(profile))
      ? () => <div style={{color: colors.success}}>Enabled</div>
      : () => <div style={{color: colors.danger}}>Disabled</div>;
}

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
      if (getRegisteredTierConfig(selectedInstitution).membershipRequirement === InstitutionMembershipRequirement.ADDRESSES) {
        // Institution requires an exact email address match and the email is not in allowed emails list
        return <EmailAddressMismatchErrorMessage/>;
      } else {
        // Institution requires email domain matching and the domain is not in the allowed list
        return <EmailDomainMismatchErrorMessage/>;
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

interface ExpirationProps {
  modules: Array<AccessModuleStatus>;
  UserStatusComponent: () => JSX.Element;
}
const AccessModuleExpirations = ({modules, UserStatusComponent}: ExpirationProps) => {
  // compliance training is feature-flagged in some environments
  const {enableComplianceTraining} = serverConfigStore.get().config;
  const moduleNames = enableComplianceTraining
      ? accessRenewalModules
      : accessRenewalModules.filter(moduleName => moduleName !== AccessModule.COMPLIANCETRAINING);

  return <FlexColumn style={{marginTop: '1rem'}}>
    <label style={styles.semiBold}>Data Access Status: <UserStatusComponent/></label>
    {moduleNames.map((moduleName, zeroBasedStep) => {
      // return the status if found; init an empty status with the moduleName if not
      const status: AccessModuleStatus = modules.find(s => s.moduleName === moduleName) || {moduleName};
      const {lastConfirmedDate, nextReviewDate} = computeDisplayDates(status);
      const {AARTitleComponent} = getAccessModuleConfig(moduleName);
      return <FlexRow key={zeroBasedStep} style={{marginTop: '0.5rem'}}>
        <FlexColumn>
          <label style={styles.semiBold}>Step {zeroBasedStep + 1}: <AARTitleComponent/></label>
          <div>Last Updated On: {lastConfirmedDate}</div>
          <div>Next Review: {nextReviewDate}</div>
        </FlexColumn>
      </FlexRow>})}
  </FlexColumn>;
}

interface Props extends WithSpinnerOverlayProps, RouteComponentProps<MatchParams> {}

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

export const AdminUser = withRouter(class extends React.Component<Props, State> {

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
    this.props.hideSpinner();
    this.getUserData();
  }

  componentDidUpdate(prevProps: Readonly<Props>) {
    if (hasNewValidProps(this.props, prevProps, [p => p.match.params.usernameWithoutGsuiteDomain])) {
      this.getUserData();
    }
  }

  async getUserData() {
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

    this.setState({loading: true});
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
    this.setState({loading: false});
  }

  async getUser() {
    try {
      const profile = await adminGetProfile(this.props.match.params.usernameWithoutGsuiteDomain);
      this.setState({oldProfile: profile, updatedProfile: profile, profileLoadingError: ''});
    } catch (error) {
      this.setState({profileLoadingError: 'Could not find user - please check spelling of username and try again'});
    }
  }

  usageIsAboveLimit(): boolean {
    const {updatedProfile: {freeTierDollarQuota, freeTierUsage}} = this.state;
    return freeTierDollarQuota < freeTierUsage;
  }

  async getInstitutions() {
    try {
      this.setState({verifiedInstitutionOptions: await getPublicInstitutionDetails()});
    } catch (error) {
      this.setState({institutionsLoadingError: 'Could not get list of verified institutions - please try again later'});
    }
  }

  isSaveDisabled(errors) {
    const {oldProfile, updatedProfile} = this.state;
    return fp.isEqual(oldProfile, updatedProfile) || errors;
  }

  async setVerifiedInstitutionOnProfile(institutionShortName: string) {
    const {verifiedInstitutionOptions} = this.state;
    this.setState(fp.flow(
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
    this.setState(fp.set(['updatedProfile', 'contactEmail'], contactEmail));
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
    userAdminApi().updateAccountProperties(request).then((response) => {
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
      verifiedInstitutionOptions,
      oldProfile
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
    return <FadeBox style={styles.fadeBox}>
      {emailValidationError && <div>{emailValidationError}</div>}
      {institutionsLoadingError && <div>{institutionsLoadingError}</div>}
      {profileLoadingError && <div>{profileLoadingError}</div>}
      {updatedProfile && <FlexColumn>
        <FlexRow style={{alignItems: 'center'}}>
          <UserAdminTableLink/>
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
                labelText={'Access tiers'}
                placeholder={
                  fp.flow(
                    fp.map(fp.capitalize),
                    fp.join(', '))
                  (updatedProfile.accessTierShortNames)
                }
                inputId={'accessTiers'}
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
            <ContactEmailTextInput
              contactEmail={updatedProfile.contactEmail}
              onChange={email => this.setContactEmail(email)}
              labelStyle={styles.label}
              inputStyle={styles.textInput}
              containerStyle={styles.textInputContainer}/>
            <FreeCreditsUsage
              isAboveLimit={this.usageIsAboveLimit()}
              usage={getFreeCreditUsage(this.state.updatedProfile)}
            />
          </FlexColumn>
          <FlexColumn style={{width: '33%'}}>
            <FreeCreditsDropdown
              initialLimit={oldProfile?.freeTierDollarQuota}
              currentLimit={updatedProfile.freeTierDollarQuota}
              labelStyle={styles.label}
              dropdownStyle={styles.textInput}
              onChange={async(event) => this.setFreeTierCreditDollarLimit(event.value)}/>
            <InstitutionDropdown
              institutions={verifiedInstitutionOptions}
              initialInstitutionShortName={updatedProfile.verifiedInstitutionalAffiliation?.institutionShortName}
              labelStyle={styles.label}
              dropdownStyle={styles.textInput}
              onChange={async(event) => this.setVerifiedInstitutionOnProfile(event.value)}/>
            {emailValidationResponse && !emailValidationResponse.isValidMember && <EmailValidationErrorMessage
              emailValidationResponse={emailValidationResponse}
              updatedProfile={updatedProfile}
              verifiedInstitutionOptions={verifiedInstitutionOptions}/>}
            <InstitutionalRoleDropdown
              institutions={verifiedInstitutionOptions}
              initialAffiliation={updatedProfile.verifiedInstitutionalAffiliation}
              labelStyle={styles.label}
              dropdownStyle={styles.textInput}
              onChange={(event) => this.setInstitutionalRoleOnProfile(event.value)}/>
            <InstitutionalRoleOtherTextInput
              affiliation={updatedProfile.verifiedInstitutionalAffiliation}
              labelStyle={styles.label}
              inputStyle={styles.textInput}
              containerStyle={styles.textInputContainer}
              onChange={(value) => this.setState(
                fp.set(['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionalRoleOtherText'], value))
              }/>
            <AccessModuleExpirations modules={updatedProfile.accessModules.modules} UserStatusComponent={getUserStatus(updatedProfile)}/>
          </FlexColumn>
        </FlexRow>
        <FlexRow>
          <h2>Egress event history</h2>
        </FlexRow>
        <FlexRow>
          <EgressEventsTable displayPageSize={10} sourceUserEmail={updatedProfile.username} />
        </FlexRow>
      </FlexColumn>}
      {this.state.loading && <SpinnerOverlay/>}
    </FadeBox>;
  }
});
