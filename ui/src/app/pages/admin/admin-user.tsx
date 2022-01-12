import * as fp from 'lodash/fp';
import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import validate from 'validate.js';

import { Button } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { SmallHeader } from 'app/components/headers';
import { TextInputWithLabel, Toggle } from 'app/components/inputs';
import { SpinnerOverlay } from 'app/components/spinners';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { hasNewValidProps, isBlank, reactStyles } from 'app/utils';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import {
  checkInstitutionalEmail,
  getEmailValidationErrorMessage,
} from 'app/utils/institutions';
import { MatchParams, serverConfigStore } from 'app/utils/stores';
import {
  CheckEmailResponse,
  InstitutionalRole,
  Profile,
  PublicInstitutionDetails,
} from 'generated/fetch';
import { EgressEventsTable } from './egress-events-table';
import {
  adminGetProfile,
  UserAdminTableLink,
  commonStyles,
  getInitalCreditsUsage,
  InitialCreditsDropdown,
  InstitutionDropdown,
  InstitutionalRoleDropdown,
  InstitutionalRoleOtherTextInput,
  getPublicInstitutionDetails,
  ContactEmailTextInput,
  updateAccountProperties,
  enableSave,
  ErrorsTooltip,
  AccessModuleExpirations,
} from './admin-user-common';

const styles = reactStyles({
  ...commonStyles,
  backgroundColorDark: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.95),
  },
  label: {
    color: colors.primary,
    fontSize: '14px',
    fontWeight: 600,
    paddingLeft: 0,
  },
  textInput: {
    width: '17.5rem',
    opacity: '100%',
    marginLeft: 0,
  },
  textInputContainer: {
    marginTop: '1rem',
  },
});

const MaybeEmailValidationErrorMessage = ({
  updatedProfile,
  verifiedInstitutionOptions,
}) => {
  const selectedInstitution = fp.find(
    (institution) =>
      institution.shortName ===
      updatedProfile?.verifiedInstitutionalAffiliation?.institutionShortName,
    verifiedInstitutionOptions
  );
  return selectedInstitution
    ? getEmailValidationErrorMessage(selectedInstitution)
    : null;
};

interface InitialCreditsProps {
  isAboveLimit: boolean;
  usage: string;
}

const InitialCreditsUsage = ({ isAboveLimit, usage }: InitialCreditsProps) => {
  const inputStyle = isAboveLimit
    ? {
        ...styles.textInput,
        backgroundColor: colorWithWhiteness(colors.danger, 0.95),
        borderColor: colors.danger,
        color: colors.danger,
      }
    : {
        ...styles.textInput,
        ...styles.backgroundColorDark,
        color: colors.disabled,
      };

  return (
    <React.Fragment>
      <TextInputWithLabel
        labelText='Initial credits used'
        value={usage}
        inputId='initial-credits-used'
        disabled={true}
        inputStyle={inputStyle}
        containerStyle={styles.textInputContainer}
      />
      {isAboveLimit && (
        <div style={{ color: colors.danger }}>Update initial credit limit</div>
      )}
    </React.Fragment>
  );
};

interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {}

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

