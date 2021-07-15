import * as fp from 'lodash/fp';
import * as React from 'react';
import * as validate from 'validate.js';

import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ControlledTierBadge, ExclamationTriangle} from 'app/components/icons';
import {TextAreaWithLengthValidationMessage, TextInput, ValidationError} from 'app/components/inputs';
import {BulletAlignedUnorderedList} from 'app/components/lists';
import {Modal} from 'app/components/modals';
import {withErrorModal, withSuccessModal} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {AoU} from 'app/components/text-wrappers';
import {withProfileErrorModal, WithProfileErrorModalProps} from 'app/components/with-error-modal';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {getRegistrationTasksMap} from 'app/pages/homepage/registration-dashboard';
import {AccountCreationOptions} from 'app/pages/login/account-creation/account-creation-options';
import {DataAccessPanel} from 'app/pages/profile/data-access-panel';
import {DemographicSurvey} from 'app/pages/profile/demographic-survey';
import {ProfileRegistrationStepStatus} from 'app/pages/profile/profile-registration-step-status';
import {styles} from 'app/pages/profile/profile-styles';
import {institutionApi, profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  displayDateWithoutHours,
  formatFreeCreditsUSD,
  lensOnProps,
  withUserProfile
} from 'app/utils';
import {wasReferredFromRenewal} from 'app/utils/access-utils';
import {convertAPIError, reportError} from 'app/utils/errors';
import {navigate} from 'app/utils/navigation';
import {serverConfigStore} from 'app/utils/stores';
import {environment} from 'environments/environment';
import {InstitutionalRole, Profile} from 'generated/fetch';
import {PublicInstitutionDetails} from 'generated/fetch';
import {Dropdown} from 'primereact/dropdown';


// validators for validate.js
const required = {presence: {allowEmpty: false}};
const notTooLong = maxLength => ({
  length: {
    maximum: maxLength,
    tooLong: 'must be %{count} characters or less'
  }
});

const validators = {
  givenName: {...required, ...notTooLong(80)},
  familyName: {...required, ...notTooLong(80)},
  areaOfResearch: {...required, ...notTooLong(2000)},
  streetAddress1: {...required, ...notTooLong(95)},
  streetAddress2: notTooLong(95),
  zipCode: {...required, ...notTooLong(10)},
  city: {...required, ...notTooLong(95)},
  state: {...required, ...notTooLong(95)},
  country: {...required, ...notTooLong(95)}
};

enum RegistrationStepStatus {
  COMPLETED,
  BYPASSED,
  UNCOMPLETE
}

interface ProfilePageProps extends WithProfileErrorModalProps, WithSpinnerOverlayProps {
  profileState: {
    profile: Profile;
    reload: () => {};
  };
  controlledTierProfile: {
    controlledTierCompletionTime?: number
    controlledTierBypassTime?: number
    controlledTierEnabled?: boolean
  };
}

interface ProfilePageState {
  currentProfile: Profile;
  institutions: Array<PublicInstitutionDetails>;
  showDemographicSurveyModal: boolean;
  updating: boolean;
}

interface CompletionTime {
  completionTime: number;
  bypassTime: number;
}

const getRegistrationStatus = (completionTime: number, bypassTime: number) => {
  return completionTime !== null && completionTime !== undefined ? RegistrationStepStatus.COMPLETED :
  bypassTime !== null && completionTime !== undefined ? RegistrationStepStatus.BYPASSED : RegistrationStepStatus.UNCOMPLETE;
};

const bypassedText = (bypassTime: number): JSX.Element => {
  return <React.Fragment>
  <div>Bypassed on:</div>
  <div>{displayDateWithoutHours(bypassTime)}</div>
</React.Fragment>;
};

const getCompleteOrBypassContent = ({bypassTime, completionTime}: CompletionTime): JSX.Element => {
  switch (getRegistrationStatus(completionTime, bypassTime)) {
    case RegistrationStepStatus.COMPLETED:
      return <React.Fragment>
      <div>Completed on:</div>
      <div>{displayDateWithoutHours(completionTime)}</div>
    </React.Fragment>;
    case RegistrationStepStatus.BYPASSED:
      return bypassedText(bypassTime);
    default:
      return;
  }
};

const focusCompletionProps = lensOnProps(['completionTime', 'bypassTime']);

