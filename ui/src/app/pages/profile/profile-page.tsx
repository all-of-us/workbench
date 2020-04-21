import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import * as React from 'react';
import * as validate from 'validate.js';

import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {TextArea, TextInput, ValidationError} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {getRegistrationTasksMap} from 'app/pages/homepage/registration-dashboard';
import {AccountCreationOptions} from 'app/pages/login/account-creation/account-creation-options';
import {DemographicSurvey} from 'app/pages/profile/demographic-survey';
import {ProfileRegistrationStepStatus} from 'app/pages/profile/profile-registration-step-status';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {institutionApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {convertAPIError, reportError} from 'app/utils/errors';
import {serverConfigStore} from 'app/utils/navigation';
import {environment} from 'environments/environment';
import {ErrorResponse, InstitutionalRole, Profile} from 'generated/fetch';
import {PublicInstitutionDetails} from 'generated/fetch';
import {Dropdown} from 'primereact/dropdown';

const styles = reactStyles({
  h1: {
    color: colors.primary,
    fontSize: 20,
    fontWeight: 500,
    lineHeight: '24px'
  },
  inputLabel: {
    color: colors.primary,
    fontSize: 14,
    fontWeight: 500,
    lineHeight: '18px',
    marginBottom: 6
  },
  inputStyle: {
    width: 300,
    marginRight: 20
  },
  longInputStyle: {
    height: 175, width: 420,
    resize: 'both'
  },
  box: {
    backgroundColor: colors.white,
    borderRadius: 8,
    padding: 21
  },
  title: {
    color: colors.primary,
    fontSize: 16,
    fontWeight: 500,
    width: '40%',
    display: 'inline',
    alignItems: 'flexEnd'
  },
  uneditableProfileElement: {
    paddingLeft: '0.5rem',
    marginRight: 20,
    marginBottom: 20,
    height: '1.5rem',
    color: colors.primary
  },
  fadebox: {
    margin: '1rem 0 0 3%',
    width: '95%',
    padding: '0 0.1rem'
  },
  verticalLine: {
    marginTop: '0.3rem', marginInlineStart: '0rem', width: '64%'
  },
  researchPurposeInfo: {
    fontWeight: 100,
    width: '80%',
    marginTop: '0.5rem',
    marginBottom: '0.3rem'
  },
  freeCreditsBox: {
    borderRadius: '0.4rem',
    height: '3rem',
    marginTop: '0.7rem',
    marginBottom: '1.4rem',
    color: colors.primary,
    backgroundColor: colorWithWhiteness(colors.disabled, 0.7)
  }
});

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

interface ProfilePageProps {
  profileState: {
    profile: Profile;
    reload: () => {};
  };
}

interface ProfilePageState {
  currentProfile: Profile;
  institutions: Array<PublicInstitutionDetails>;
  saveProfileErrorResponse: ErrorResponse;
  showDemographicSurveyModal: boolean;
  updating: boolean;
}

export const ProfilePage = withUserProfile()(class extends React.Component<
    ProfilePageProps,
    ProfilePageState
> {
  static displayName = 'ProfilePage';

  constructor(props) {
    super(props);

    this.state = {
      currentProfile: this.initializeProfile(),
      institutions: [],
      saveProfileErrorResponse: null,
      showDemographicSurveyModal: false,
      updating: false
    };
  }

  async componentDidMount() {
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

  setVerifiedInstitutionRole(newRole) {
    this.setState(fp.set(['currentProfile', 'verifiedInstitutionalAffiliation', 'institutionalRoleEnum'], newRole));

  }

  get saveProfileErrorMessage() {
    if (this.props.profileState.profile && !this.props.profileState.profile.verifiedInstitutionalAffiliation) {
      return 'Institution cannot be empty contact admin';
    }
    return 'You must correct errors before saving.';
  }

  async saveProfile(profile: Profile): Promise<Profile> {
    const {profileState: {reload}} = this.props;

    // updating is only used to control spinner display. If the demographic survey modal
    // is open (and, by extension, it is causing this save), a spinner is being displayed over
    // that modal, so no need to show one here.
    if (!this.state.showDemographicSurveyModal) {
      this.setState({updating: true});
    }

    try {
      await profileApi().updateProfile(profile);
      await reload();
      return profile;
    } catch (error) {
      reportError(error);
      const errorResponse = await convertAPIError(error);
      this.setState({saveProfileErrorResponse: errorResponse});
      console.error(error);
      return Promise.reject();
    } finally {
      this.setState({updating: false});
    }
  }

  render() {
    const {profileState: {profile}} = this.props;
    const {currentProfile, saveProfileErrorResponse, updating, showDemographicSurveyModal} = this.state;
    const {enableComplianceTraining, enableEraCommons, enableDataUseAgreement} =
      serverConfigStore.getValue();
    const {
      givenName, familyName, areaOfResearch,
        address: {
        streetAddress1,
        streetAddress2,
          zipCode,
          city,
            state,
            country
        }
    } = currentProfile;
    const errors = validate({
      givenName,
      familyName, areaOfResearch,
      streetAddress1,
      streetAddress2,
      zipCode,
      city,
      state,
      country
    }, validators, {
      prettify: v => ({
        givenName: 'First Name',
        familyName: 'Last Name',
        areaOfResearch: 'Current Research'
      }[v] || validate.prettify(v))
    });

    // render a float value as US currency, rounded to cents: 255.372793 -> $255.37
    const usdElement = (value: number) => {
      value = value || 0.0;
      if (value < 0.0) {
        return <div style={{fontWeight: 600}}>-${(-value).toFixed(2)}</div>;
      } else {
        return <div style={{fontWeight: 600}}>${(value).toFixed(2)}</div>;
      }
    };

    const makeProfileInput = ({title, valueKey, isLong = false, ...props}) => {
      let errorText = profile && errors && errors[valueKey];
      if (valueKey && Array.isArray(valueKey) && valueKey.length > 1) {
        errorText = profile && errors && errors[valueKey[1]];
      }
      const inputProps = {
        value: fp.get(valueKey, currentProfile) || '',
        onChange: v => this.setState(fp.set(['currentProfile', ...valueKey], v)),
        invalid: !!errorText,
        ...props
      };

      return <div style={{marginBottom: 40}}>
        <div style={styles.inputLabel}>{title}</div>
        {isLong ? <TextArea  data-test-id={props.id || valueKey}
            style={styles.longInputStyle}
            {...inputProps}
          />  :
            <TooltipTrigger content='This field cannot be edited' disabled={!props.disabled}>
          <TextInput  data-test-id={props.id || valueKey}
            style={styles.inputStyle}
            {...inputProps}
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
            <div style={styles.title}>Public displayed Information</div>
            <hr style={styles.verticalLine}/>
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
            <FlexRow style={{height: '6rem'}}>
              <FlexColumn>
                {makeProfileInput({
                  title: 'Your Institution',
                  valueKey: 'verifiedInstitutionalAffiliation.institutionDisplayName',
                  disabled: true
                })}
                {!profile.verifiedInstitutionalAffiliation && <div style={{color: colors.danger}}>
                  Institution cannot be empty. Please contact admin
                </div>}
              </FlexColumn>

              <FlexColumn style={{marginBottom: 40}}>
                <div style={styles.inputLabel}>Your Role</div>
                {profile.verifiedInstitutionalAffiliation &&
                <Dropdown style={{width: '12.5rem'}} data-test-id='role-dropdown'
                          placeholder='Your Role'
                          options={this.getRoleOptions()}
                          onChange={(v) => this.setVerifiedInstitutionRole(v.value)}
                          value={currentProfile.verifiedInstitutionalAffiliation.institutionalRoleEnum}/>}

                {currentProfile.verifiedInstitutionalAffiliation &&
                currentProfile.verifiedInstitutionalAffiliation.institutionalRoleEnum &&
                currentProfile.verifiedInstitutionalAffiliation.institutionalRoleEnum ===
                InstitutionalRole.OTHER && <div>{makeProfileInput({
                  title: '',
                  valueKey: ['verifiedInstitutionalAffiliation', 'institutionalRoleOtherText'],
                  style: {marginTop: '1rem'}
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
                  title: 'Username',
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
          <div style={{width: '16rem', marginRight: '4rem'}}>
            <div style={styles.title}>Free credits balance
            </div>
            <hr style={{...styles.verticalLine, width: '15.7rem'}}/>
            {profile && <FlexRow style={styles.freeCreditsBox}>
              <FlexColumn style={{marginLeft: '0.8rem'}}>
                <div style={{marginTop: '0.4rem'}}><i>All of Us</i> Free credits used:</div>
                <div>Remaining <i>All of Us</i> Free credits:</div>
              </FlexColumn>
              <FlexColumn style={{alignItems: 'flex-end', marginLeft: '1.0rem'}}>
                <div style={{marginTop: '0.4rem'}}>{usdElement(profile.freeTierUsage)}</div>
                {usdElement(profile.freeTierDollarQuota - profile.freeTierUsage)}
              </FlexColumn>
            </FlexRow>}
            <div style={styles.title}>Requirements for All
            </div>
            <hr style={{...styles.verticalLine, width: '15.8rem'}}/>
            <div>
              <ProfileRegistrationStepStatus
                  title='Turn on Google 2-Step Verification'
                  wasBypassed={!!profile.twoFactorAuthBypassTime}
                  incompleteButtonText='Set Up'
                  completedButtonText={getRegistrationTasksMap()['twoFactorAuth'].completedText}
                  completionTimestamp={getRegistrationTasksMap()['twoFactorAuth'].completionTimestamp(profile)}
                  isComplete={!!(getRegistrationTasksMap()['twoFactorAuth'].completionTimestamp(profile))}
                  completeStep={getRegistrationTasksMap()['twoFactorAuth'].onClick}/>


            </div>
            <div>

              {enableEraCommons && <ProfileRegistrationStepStatus
                  title='Connect Your eRA Commons Account'
                  wasBypassed={!!profile.eraCommonsBypassTime}
                  incompleteButtonText='Link'
                  completedButtonText={getRegistrationTasksMap()['eraCommons'].completedText}
                  completionTimestamp={getRegistrationTasksMap()['eraCommons'].completionTimestamp(profile)}
                  isComplete={!!(getRegistrationTasksMap()['eraCommons'].completionTimestamp(profile))}
                  completeStep={getRegistrationTasksMap()['eraCommons'].onClick}>
                <div>
                  {profile.eraCommonsLinkedNihUsername != null && <React.Fragment>
                    <div> Username:</div>
                    <div> {profile.eraCommonsLinkedNihUsername} </div>
                  </React.Fragment>}
                  {profile.eraCommonsLinkExpireTime != null &&
                  //  Firecloud returns eraCommons link expiration as 0 if there is no linked account.
                  profile.eraCommonsLinkExpireTime !== 0
                  && <React.Fragment>
                    <div> Link Expiration:</div>
                    <div>
                      {moment.unix(profile.eraCommonsLinkExpireTime)
                          .format('MMMM Do, YYYY, h:mm:ss A')}
                    </div>
                  </React.Fragment>}
                </div>
              </ProfileRegistrationStepStatus>}

              {enableComplianceTraining && <ProfileRegistrationStepStatus
                  title={<span><i>All of Us</i> Responsible Conduct of Research Training'</span>}
                wasBypassed={!!profile.complianceTrainingBypassTime}
                incompleteButtonText='Access Training'
                completedButtonText={getRegistrationTasksMap()['complianceTraining'].completedText}
                completionTimestamp={getRegistrationTasksMap()['complianceTraining'].completionTimestamp(profile)}
                isComplete={!!(getRegistrationTasksMap()['complianceTraining'].completionTimestamp(profile))}
                completeStep={getRegistrationTasksMap()['complianceTraining'].onClick} />}


              {enableDataUseAgreement && <ProfileRegistrationStepStatus
                  title='Sign Data User Code Of Conduct'
                  wasBypassed={!!profile.dataUseAgreementBypassTime}
                  incompleteButtonText='Sign'
                  completedButtonText={getRegistrationTasksMap()['dataUserCodeOfConduct'].completedText}
                  completionTimestamp={getRegistrationTasksMap()['dataUserCodeOfConduct'].completionTimestamp(profile)}
                  isComplete={!!(getRegistrationTasksMap()['dataUserCodeOfConduct'].completionTimestamp(profile))}
                  completeStep={getRegistrationTasksMap()['dataUserCodeOfConduct'].onClick}
                  childrenStyle={{marginLeft: '0rem'}}>
                {profile.dataUseAgreementCompletionTime != null && <React.Fragment>
                  <div> Agreement Renewal:</div>
                  <div>
                    {moment.unix(profile.dataUseAgreementCompletionTime / 1000)
                        .add(1, 'year')
                        .format('MMMM Do, YYYY')}
                  </div>
                </React.Fragment>}
                <a onClick={getRegistrationTasksMap()['dataUserCodeOfConduct'].onClick}>
                  View current agreement
                </a>
              </ProfileRegistrationStepStatus>}
            </div>
            <div style={{marginTop: '1rem', marginLeft: '1rem'}}>

              <div style={styles.title}>Optional Demographics Survey</div>
              <hr style={{...styles.verticalLine, width: '15.8rem'}}/>
              <Button
                  type={'link'}
                  onClick={() => {
                    this.setState({showDemographicSurveyModal: true});
                  }}
                  data-test-id={'demographics-survey-button'}
              >Update Survey</Button>
            </div>
          </div>
        </FlexRow>
        <div style={{display: 'flex'}}>


          <div style={{display: 'flex', marginBottom: '2rem'}}>
            <Button type='link'
                    onClick={() => this.setState({currentProfile: profile})}
            >
              Discard Changes
            </Button>
            <TooltipTrigger
                side='top'
                content={(!!errors || !profile.verifiedInstitutionalAffiliation) && this.saveProfileErrorMessage}>
              <Button
                  data-test-id='save_profile'
                  type='purplePrimary'
                  style={{marginLeft: 40}}
                  onClick={() => this.saveProfile(currentProfile)}
                  disabled={!!errors || fp.isEqual(profile, currentProfile) || !profile.verifiedInstitutionalAffiliation}
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
              onSubmit={async(profileWithUpdatedDemographicSurvey, captchaToken) => {
                const savedProfile = await this.saveProfile(profileWithUpdatedDemographicSurvey);
                this.setState({showDemographicSurveyModal: false});
                return savedProfile;
              }}
              enableCaptcha={false}
              enablePrevious={false}
          />
        </Modal>}
        {saveProfileErrorResponse &&
        <Modal data-test-id='update-profile-error'>
          <ModalTitle>Error creating account</ModalTitle>
          <ModalBody>
            <div>An error occurred while updating your profile. The following message was
              returned:
            </div>
            <div style={{marginTop: '1rem', marginBottom: '1rem'}}>
              "{saveProfileErrorResponse.message}"
            </div>
            <div>
              Please try again or contact <a
                href='mailto:support@researchallofus.org'>support@researchallofus.org</a>.
            </div>
          </ModalBody>
          <ModalFooter>
            <Button onClick={() => this.setState({saveProfileErrorResponse: null})}
                    type='primary'>Close</Button>
          </ModalFooter>
        </Modal>
        }
      </div></FadeBox>;
  }
});

@Component({
  template: '<div #root></div>'
})
export class ProfilePageComponent extends ReactWrapperBase {
  constructor() {
    super(ProfilePage, []);
  }
}