export const AdminUser = withRouter(
  class extends React.Component<Props, State> {
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
      await this.getUserData();
    }

    componentDidUpdate(prevProps: Readonly<Props>) {
      if (
        hasNewValidProps(this.props, prevProps, [
          (p) => p.match.params.usernameWithoutGsuiteDomain,
        ])
      ) {
        this.getUserData();
      }
    }

    async getUserData() {
      try {
        await Promise.all([this.getUser(), this.getInstitutions()]);
      } finally {
        this.setState({ loading: false });
      }
    }

    componentWillUnmount(): void {
      if (this.aborter) {
        this.aborter.abort();
      }
    }

    async validateEmail(contactEmail: string, institutionShortName: string) {
      this.setState({ emailValidationResponse: null });

      // Early-exit with no result if either input is blank.
      if (!institutionShortName || isBlank(contactEmail)) {
        return;
      }

      this.setState({ loading: true });
      // Cancel any outstanding API calls.
      if (this.aborter) {
        this.aborter.abort();
      }
      this.aborter = new AbortController();

      try {
        const result = await checkInstitutionalEmail(
          contactEmail,
          institutionShortName,
          this.aborter
        );
        this.setState({
          emailValidationError: '',
          emailValidationResponse: result,
        });
      } catch (e) {
        this.setState({
          emailValidationError:
            'Error validating user email against institution - please refresh page and try again',
          emailValidationResponse: null,
        });
      }
      this.setState({ loading: false });
    }

    async getUser() {
      const { usernameWithoutGsuiteDomain } = this.props.match.params;
      const { gsuiteDomain } = serverConfigStore.get().config;
      try {
        const profile = await adminGetProfile(
          usernameWithoutGsuiteDomain + '@' + gsuiteDomain
        );
        this.setState({
          oldProfile: profile,
          updatedProfile: profile,
          profileLoadingError: '',
        });
      } catch (error) {
        this.setState({
          profileLoadingError:
            'Could not find user - please check spelling of username and try again',
        });
      }
    }

    usageIsAboveLimit(): boolean {
      const {
        updatedProfile: { freeTierDollarQuota, freeTierUsage },
      } = this.state;
      return freeTierDollarQuota < freeTierUsage;
    }

    async getInstitutions() {
      try {
        this.setState({
          verifiedInstitutionOptions: await getPublicInstitutionDetails(),
        });
      } catch (error) {
        this.setState({
          institutionsLoadingError:
            'Could not get list of verified institutions - please try again later',
        });
      }
    }

    async setVerifiedInstitutionOnProfile(institutionShortName: string) {
      const {
        updatedProfile: { contactEmail },
      } = this.state;
      await this.validateEmail(contactEmail, institutionShortName);

      const { verifiedInstitutionOptions } = this.state;
      this.setState(
        fp.flow(
          fp.set(
            [
              'updatedProfile',
              'verifiedInstitutionalAffiliation',
              'institutionShortName',
            ],
            institutionShortName
          ),
          fp.set(
            [
              'updatedProfile',
              'verifiedInstitutionalAffiliation',
              'institutionDisplayName',
            ],
            verifiedInstitutionOptions.find(
              (institution) => institution.shortName === institutionShortName,
              verifiedInstitutionOptions
            ).displayName
          ),
          fp.set(
            [
              'updatedProfile',
              'verifiedInstitutionalAffiliation',
              'institutionRoleEnum',
            ],
            undefined
          ),
          fp.set(
            [
              'updatedProfile',
              'verifiedInstitutionalAffiliation',
              'institutionalRoleOtherText',
            ],
            undefined
          )
        )
      );
    }

    async setContactEmail(contactEmail: string) {
      const {
        updatedProfile: { verifiedInstitutionalAffiliation },
      } = this.state;
      await this.validateEmail(
        contactEmail,
        verifiedInstitutionalAffiliation?.institutionShortName
      );
      this.setState(fp.set(['updatedProfile', 'contactEmail'], contactEmail));
    }

    setInitialCreditsDollarLimitOverride(newLimit: number) {
      this.setState(
        fp.set(['updatedProfile', 'freeTierDollarQuota'], newLimit)
      );
    }

    setInstitutionalRoleOnProfile(institutionalRoleEnum: InstitutionalRole) {
      this.setState(
        fp.flow(
          fp.set(
            [
              'updatedProfile',
              'verifiedInstitutionalAffiliation',
              'institutionalRoleEnum',
            ],
            institutionalRoleEnum
          ),
          fp.set(
            [
              'updatedProfile',
              'verifiedInstitutionalAffiliation',
              'institutionalRoleOtherText',
            ],
            undefined
          )
        )
      );
    }

    validateCheckEmailResponse() {
      const { emailValidationResponse, emailValidationError } = this.state;

      // if we have never called validateEmail()
      if (!emailValidationResponse && !emailValidationError) {
        return true;
      }

      if (emailValidationResponse) {
        return emailValidationResponse.isValidMember;
      }
      return false;
    }

    validateInstitutionalRoleOtherText(updatedProfile: Profile) {
      return (
        updatedProfile?.verifiedInstitutionalAffiliation
          ?.institutionalRoleEnum !== InstitutionalRole.OTHER ||
        !!updatedProfile?.verifiedInstitutionalAffiliation
          ?.institutionalRoleOtherText
      );
    }

    render() {
      const {
        emailValidationError,
        emailValidationResponse,
        institutionsLoadingError,
        profileLoadingError,
        updatedProfile,
        verifiedInstitutionOptions,
        oldProfile,
      } = this.state;
      const errors = validate(
        {
          contactEmail: !!updatedProfile?.contactEmail,
          verifiedInstitutionalAffiliation:
            !!updatedProfile?.verifiedInstitutionalAffiliation,
          institutionShortName:
            !!updatedProfile?.verifiedInstitutionalAffiliation
              ?.institutionShortName,
          institutionalRoleEnum:
            !!updatedProfile?.verifiedInstitutionalAffiliation
              ?.institutionalRoleEnum,
          institutionalRoleOtherText: !!(
            updatedProfile?.verifiedInstitutionalAffiliation
              ?.institutionalRoleEnum !== InstitutionalRole.OTHER ||
            updatedProfile?.verifiedInstitutionalAffiliation
              ?.institutionalRoleOtherText
          ),
          institutionMembership: this.validateCheckEmailResponse(),
        },
        {
          contactEmail: { truthiness: true },
          verifiedInstitutionalAffiliation: { truthiness: true },
          institutionShortName: { truthiness: true },
          institutionalRoleEnum: { truthiness: true },
          institutionalRoleOtherText: { truthiness: true },
          institutionMembership: { truthiness: true },
        }
      );
      return (
        <FadeBox style={styles.fadeBox}>
          {emailValidationError && <div>{emailValidationError}</div>}
          {institutionsLoadingError && <div>{institutionsLoadingError}</div>}
          {profileLoadingError && <div>{profileLoadingError}</div>}
          {updatedProfile && (
            <FlexColumn>
              <FlexRow style={{ alignItems: 'center' }}>
                <UserAdminTableLink />
                <SmallHeader style={{ marginTop: 0, marginLeft: '0.5rem' }}>
                  User Profile Information
                </SmallHeader>
              </FlexRow>
              <FlexRow
                style={{
                  width: '100%',
                  marginTop: '1rem',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                }}
              >
                <FlexRow
                  style={{
                    alignItems: 'center',
                    backgroundColor: colorWithWhiteness(colors.primary, 0.85),
                    borderRadius: '5px',
                    padding: '0 .5rem',
                    height: '1.625rem',
                    width: '17.5rem',
                  }}
                >
                  <label style={{ fontWeight: 600 }}>Account access</label>
                  <Toggle
                    name={updatedProfile.disabled ? 'Disabled' : 'Enabled'}
                    checked={!updatedProfile.disabled}
                    disabled={true}
                    data-test-id='account-access-toggle'
                    onToggle={() => {}}
                    style={{ marginLeft: 'auto', paddingBottom: '0px' }}
                    height={18}
                    width={33}
                  />
                </FlexRow>
                <ErrorsTooltip errors={errors}>
                  <Button
                    type='primary'
                    disabled={!enableSave(oldProfile, updatedProfile, errors)}
                    onClick={async () => {
                      this.setState({ loading: true });
                      const response = await updateAccountProperties(
                        oldProfile,
                        updatedProfile
                      );
                      this.setState({
                        oldProfile: response,
                        updatedProfile: response,
                        loading: false,
                      });
                    }}
                  >
                    Save
                  </Button>
                </ErrorsTooltip>
              </FlexRow>
              <FlexRow>
                <FlexColumn style={{ width: '33%', marginRight: '1rem' }}>
                  <TextInputWithLabel
                    labelText={'User name'}
                    placeholder={
                      updatedProfile.givenName + ' ' + updatedProfile.familyName
                    }
                    inputId={'userFullName'}
                    disabled={true}
                    inputStyle={{
                      ...styles.textInput,
                      ...styles.backgroundColorDark,
                    }}
                    containerStyle={styles.textInputContainer}
                  />
                  <TextInputWithLabel
                    labelText={'Access tiers'}
                    placeholder={fp.flow(
                      fp.map(fp.capitalize),
                      fp.join(', ')
                    )(updatedProfile.accessTierShortNames)}
                    inputId={'accessTiers'}
                    disabled={true}
                    inputStyle={{
                      ...styles.textInput,
                      ...styles.backgroundColorDark,
                    }}
                    containerStyle={styles.textInputContainer}
                  />
                  <TextInputWithLabel
                    labelText={'Username'}
                    placeholder={updatedProfile.username}
                    inputId={'username'}
                    disabled={true}
                    inputStyle={{
                      ...styles.textInput,
                      ...styles.backgroundColorDark,
                    }}
                    containerStyle={styles.textInputContainer}
                  />
                  <ContactEmailTextInput
                    contactEmail={updatedProfile.contactEmail}
                    onChange={(email) => this.setContactEmail(email)}
                    labelStyle={styles.label}
                    inputStyle={styles.textInput}
                    containerStyle={styles.textInputContainer}
                  />
                  <InitialCreditsUsage
                    isAboveLimit={this.usageIsAboveLimit()}
                    usage={getInitalCreditsUsage(this.state.updatedProfile)}
                  />
                </FlexColumn>
                <FlexColumn style={{ width: '33%' }}>
                  <InitialCreditsDropdown
                    currentLimit={updatedProfile.freeTierDollarQuota}
                    labelStyle={styles.label}
                    dropdownStyle={styles.textInput}
                    onChange={async (event) =>
                      this.setInitialCreditsDollarLimitOverride(event.value)
                    }
                  />
                  <InstitutionDropdown
                    institutions={verifiedInstitutionOptions}
                    currentInstitutionShortName={
                      updatedProfile.verifiedInstitutionalAffiliation
                        ?.institutionShortName
                    }
                    labelStyle={styles.label}
                    dropdownStyle={styles.textInput}
                    onChange={async (event) =>
                      this.setVerifiedInstitutionOnProfile(event.value)
                    }
                  />
                  {emailValidationResponse &&
                    !emailValidationResponse.isValidMember && (
                      <MaybeEmailValidationErrorMessage
                        updatedProfile={updatedProfile}
                        verifiedInstitutionOptions={verifiedInstitutionOptions}
                      />
                    )}
                  <InstitutionalRoleDropdown
                    institutions={verifiedInstitutionOptions}
                    currentAffiliation={
                      updatedProfile.verifiedInstitutionalAffiliation
                    }
                    labelStyle={styles.label}
                    dropdownStyle={styles.textInput}
                    onChange={(event) =>
                      this.setInstitutionalRoleOnProfile(event.value)
                    }
                  />
                  <InstitutionalRoleOtherTextInput
                    affiliation={
                      updatedProfile.verifiedInstitutionalAffiliation
                    }
                    labelStyle={styles.label}
                    inputStyle={styles.textInput}
                    containerStyle={styles.textInputContainer}
                    onChange={(value) =>
                      this.setState(
                        fp.set(
                          [
                            'updatedProfile',
                            'verifiedInstitutionalAffiliation',
                            'institutionalRoleOtherText',
                          ],
                          value
                        )
                      )
                    }
                  />
                  <AccessModuleExpirations profile={updatedProfile} />
                </FlexColumn>
              </FlexRow>
              <FlexRow>
                <h2>Egress event history</h2>
              </FlexRow>
              <FlexRow>
                <EgressEventsTable
                  displayPageSize={10}
                  sourceUserEmail={updatedProfile.username}
                />
              </FlexRow>
            </FlexColumn>
          )}
          {this.state.loading && <SpinnerOverlay />}
        </FadeBox>
      );
    }
  }
);
