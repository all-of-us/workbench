import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { Dropdown } from 'primereact/dropdown';
import validate from 'validate.js';
import { faCircleInfo } from '@fortawesome/pro-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import {
  AccessModule,
  InstitutionalRole,
  Profile,
  PublicInstitutionDetails,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ExclamationTriangle } from 'app/components/icons';
import {
  TextAreaWithLengthValidationMessage,
  TextInput,
  ValidationError,
} from 'app/components/inputs';
import { BulletAlignedUnorderedList } from 'app/components/lists';
import { withErrorModal, withSuccessModal } from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { SpinnerOverlay } from 'app/components/spinners';
import {
  withProfileErrorModal,
  WithProfileErrorModalProps,
} from 'app/components/with-error-modal-wrapper';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { AccountCreationOptions } from 'app/pages/login/account-creation/account-creation-options';
import { styles } from 'app/pages/profile/profile-styles';
import { institutionApi, profileApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { withUserProfile } from 'app/utils';
import {
  DARPageMode,
  DATA_ACCESS_REQUIREMENTS_PATH,
  wasReferredFromRenewal,
} from 'app/utils/access-utils';
import { canRenderSignedDucc } from 'app/utils/code-of-conduct';
import { Country } from 'app/utils/constants';
import { convertAPIError } from 'app/utils/errors';
import { NavigationProps } from 'app/utils/navigation';
import { isUserFromUS } from 'app/utils/profile-utils';
import { canonicalizeUrl } from 'app/utils/urls';
import { notTooLong, required } from 'app/utils/validators';
import { withNavigation } from 'app/utils/with-navigation-hoc';

import { DataAccessPanel } from './data-access-panel';
import { DemographicSurveyPanel } from './demographic-survey-panel';
import { InitialCreditsPanel } from './initial-credits-panel';
import { SignedDuccPanel } from './signed-ducc-panel';

const validators = {
  givenName: { ...required, ...notTooLong(80) },
  familyName: { ...required, ...notTooLong(80) },
  areaOfResearch: { ...required, ...notTooLong(2000) },
  streetAddress1: { ...required, ...notTooLong(95) },
  streetAddress2: notTooLong(95),
  zipCode: { ...required, ...notTooLong(10) },
  city: { ...required, ...notTooLong(95) },
  state: { ...required, ...notTooLong(95) },
};

interface ProfilePageProps
  extends WithProfileErrorModalProps,
    WithSpinnerOverlayProps,
    NavigationProps,
    RouteComponentProps {
  profileState: {
    profile: Profile;
    reload: () => {};
  };
}
interface ProfilePageState {
  currentProfile: Profile;
  institutions: Array<PublicInstitutionDetails>;
  updating: boolean;
}
export const ProfileComponent = fp.flow(
  withUserProfile(),
  withProfileErrorModal,
  withNavigation,
  withRouter
)(
  class extends React.Component<ProfilePageProps, ProfilePageState> {
    constructor(props) {
      super(props);

      this.state = {
        currentProfile: this.initializeProfile(),
        institutions: [],
        updating: false,
      };
    }
    static displayName = 'ProfilePage';

    saveProfileWithRenewal = withSuccessModal(
      {
        title: 'Your profile has been updated',
        message:
          'You will be redirected to the access renewal page upon closing this dialog.',
        onDismiss: () =>
          this.props.navigate([DATA_ACCESS_REQUIREMENTS_PATH], {
            queryParams: {
              pageMode: DARPageMode.ANNUAL_RENEWAL,
            },
          }),
      },
      this.saveProfile.bind(this)
    );

    confirmProfile = fp.flow(
      withSuccessModal({
        title: 'You have confirmed your profile is accurate',
        message:
          'You will be redirected to the access renewal page upon closing this dialog.',
        onDismiss: () =>
          this.props.navigate([DATA_ACCESS_REQUIREMENTS_PATH], {
            queryParams: {
              pageMode: DARPageMode.ANNUAL_RENEWAL,
            },
          }),
      }),
      withErrorModal({
        title: 'Failed To Confirm Profile',
        message:
          'An error occurred trying to confirm your profile. Please try again.',
      })
    )(async () => {
      this.setState({ updating: true });
      await profileApi().confirmProfile();
      this.setState({ updating: false });
    });

    async componentDidMount() {
      this.props.hideSpinner();
      try {
        const { institutions } =
          await institutionApi().getPublicInstitutionDetails();
        this.setState({ institutions });
      } catch (e) {
        // continue regardless of error
      }
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
        };
      }
      return this.props.profileState.profile;
    }

    createInitialProfile(): Profile {
      return {
        ...this.props.profileState.profile,
        demographicSurvey: {},
      };
    }

    componentDidUpdate(prevProps) {
      const {
        profileState: { profile },
      } = this.props;

      if (!fp.isEqual(prevProps.profileState.profile, profile)) {
        this.setState({ currentProfile: profile }); // for when profile loads after component load
      }
    }

    getRoleOptions(): Array<{ label: string; value: InstitutionalRole }> {
      const { institutions, currentProfile } = this.state;
      if (currentProfile) {
        const selectedOrgType = institutions.find(
          (inst) =>
            inst.shortName ===
            currentProfile.verifiedInstitutionalAffiliation.institutionShortName
        );
        if (selectedOrgType) {
          const sel = selectedOrgType.organizationTypeEnum;

          const availableRoles: Array<InstitutionalRole> =
            AccountCreationOptions.institutionalRolesByOrganizationType.find(
              (obj) => obj.type === sel
            ).roles;

          return AccountCreationOptions.institutionalRoleOptions.filter(
            (option) => availableRoles.includes(option.value)
          );
        }
      }
    }

    saveProfileErrorMessage(errors) {
      return (
        <React.Fragment>
          <div>You must correct errors before saving: </div>
          <BulletAlignedUnorderedList>
            {Object.keys(errors).map((key) => (
              <li key={errors[key][0]}>{errors[key][0]}</li>
            ))}
          </BulletAlignedUnorderedList>
        </React.Fragment>
      );
    }

    async saveProfile(profile: Profile): Promise<Profile> {
      const {
        profileState: { reload },
      } = this.props;

      this.setState({ updating: true });

      try {
        await profileApi().updateProfile(profile);
        await reload();
        return profile;
      } catch (error) {
        const errorResponse = await convertAPIError(error);
        this.props.showProfileErrorModal(errorResponse.message);
        console.error(error);
        return Promise.reject();
      } finally {
        this.setState({ updating: false });
      }
    }

    render() {
      const {
        profileState: { profile },
      } = this.props;
      const { currentProfile, updating } = this.state;
      const {
        givenName,
        familyName,
        areaOfResearch,
        professionalUrl,
        address: { streetAddress1, streetAddress2, zipCode, city, state },
      } = currentProfile;

      const profileConfirmationAccessModule = fp.find(
        { moduleName: AccessModule.PROFILECONFIRMATION },
        profile.accessModules.modules
      );
      const hasExpired =
        profileConfirmationAccessModule.expirationEpochMillis &&
        profileConfirmationAccessModule.expirationEpochMillis < Date.now();
      const bypassed = !!profileConfirmationAccessModule.bypassEpochMillis;
      const showRenewalBox =
        (hasExpired && !bypassed) ||
        wasReferredFromRenewal(this.props.location.search);

      // validatejs requires a scheme, which we don't necessarily need in the profile; rather than
      // forking their website regex, just ensure a scheme ahead of validation.
      const urlError = professionalUrl
        ? validate(
            { website: canonicalizeUrl(professionalUrl) },
            {
              website: {
                url: {
                  message: `^Professional URL ${professionalUrl} is not a valid URL`,
                },
              },
            }
          )
        : undefined;
      const errorMessages = {
        ...urlError,
        ...validate(
          {
            givenName,
            familyName,
            areaOfResearch,
            streetAddress1,
            streetAddress2,
            zipCode,
            city,
            state,
          },
          validators,
          {
            prettify: (v) =>
              ({
                givenName: 'First Name',
                familyName: 'Last Name',
                areaOfResearch: 'Current Research',
                state: 'State/Province/Region',
                zipCode: 'Zip/Postal Code',
              }[v] || validate.prettify(v)),
          }
        ),
      };
      const errors = fp.isEmpty(errorMessages) ? undefined : errorMessages;

      const makeProfileInput = ({
        title,
        valueKey,
        isLong = false,
        ...props
      }) => {
        let errorText = profile && errors?.[valueKey];
        if (valueKey && !Array.isArray(valueKey)) {
          valueKey = [valueKey];
        }
        if (valueKey && valueKey.length > 1) {
          errorText = profile && errors?.[valueKey[1]];
        }
        const inputProps = {
          value: fp.get(valueKey, currentProfile) || '',
          onChange: (v) =>
            this.setState(fp.set(['currentProfile', ...valueKey], v)),
          invalid: !!errorText,
          style: props.style,
          maxCharacters: props.maxCharacters,
          ...props,
        };
        const id = props.id || valueKey;

        return (
          <div style={{ marginBottom: 40 }}>
            <div style={styles.inputLabel}>{title}</div>
            {isLong ? (
              <TextAreaWithLengthValidationMessage
                id={id}
                data-test-id={id}
                heightOverride={styles.longInputHeightStyle}
                initialText={inputProps.value}
                maxCharacters={inputProps.maxCharacters}
                {...inputProps}
                textBoxStyleOverrides={{
                  ...styles.longInputContainerStyle,
                  ...inputProps.style,
                }}
              />
            ) : (
              <TooltipTrigger
                content='This field cannot be edited'
                disabled={!props.disabled}
              >
                <div>
                  <TextInput
                    data-test-id={props.id || valueKey}
                    {...inputProps}
                    style={
                      props.disabled
                        ? {
                            ...styles.disabledInput,
                            ...styles.inputStyle,
                            ...inputProps.style,
                          }
                        : { ...styles.inputStyle, ...inputProps.style }
                    }
                  />
                </div>
              </TooltipTrigger>
            )}
            <ValidationError>{errorText}</ValidationError>
          </div>
        );
      };

      /* API returns completion time as a Date object but creates that Date object with a
       * seconds representation instead of a milliseconds representation, so it needs to be adjusted
       * */
      const demographicSurveyV2CompletionTimeMillis = profile
        ?.demographicSurveyV2?.completionTime
        ? new Date(profile.demographicSurveyV2.completionTime).valueOf() * 1000
        : null;

      return (
        <FadeBox style={styles.fadebox}>
          <div style={{ width: '95%' }}>
            {(!profile || updating) && <SpinnerOverlay />}
            <div style={{ ...styles.h1, marginBottom: '1.05rem' }}>Profile</div>
            <FlexRow style={{ justifyContent: 'spaceBetween' }}>
              <div>
                {showRenewalBox && (
                  <div
                    style={styles.renewalBox}
                    data-test-id='profile-confirmation-renewal-box'
                  >
                    <ExclamationTriangle
                      size={25}
                      color={colors.warning}
                      style={{ margin: '0.75rem' }}
                    />
                    <div style={{ color: colors.primary, fontWeight: 600 }}>
                      Please update or verify your profile.
                    </div>
                    <a
                      onClick={() => this.confirmProfile()}
                      style={{
                        margin: '0 0.75rem 0 auto',
                        textDecoration: 'underline',
                      }}
                    >
                      Looks Good
                    </a>
                  </div>
                )}
                <div style={styles.title}>Public displayed Information</div>
                <hr style={{ ...styles.verticalLine, width: '64%' }} />
                <FlexRow style={{ marginTop: '1.5rem' }}>
                  {makeProfileInput({
                    title: 'First Name',
                    valueKey: 'givenName',
                  })}
                  {makeProfileInput({
                    title: 'Last Name',
                    valueKey: 'familyName',
                  })}
                </FlexRow>
                <FlexRow>
                  <FlexColumn>
                    {makeProfileInput({
                      title: 'Your Institution',
                      valueKey: [
                        'verifiedInstitutionalAffiliation',
                        'institutionDisplayName',
                      ],
                      disabled: true,
                    })}
                    {!profile.verifiedInstitutionalAffiliation && (
                      <div style={{ color: colors.danger }}>
                        Institution cannot be empty. Please contact admin.
                      </div>
                    )}
                  </FlexColumn>
                  <FlexColumn>
                    <div style={styles.inputLabel}>Your Role</div>
                    {profile.verifiedInstitutionalAffiliation && (
                      <Dropdown
                        style={{ width: '18.75rem' }}
                        data-test-id='role-dropdown'
                        placeholder='Your Role'
                        options={this.getRoleOptions()}
                        disabled={true}
                        value={
                          currentProfile.verifiedInstitutionalAffiliation
                            .institutionalRoleEnum
                        }
                      />
                    )}

                    {currentProfile.verifiedInstitutionalAffiliation
                      ?.institutionalRoleEnum &&
                      currentProfile.verifiedInstitutionalAffiliation
                        .institutionalRoleEnum === InstitutionalRole.OTHER && (
                        <div>
                          {makeProfileInput({
                            title: '',
                            valueKey: [
                              'verifiedInstitutionalAffiliation',
                              'institutionalRoleOtherText',
                            ],
                            style: { marginTop: '1.5rem' },
                            disabled: true,
                          })}
                        </div>
                      )}
                  </FlexColumn>
                </FlexRow>

                <FlexRow style={{ width: '100%' }}>
                  {makeProfileInput({
                    title: 'Professional URL',
                    valueKey: 'professionalUrl',
                    style: { width: '39rem' },
                  })}
                </FlexRow>
                <FlexRow>
                  {makeProfileInput({
                    title: (
                      <FlexColumn>
                        <div>
                          Your research background, experience and research
                          interests
                        </div>
                        <div style={styles.researchPurposeInfo}>
                          This information will be posted publicly on the{' '}
                          <i>AoU</i> Research Hub Website to inform the{' '}
                          <i>AoU</i> Research Participants.
                        </div>
                      </FlexColumn>
                    ),
                    maxCharacters: 2000,
                    valueKey: 'areaOfResearch',
                    isLong: true,
                    style: { width: '39rem' },
                  })}
                </FlexRow>
                <div style={{ width: '65%', marginTop: '0.75rem' }}>
                  <div style={styles.title}>Private Information</div>
                  <hr style={{ ...styles.verticalLine, width: '39rem' }} />
                  <FlexRow style={{ marginTop: '1.5rem' }}>
                    {makeProfileInput({
                      title: 'User name',
                      valueKey: 'username',
                      disabled: true,
                    })}
                    {makeProfileInput({
                      title: 'Institutional email address',
                      valueKey: 'contactEmail',
                      disabled: true,
                    })}
                  </FlexRow>
                  <FlexRow>
                    {makeProfileInput({
                      title: 'Street address 1',
                      valueKey: ['address', 'streetAddress1'],
                      id: 'streetAddress1',
                    })}
                    {makeProfileInput({
                      title: 'Street address 2',
                      valueKey: ['address', 'streetAddress2'],
                      id: 'streetAddress2',
                    })}
                  </FlexRow>
                  <FlexRow>
                    {makeProfileInput({
                      title: 'City',
                      valueKey: ['address', 'city'],
                      id: 'city',
                    })}
                    {makeProfileInput({
                      title: 'State/Province/Region',
                      valueKey: ['address', 'state'],
                      id: 'state',
                    })}
                  </FlexRow>
                  <FlexRow>
                    {makeProfileInput({
                      title: 'Zip/Postal Code',
                      valueKey: ['address', 'zipCode'],
                      id: 'zipCode',
                    })}
                    {makeProfileInput({
                      title: (
                        <FlexRow style={{ gap: '10px' }}>
                          <label>Country</label>
                          <TooltipTrigger
                            side={'right'}
                            content={
                              'If you need to update your country, email drcsupport@researchallofus.org with your updated country.'
                            }
                          >
                            <FontAwesomeIcon icon={faCircleInfo} />
                          </TooltipTrigger>
                        </FlexRow>
                      ),
                      valueKey: ['address', 'country'],
                      id: 'country',
                      disabled: true,
                    })}
                  </FlexRow>
                </div>
              </div>
              <div style={{ width: '30rem', marginRight: '6rem' }}>
                <div style={{ marginLeft: '1.5rem' }}>
                  <div style={styles.title}>Initial credits balance</div>
                  <hr style={{ ...styles.verticalLine }} />
                  {profile && (
                    <InitialCreditsPanel
                      freeTierUsage={profile.freeTierUsage}
                      freeTierDollarQuota={profile.freeTierDollarQuota}
                    />
                  )}
                </div>
                <DataAccessPanel
                  userAccessTiers={profile.accessTierShortNames}
                />
                {isUserFromUS(profile) && (
                  <DemographicSurveyPanel
                    demographicSurveyCompletionTime={
                      demographicSurveyV2CompletionTimeMillis
                    }
                  />
                )}
                {canRenderSignedDucc(profile.duccSignedVersion) && (
                  <SignedDuccPanel
                    signedDate={profile.duccCompletionTimeEpochMillis}
                  />
                )}
              </div>
            </FlexRow>
            <div style={{ display: 'flex' }}>
              <div style={{ display: 'flex', marginBottom: '3rem' }}>
                <Button
                  type='link'
                  onClick={() => this.setState({ currentProfile: profile })}
                >
                  Cancel
                </Button>
                <TooltipTrigger
                  side='top'
                  content={
                    !!errors && this.saveProfileErrorMessage(errorMessages)
                  }
                >
                  <Button
                    data-test-id='save_profile'
                    type='purplePrimary'
                    style={{ marginLeft: 40 }}
                    onClick={() =>
                      wasReferredFromRenewal(this.props.location.search)
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
          </div>
        </FadeBox>
      );
    }
  }
);