const getTwoFactorContent = fp.flow(
  focusCompletionProps(['twoFactorAuthCompletionTime', 'twoFactorAuthBypassTime']),
  getCompleteOrBypassContent
);

const getControlledTierContent = fp.flow(
  focusCompletionProps(['controlledTierCompletionTime', 'controlledTierBypassTime']),
  getCompleteOrBypassContent
);

export const ProfileComponent = fp.flow(
  withUserProfile(),
  withProfileErrorModal
  )(class extends React.Component<
    ProfilePageProps,
    ProfilePageState
> {

    constructor(props) {
      super(props);

      this.state = {
        currentProfile: this.initializeProfile(),
        institutions: [],
        showDemographicSurveyModal: false,
        updating: false
      };
    }
    static displayName = 'ProfilePage';

    saveProfileWithRenewal = withSuccessModal({
      title: 'Your profile has been updated',
      message: 'You will be redirected to the access renewal page upon closing this dialog.',
      onDismiss: () => navigate(['access-renewal'])
    }, this.saveProfile.bind(this));

    confirmProfile = fp.flow(
      withSuccessModal({
        title: 'You have confirmed your profile is accurate',
        message: 'You will be redirected to the access renewal page upon closing this dialog.',
        onDismiss: () => navigate(['access-renewal'])
      }),
      withErrorModal({ title: 'Failed To Confirm Profile', message: 'An error occurred trying to confirm your profile. Please try again.'})
    )(async() => {
      this.setState({updating: true});
      await profileApi().confirmProfile();
      this.setState({updating: false});
    });

    async componentDidMount() {
      this.props.hideSpinner();
      try {
        const details = await institutionApi().getPublicInstitutionDetails();
        this.setState({
          institutions: details.institutions
        });
      } catch (e) {
        reportError(e);
      }
    }

    navigateToTraining(): void {
      window.location.assign(
        environment.trainingUrl + '/static/data-researcher.html?saml=on');
    }

    initializeProfile() {
      if (!this.props.profileState.profile) {
        return this.createInitialProfile();
      }
      if (!this.props.profileState.profile.address) {
        this.props.profileState.profile.address = {
          streetAddress1: '',
          city: '',
          state: '',
          zipCode: '',
          country: ''
        };
      }
      return this.props.profileState.profile;
    }

    createInitialProfile(): Profile {
      return {
        ...this.props.profileState.profile,
        demographicSurvey: {}
      };
    }

    componentDidUpdate(prevProps) {
      const {profileState: {profile}} = this.props;

      if (!fp.isEqual(prevProps.profileState.profile, profile)) {
        this.setState({currentProfile: profile}); // for when profile loads after component load
      }
    }

    getRoleOptions(): Array<{label: string, value: InstitutionalRole}> {
      const {institutions, currentProfile} = this.state;
      if (currentProfile) {
        const selectedOrgType = institutions.find(
          inst => inst.shortName === currentProfile.verifiedInstitutionalAffiliation.institutionShortName);
        if (selectedOrgType) {
          const sel = selectedOrgType.organizationTypeEnum;

          const availableRoles: Array<InstitutionalRole> =
           AccountCreationOptions.institutionalRolesByOrganizationType
               .find(obj => obj.type === sel)
               .roles;

          return AccountCreationOptions.institutionalRoleOptions.filter(option =>
           availableRoles.includes(option.value)
       );
        }
      }
    }

    saveProfileErrorMessage(errors) {
      return <React.Fragment>
      <div>You must correct errors before saving: </div>
      <BulletAlignedUnorderedList>
      {Object.keys(errors).map((key) => <li key={errors[key][0]}>{errors[key][0]}</li>)}
      </BulletAlignedUnorderedList>
    </React.Fragment>;
    }

    async saveProfile(profile: Profile): Promise<Profile> {
      const {profileState: {reload}} = this.props;

      this.setState({updating: true});

      try {
        await profileApi().updateProfile(profile);
        await reload();
        return profile;
      } catch (error) {
        reportError(error);
        const errorResponse = await convertAPIError(error);
        this.props.showProfileErrorModal(errorResponse.message);
        console.error(error);
        return Promise.reject();
      } finally {
        this.setState({updating: false});
      }
    }

    private getEraCommonsCardText(profile) {
      switch (getRegistrationStatus(profile.eraCommonsCompletionTime, profile.eraCommonsBypassTime)) {
        case RegistrationStepStatus.COMPLETED:
          return <div>
          {profile.eraCommonsLinkedNihUsername != null && <React.Fragment>
              <div> Username:</div>
              <div> {profile.eraCommonsLinkedNihUsername} </div>
          </React.Fragment>}
          {profile.eraCommonsLinkExpireTime != null &&
          //  Firecloud returns eraCommons link expiration as 0 if there is no linked account.
          profile.eraCommonsLinkExpireTime !== 0
          && <React.Fragment>
              <div> Completed on:</div>
              <div>
                {displayDateWithoutHours(profile.eraCommonsCompletionTime)}
              </div>
          </React.Fragment>}
        </div>;
        case RegistrationStepStatus.BYPASSED:
          return bypassedText(profile.twoFactorAuthBypassTime);
        default:
          return;
      }
    }

    private getComplianceTrainingText(profile) {
      switch (getRegistrationStatus(profile.complianceTrainingCompletionTime, profile.complianceTrainingBypassTime)) {
        case RegistrationStepStatus.COMPLETED:
          return <React.Fragment>
          <div>Training Completed</div>
          <div>{displayDateWithoutHours(profile.complianceTrainingCompletionTime)}</div>
        </React.Fragment>;
        case RegistrationStepStatus.BYPASSED:
          return bypassedText(profile.complianceTrainingBypassTime);
        default:
          return;
      }
    }

    private getDataUseAgreementText(profile) {
      const universalText = <a onClick={getRegistrationTasksMap()['dataUserCodeOfConduct'].onClick}>
      View code of conduct
    </a>;
      switch (getRegistrationStatus(profile.dataUseAgreementCompletionTime, profile.dataUseAgreementBypassTime)) {
        case RegistrationStepStatus.COMPLETED:
          return <React.Fragment>
          <div>Signed On:</div>
          <div>
            {displayDateWithoutHours(profile.dataUseAgreementCompletionTime)}
          </div>
          {universalText}
        </React.Fragment>;
        case RegistrationStepStatus.BYPASSED:
          return <React.Fragment>
          {bypassedText(profile.dataUseAgreementBypassTime)}
          {universalText}
        </React.Fragment>;
        case RegistrationStepStatus.UNCOMPLETE:
          return universalText;
      }
    }

    render() {
      const {
        profileState: {
          profile
        },
        // TODO: when the controlled tier data is available fetch it from the profile
        controlledTierProfile: {
          controlledTierEnabled = false, controlledTierBypassTime = null, controlledTierCompletionTime = null
        } = {}
      } = this.props;
      const {currentProfile, updating, showDemographicSurveyModal} = this.state;
      const {enableComplianceTraining, enableEraCommons} =
      serverConfigStore.get().config;
      const {
      givenName, familyName, areaOfResearch, professionalUrl,
        address: {
        streetAddress1,
        streetAddress2,
          zipCode,
          city,
            state,
            country
        }
    } = currentProfile;


      const hasExpired = fp.flow(
        fp.find({moduleName: 'profileConfirmation'}),
        fp.get('hasExpired')
      )(profile.renewableAccessModules.modules);
      const urlError = professionalUrl
      ? validate({website: professionalUrl}, {website: {url: {message: '^Professional URL %{value} is not a valid URL'}}})
      : undefined;
      const errorMessages = {
        ...urlError,
        ...validate({
          givenName,
          familyName, areaOfResearch,
          streetAddress1,
          streetAddress2,
          zipCode,
          city,
          state,
          country
        },
          validators, {
            prettify: v => ({
              givenName: 'First Name',
              familyName: 'Last Name',
              areaOfResearch: 'Current Research'
            }[v] || validate.prettify(v))
          })
      };
      const errors = fp.isEmpty(errorMessages) ? undefined : errorMessages;

      const makeProfileInput = ({title, valueKey, isLong = false, ...props}) => {
        let errorText = profile && errors && errors[valueKey];
        if (valueKey && !Array.isArray(valueKey)) {
          valueKey = [valueKey];
        }
        if (valueKey && valueKey.length > 1) {
          errorText = profile && errors && errors[valueKey[1]];
        }
        const inputProps = {
          value: fp.get(valueKey, currentProfile) || '',
          onChange: v => this.setState(fp.set(['currentProfile', ...valueKey], v)),
          invalid: !!errorText,
          style: props.style,
          maxCharacters: props.maxCharacters,
          tooLongWarningCharacters: props.tooLongWarningCharacters,
          ...props
        };
        const id = props.id || valueKey;

        return <div style={{marginBottom: 40}}>
        <div style={styles.inputLabel}>{title}</div>
        {isLong ? <TextAreaWithLengthValidationMessage
            id={id} data-test-id={id}
            heightOverride={styles.longInputHeightStyle}
            initialText={inputProps.value}
            maxCharacters={inputProps.maxCharacters}
            tooLongWarningCharacters={inputProps.tooLongWarningCharacters}
            {...inputProps}
            textBoxStyleOverrides={{...styles.longInputContainerStyle, ...inputProps.style}}
          />  :
            <TooltipTrigger content='This field cannot be edited' disabled={!props.disabled}>
          <TextInput  data-test-id={props.id || valueKey}
            {...inputProps}
            style={{...styles.inputStyle, ...inputProps.style}}
          /></TooltipTrigger>}
        <ValidationError>{errorText}</ValidationError>
      </div>;
      };

      return <FadeBox style={styles.fadebox}>
      <div style={{width: '95%'}}>
        {(!profile || updating) && <SpinnerOverlay/>}
        <div style={{...styles.h1, marginBottom: '0.7rem'}}>Profile</div>
        <FlexRow style={{justifyContent: 'spaceBetween'}}>
          <div>
            {(hasExpired || wasReferredFromRenewal()) &&
              <div style={styles.renewalBox}>
                <ExclamationTriangle size={25} color={colors.warning} style={{margin: '0.5rem'}}/>
                <div style={{color: colors.primary, fontWeight: 600}}>Please update or verify your profile.</div>
                <a onClick={() => this.confirmProfile()} style={{margin: '0 0.5rem 0 auto', textDecoration: 'underline'}}>Looks Good</a>
              </div>
            }
            <div style={styles.title}>Public displayed Information</div>
            <hr style={{...styles.verticalLine, width: '64%'}}/>
            <FlexRow style={{marginTop: '1rem'}}>
              {makeProfileInput({
                title: 'First Name',
                valueKey: 'givenName'
              })}
              {makeProfileInput({
                title: 'Last Name',
                valueKey: 'familyName'
              })}
            </FlexRow>
            <FlexRow>
              <FlexColumn>
                {makeProfileInput({
                  title: 'Your Institution',
                  valueKey: ['verifiedInstitutionalAffiliation', 'institutionDisplayName'],
                  disabled: true
                })}
                {!profile.verifiedInstitutionalAffiliation &&
                  <div style={{color: colors.danger}}>
                    Institution cannot be empty. Please contact admin.
                  </div>}
              </FlexColumn>
              <FlexColumn>
                <div style={styles.inputLabel}>Your Role</div>
                {profile.verifiedInstitutionalAffiliation &&
                  <Dropdown style={{width: '12.5rem'}}
                            data-test-id='role-dropdown'
                            placeholder='Your Role'
                            options={this.getRoleOptions()}
                            disabled={true}
                            value={currentProfile.verifiedInstitutionalAffiliation.institutionalRoleEnum}/>}

                {currentProfile.verifiedInstitutionalAffiliation &&
                currentProfile.verifiedInstitutionalAffiliation.institutionalRoleEnum &&
                currentProfile.verifiedInstitutionalAffiliation.institutionalRoleEnum ===
                InstitutionalRole.OTHER && <div>{makeProfileInput({
                  title: '',
                  valueKey: ['verifiedInstitutionalAffiliation', 'institutionalRoleOtherText'],
                  style: {marginTop: '1rem'},
                  disabled: true
                })}
                </div>}
              </FlexColumn>
            </FlexRow>

            <FlexRow style={{width: '100%'}}>
              {makeProfileInput({
                title: 'Professional URL',
                valueKey: 'professionalUrl',
                style: {width: '26rem'}
              })}
            </FlexRow>
            <FlexRow>

              {makeProfileInput({
                title: <FlexColumn>
                  <div>Your research background, experience and research interests</div>
                  <div style={styles.researchPurposeInfo}>
                    This information will be posted publicly on the <i>AoU</i> Research Hub Website
                    to
                    inform the <i>AoU</i> Research Participants.
                  </div>
                </FlexColumn>,
                maxCharacters: 2000,
                tooLongWarningCharacters: 1900,
                valueKey: 'areaOfResearch',
                isLong: true,
                style: {width: '26rem'}
              })}
            </FlexRow>
            <div style={{width: '65%', marginTop: '0.5rem'}}>
              <div style={styles.title}>Private Information</div>
              <hr style={{...styles.verticalLine, width: '26rem'}}/>
              <FlexRow style={{marginTop: '1rem'}}>
                {makeProfileInput({
                  title: 'User name',
                  valueKey: 'username',
                  disabled: true
                })}
                {makeProfileInput({
                  title: 'Institutional email address',
                  valueKey: 'contactEmail',
                  disabled: true
                })}
              </FlexRow>
              <FlexRow>
                {makeProfileInput({
                  title: 'Street address 1',
                  valueKey: ['address', 'streetAddress1'],
                  id: 'streetAddress1'
                })}
                {makeProfileInput({
                  title: 'Street address 2',
                  valueKey: ['address', 'streetAddress2'],
                  id: 'streetAddress2'
                })}
              </FlexRow>
              <FlexRow>
                {makeProfileInput({
                  title: 'City',
                  valueKey: ['address', 'city'],
                  id: 'city'
                })}
                {makeProfileInput({
                  title: 'State',
                  valueKey: ['address', 'state'],
                  id: 'state'
                })}
              </FlexRow>
              <FlexRow>
                {makeProfileInput({
                  title: 'Zip Code',
                  valueKey: ['address', 'zipCode'],
                  id: 'zipCode'
                })}
                {makeProfileInput({
                  title: 'Country',
                  valueKey: ['address', 'country'],
                  id: 'country'
                })}
              </FlexRow>
            </div>
          </div>
          <div style={{width: '20rem', marginRight: '4rem'}}>
            <div style={styles.title}>Free credits balance
            </div>
            <hr style={{...styles.verticalLine}}/>
            {profile && <FlexRow style={styles.freeCreditsBox}>
                <FlexColumn style={{marginLeft: '0.8rem'}}>
                    <div style={{marginTop: '0.4rem'}}><i>All of Us</i> free credits used:</div>
                    <div>Remaining <i>All of Us</i> free credits:</div>
                </FlexColumn>
                <FlexColumn style={{alignItems: 'flex-end', marginLeft: '1.0rem'}}>
                  <div style={{marginTop: '0.4rem', fontWeight: 600}}>{formatFreeCreditsUSD(profile.freeTierUsage)}</div>
                  <div style={{fontWeight: 600}}>{formatFreeCreditsUSD(profile.freeTierDollarQuota - profile.freeTierUsage)}</div>
                </FlexColumn>
            </FlexRow>}
            {controlledTierEnabled && <DataAccessPanel tiers={profile.accessTierShortNames}/>}
            <div style={styles.title}>
              Requirements for <AoU/> Workbench access
            </div>
            <hr style={{...styles.verticalLine}}/>
            <div style={{display: 'grid', gap: '10px', gridAutoRows: '225px', gridTemplateColumns: '220px 220px'}}>
              {controlledTierEnabled && <ProfileRegistrationStepStatus
                title={<span><i>All of Us</i> Controlled Tier Data Training</span>}
                wasBypassed={!!controlledTierBypassTime}
                incompleteButtonText={'Get Started'}
                completedButtonText={'Completed'}
                isComplete={!!(controlledTierCompletionTime || controlledTierBypassTime)}
                // TODO: link to the training modules once they are available
                completeStep={() => null}
                content={getControlledTierContent({controlledTierCompletionTime, controlledTierBypassTime})}
                >
                <div>
                  {!(controlledTierCompletionTime || controlledTierBypassTime) && <div>To be completed</div>}
                  <ControlledTierBadge/>
                </div>
              </ProfileRegistrationStepStatus>}
              <ProfileRegistrationStepStatus
                title='Turn on Google 2-Step Verification'
                wasBypassed={!!profile.twoFactorAuthBypassTime}
                incompleteButtonText='Set Up'
                completedButtonText={getRegistrationTasksMap()['twoFactorAuth'].completedText}
                isComplete={!!(getRegistrationTasksMap()['twoFactorAuth'].completionTimestamp(profile))}
                completeStep={getRegistrationTasksMap()['twoFactorAuth'].onClick}
                content={getTwoFactorContent(profile)}
                >
              </ProfileRegistrationStepStatus>
              {enableEraCommons && <ProfileRegistrationStepStatus
                  title='Connect Your eRA Commons Account'
                  wasBypassed={!!profile.eraCommonsBypassTime}
                  incompleteButtonText='Link'
                  completedButtonText={getRegistrationTasksMap()['eraCommons'].completedText}
                  isComplete={!!(getRegistrationTasksMap()['eraCommons'].completionTimestamp(profile))}
                  completeStep={getRegistrationTasksMap()['eraCommons'].onClick}
                  content={this.getEraCommonsCardText(profile)}
                >
              </ProfileRegistrationStepStatus>}
              {enableComplianceTraining && <ProfileRegistrationStepStatus
                  title={<span><i>All of Us</i> Responsible Conduct of Research Training</span>}
                  wasBypassed={!!profile.complianceTrainingBypassTime}
                  incompleteButtonText='Access Training'
                  completedButtonText={getRegistrationTasksMap()['complianceTraining'].completedText}
                  isComplete={!!(getRegistrationTasksMap()['complianceTraining'].completionTimestamp(profile))}
                  completeStep={getRegistrationTasksMap()['complianceTraining'].onClick}
                  content={this.getComplianceTrainingText(profile)}
                >
              </ProfileRegistrationStepStatus>}
              <ProfileRegistrationStepStatus
                  title='Sign Data User Code Of Conduct'
                  wasBypassed={!!profile.dataUseAgreementBypassTime}
                  incompleteButtonText='Sign'
                  completedButtonText={getRegistrationTasksMap()['dataUserCodeOfConduct'].completedText}
                  isComplete={!!(getRegistrationTasksMap()['dataUserCodeOfConduct'].completionTimestamp(profile))}
                  completeStep={getRegistrationTasksMap()['dataUserCodeOfConduct'].onClick}
                  childrenStyle={{marginLeft: 0}}
                  content={this.getDataUseAgreementText(profile)}
                >
              </ProfileRegistrationStepStatus>
            </div>
            <div style={{marginTop: '1rem', marginLeft: '1rem'}}>

              <div style={styles.title}>Optional Demographics Survey</div>
              <hr style={{...styles.verticalLine}}/>
              <div style={{color: colors.primary, fontSize: '14px'}}>
                <div>Survey Completed</div>
                {/*If a user has created an account, they have, by definition, completed the demographic survey*/}
                <div>{displayDateWithoutHours(profile.demographicSurveyCompletionTime !== null ?
                  profile.demographicSurveyCompletionTime : profile.firstSignInTime)}</div>
                <Button
                  type={'link'}
                  style={styles.updateSurveyButton}
                  onClick={() => {
                    this.setState({showDemographicSurveyModal: true});
                  }}
                  data-test-id={'demographics-survey-button'}
                >Update Survey</Button>
              </div>
            </div>
          </div>
        </FlexRow>
        <div style={{display: 'flex'}}>


          <div style={{display: 'flex', marginBottom: '2rem'}}>
            <Button type='link'
                    onClick={() => this.setState({currentProfile: profile})}
            >
              Cancel
            </Button>
            <TooltipTrigger
              side='top'
              content={!!errors && this.saveProfileErrorMessage(errorMessages)}>
              <Button
                data-test-id='save_profile'
                type='purplePrimary'
                style={{marginLeft: 40}}
                onClick={() => wasReferredFromRenewal()
                  ? this.saveProfileWithRenewal(currentProfile)
                  : this.saveProfile(currentProfile)
                }
                disabled={!!errors || fp.isEqual(profile, currentProfile)}
              >
                Save Profile
              </Button>
            </TooltipTrigger>
          </div>
        </div>
        {showDemographicSurveyModal && <Modal width={850}>
            <DemographicSurvey
                profile={currentProfile}
                onCancelClick={() => {
                  this.setState({showDemographicSurveyModal: false});
                }}
                saveProfile={(profileWithDemoSurvey) => {
                  this.saveProfile(profileWithDemoSurvey);
                  this.setState({showDemographicSurveyModal: false});
                }}
                enableCaptcha={false}
                enablePrevious={false}
                showStepCount={false}
            />
        </Modal>}
      </div>
    </FadeBox>;
    }
  });
