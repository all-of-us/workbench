import * as React from 'react';
import ReCAPTCHA from 'react-google-recaptcha';
import * as fp from 'lodash/fp';

import { Degree, Profile } from 'generated/fetch';

import { environment } from 'environments/environment';
import { Button } from 'app/components/buttons';
import { DemographicSurvey } from 'app/components/demographic-survey-v2';
import { DemographicSurveyValidationMessage } from 'app/components/demographic-survey-v2-validation-message';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Footer, FooterTypeEnum } from 'app/components/footer';
import { PublicAouHeaderWithDisplayTag } from 'app/components/headers';
import { TooltipTrigger } from 'app/components/popups';
import { TermsOfService } from 'app/components/terms-of-service';
import {
  withProfileErrorModal,
  WithProfileErrorModalProps,
} from 'app/components/with-error-modal-wrapper';
import { withNewUserSatisfactionSurveyModal } from 'app/components/with-new-user-satisfaction-survey-modal-wrapper';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { AccountCreation } from 'app/pages/login/account-creation/account-creation';
import { AccountCreationInstitution } from 'app/pages/login/account-creation/account-creation-institution';
import { AccountCreationSuccess } from 'app/pages/login/account-creation/account-creation-success';
import { LoginReactComponent } from 'app/pages/login/login';
import { profileApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles, WindowSizeProps, withWindowSize } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { convertAPIError } from 'app/utils/errors';
import { restrictDemographicSurvey } from 'app/utils/profile-utils';
import { serverConfigStore } from 'app/utils/stores';
import successBackgroundImage from 'assets/images/congrats-female.png';
import successSmallerBackgroundImage from 'assets/images/congrats-female-standing.png';
import landingBackgroundImage from 'assets/images/login-group.png';
import landingSmallerBackgroundImage from 'assets/images/login-standing.png';

// A template function which returns the appropriate style config based on window size and
// background images.
export const backgroundStyleTemplate = (
  windowSize,
  imageConfig?: BackgroundImageConfig
) => {
  // Lower bounds to prevent the small and large images from covering the
  // creation controls, respectively.
  const bgWidthMinPx = 900;
  const bgWidthSmallLimitPx = 1600;

  function calculateImage() {
    if (!imageConfig) {
      return null;
    }
    let imageUrl = "url('" + imageConfig.backgroundImgSrc + "')";
    if (
      windowSize.width > bgWidthMinPx &&
      windowSize.width <= bgWidthSmallLimitPx
    ) {
      imageUrl = "url('" + imageConfig.smallerBackgroundImgSrc + "')";
    }
    return imageUrl;
  }

  function calculateBackgroundPosition() {
    let position = 'bottom right -1.5rem';
    if (
      windowSize.width > bgWidthMinPx &&
      windowSize.width <= bgWidthSmallLimitPx
    ) {
      position = 'bottom right';
    }
    return position;
  }

  return {
    backgroundImage: calculateImage(),
    backgroundColor: colors.light,
    backgroundRepeat: 'no-repeat',
    flex: 1,
    width: '100%',
    backgroundSize: windowSize.width <= bgWidthMinPx ? '0% 0%' : 'contain',
    backgroundPosition: calculateBackgroundPosition(),
  };
};

const styles = reactStyles({
  signInContainer: {
    backgroundSize: 'contain',
    backgroundRepeat: 'no-repeat',
    backgroundPosition: 'center',
    justifyContent: 'space-around',
    alignItems: 'flex-start',
    width: 'auto',
    minHeight: '100vh',
  },
});

// Tracks each major stage in the sign-in / sign-up flow. Most of the steps are related to new
// account creation.
export enum SignInStep {
  // Landing page. User can choose to sign in or create an account.
  LANDING,
  // Terms of Service page. User must read and acknowledge the privacy statement & TOS.
  TERMS_OF_SERVICE,
  // Institutional affiliation step. Includes institution drop-down and contact email.
  INSTITUTIONAL_AFFILIATION,
  // Basic account creation page. User chooses a username and provides basic name / address info.
  ACCOUNT_DETAILS,
  // Optional demographic survey. Completion of this step triggers actual user creation.
  DEMOGRAPHIC_SURVEY,
  // Account creation success page.
  SUCCESS_PAGE,
}

interface BackgroundImageConfig {
  backgroundImgSrc: string;
  smallerBackgroundImgSrc: string;
}

export const StepToImageConfig: Map<SignInStep, BackgroundImageConfig> =
  new Map([
    [
      SignInStep.LANDING,
      {
        backgroundImgSrc: landingBackgroundImage,
        smallerBackgroundImgSrc: landingSmallerBackgroundImage,
      },
    ],
    [
      SignInStep.SUCCESS_PAGE,
      {
        backgroundImgSrc: successBackgroundImage,
        smallerBackgroundImgSrc: successSmallerBackgroundImage,
      },
    ],
  ]);

export interface SignInProps
  extends WindowSizeProps,
    WithSpinnerOverlayProps,
    WithProfileErrorModalProps {
  initialStep?: SignInStep;
}

interface SignInState {
  captcha: boolean;
  captchaToken: string;
  currentStep: SignInStep;
  loading: boolean;
  profile: Profile;
  // Tracks the Terms of Service version that was viewed and acknowledged by the user.
  // This is an optional parameter in the createUser API call.
  termsOfServiceVersion?: number;
  // Page has been loaded by clicking Previous Button
  isPreviousStep: boolean;
  // Validation errors
  errors: any;
}

export const createEmptyProfile = (): Profile => {
  const profile: Profile = {
    // Note: We abuse the "username" field here by omitting "@domain.org". After
    // profile creation, this field is populated with the full email address.
    username: '',
    accessTierShortNames: [],
    givenName: '',
    familyName: '',
    contactEmail: '',
    areaOfResearch: '',
    address: {
      streetAddress1: '',
      streetAddress2: '',
      city: '',
      state: '',
      country: '',
      zipCode: '',
    },
    demographicSurvey: {},
    demographicSurveyV2: {
      education: null,
      ethnicityAiAnOtherText: null,
      ethnicityAsianOtherText: null,
      ethnicCategories: [],
      ethnicityOtherText: null,
      disabilityConcentrating: null,
      disabilityDressing: null,
      disabilityErrands: null,
      disabilityHearing: null,
      disabilityOtherText: null,
      disabilitySeeing: null,
      disabilityWalking: null,
      disadvantaged: null,
      genderIdentities: [],
      genderOtherText: null,
      orientationOtherText: null,
      sexAtBirth: null,
      sexAtBirthOtherText: null,
      sexualOrientations: [],
      yearOfBirth: null,
      yearOfBirthPreferNot: false,
    },
    degrees: [] as Degree[],
    generalDiscoverySources: [],
    partnerDiscoverySources: [],
  };

  profile.verifiedInstitutionalAffiliation = {
    institutionShortName: undefined,
    institutionDisplayName: undefined,
    institutionalRoleEnum: undefined,
    institutionalRoleOtherText: undefined,
  };

  return profile;
};

/**
 * The inner / implementation SignIn component. This class should only be rendered via the
 * SignInReact method, which wraps this with the expected higher-order components.
 *
 * This impl is separated out for testing purposes.
 */

export class SignInImpl extends React.Component<SignInProps, SignInState> {
  private captchaRef = React.createRef<ReCAPTCHA>();
  constructor(props: SignInProps) {
    super(props);
    this.state = {
      captcha: false,
      captchaToken: null,
      currentStep: props.initialStep ? props.initialStep : SignInStep.LANDING,
      loading: false,
      termsOfServiceVersion: null,
      // This defines the profile state for a new user flow. This will get passed to each
      // step component as a prop. When each sub-step completes, it will pass the updated Profile
      // data in its onComplete callback.
      profile: createEmptyProfile(),
      isPreviousStep: false,
      errors: null,
    };
  }

  componentDidMount() {
    this.props.hideSpinner();
    document.body.style.backgroundColor = colors.light;
  }

  componentDidUpdate(prevProps: SignInProps, prevState: SignInState) {
    if (prevState.currentStep !== this.state.currentStep) {
      window.scrollTo(0, 0);
    }
  }

  /**
   * Made visible for ease of unit-testing.
   */
  public getAccountCreationSteps(): Array<SignInStep> {
    return [
      SignInStep.LANDING,
      SignInStep.TERMS_OF_SERVICE,
      SignInStep.INSTITUTIONAL_AFFILIATION,
      SignInStep.ACCOUNT_DETAILS,
      SignInStep.DEMOGRAPHIC_SURVEY,
      SignInStep.SUCCESS_PAGE,
    ];
  }

  private getNextStep(currentStep: SignInStep, profile: Profile = null) {
    const steps = this.getAccountCreationSteps();
    const index = steps.indexOf(currentStep);
    if (index === -1) {
      throw new Error('Unexpected sign-in step: ' + currentStep);
    }
    if (index === steps.length) {
      throw new Error('No sign-in steps remaining after step ' + currentStep);
    }
    if (
      steps[index] === SignInStep.ACCOUNT_DETAILS &&
      restrictDemographicSurvey(profile.address.country, new Date())
    ) {
      return SignInStep.SUCCESS_PAGE;
    }
    return steps[index + 1];
  }

  private getPreviousStep(currentStep: SignInStep) {
    const steps = this.getAccountCreationSteps();
    const index = steps.indexOf(currentStep);
    if (index === -1) {
      throw new Error('Unexpected sign-in step: ' + currentStep);
    }
    if (index === 0) {
      throw new Error('No sign-in steps before step ' + currentStep);
    }
    return steps[index - 1];
  }

  // TODO: Move Previous, Next, and Submit buttons out of each of the
  // steps and into this component
  render() {
    const showFooter = this.state.currentStep !== SignInStep.TERMS_OF_SERVICE;
    const backgroundImages = StepToImageConfig.get(this.state.currentStep);
    return (
      <FlexColumn
        style={styles.signInContainer}
        data-test-id='sign-in-container'
      >
        <FlexColumn
          data-test-id='sign-in-page'
          style={backgroundStyleTemplate(
            this.props.windowSize,
            backgroundImages
          )}
        >
          <PublicAouHeaderWithDisplayTag />
          {this.renderSignInStep(this.state.currentStep)}
        </FlexColumn>
        {this.renderNavigation(this.state.currentStep)}
        {showFooter && <Footer type={FooterTypeEnum.Registration} />}
      </FlexColumn>
    );
  }

  private renderSignInStep(currentStep: SignInStep) {
    const onComplete = (profile: Profile) => {
      this.setState({
        profile: profile,
        currentStep: this.getNextStep(currentStep, profile),
        isPreviousStep: false,
      });
    };
    const onPrevious = (profile: Profile) => {
      this.setState({
        profile: profile,
        currentStep: this.getPreviousStep(currentStep),
        isPreviousStep: true,
      });
    };
    const handleUpdate = (updatedProfile) => {
      this.setState(updatedProfile);
    };

    switch (currentStep) {
      case SignInStep.LANDING:
        return (
          <LoginReactComponent
            onCreateAccount={async () => {
              AnalyticsTracker.Registration.CreateAccount();
              await this.setState({
                currentStep: this.getNextStep(currentStep),
              });
            }}
          />
        );
      case SignInStep.TERMS_OF_SERVICE:
        return (
          <TermsOfService
            showReAcceptNotification={false}
            filePath='/aou-tos.html'
            onComplete={(tosVersion) => {
              AnalyticsTracker.Registration.TOS();
              this.setState({
                termsOfServiceVersion: tosVersion,
                currentStep: this.getNextStep(currentStep),
                isPreviousStep: false,
              });
            }}
            afterPrev={this.state.isPreviousStep}
          />
        );
      case SignInStep.INSTITUTIONAL_AFFILIATION:
        return (
          <AccountCreationInstitution
            profile={this.state.profile}
            onComplete={onComplete}
            onPreviousClick={onPrevious}
          />
        );
      case SignInStep.ACCOUNT_DETAILS:
        return (
          <AccountCreation
            profile={this.state.profile}
            onComplete={onComplete}
            onPreviousClick={onPrevious}
            captchaRef={this.captchaRef}
            captureCaptchaResponse={this.captureCaptchaResponse}
            onSubmit={this.updateProfileAndCreateAccount}
          />
        );
      case SignInStep.DEMOGRAPHIC_SURVEY:
        return (
          <div
            style={{
              marginTop: '1.5rem',
              paddingLeft: '1.5rem',
              width: '48rem',
            }}
          >
            <DemographicSurvey
              onError={(value) => handleUpdate(fp.set(['errors'], value))}
              onUpdate={(prop, value) =>
                handleUpdate(
                  fp.set(['profile', 'demographicSurveyV2', prop], value)
                )
              }
              profile={this.state.profile}
            />
          </div>
        );
      case SignInStep.SUCCESS_PAGE:
        return <AccountCreationSuccess profile={this.state.profile} />;
      default:
        throw new Error('Unknown sign-in step: ' + currentStep);
    }
  }

  // This method is being used if user is international
  private updateProfileAndCreateAccount = (profile: Profile) => {
    this.setState({ profile: profile });
    // setState is flaky, sometimes it is unable to set profile by the time submit happens
    // hence to be sure lets just pass profile to submit method
    this.onSubmit(profile);
  };
  private onSubmit = async (profileArg) => {
    const { enableCaptcha } = serverConfigStore.get().config;
    this.setState({
      loading: true,
    });
    this.props.showSpinner();

    const profileToSubmit =
      !!profileArg && profileArg.address ? profileArg : this.state.profile;

    try {
      const newProfile = await profileApi().createAccount({
        profile: profileToSubmit,
        captchaVerificationToken: this.state.captchaToken,
        termsOfServiceVersion: this.state.termsOfServiceVersion,
      });

      this.setState({
        profile: newProfile,
        currentStep: this.getNextStep(this.state.currentStep, newProfile),
        loading: false,
        isPreviousStep: false,
      });
    } catch (error) {
      const { message } = await convertAPIError(error);
      this.props.showProfileErrorModal(message);
      if (enableCaptcha) {
        // Reset captcha
        this.captchaRef.current.reset();
        this.setState({ captchaToken: null, captcha: true, loading: false });
      }
    }

    this.props.hideSpinner();
  };

  private captureCaptchaResponse(token) {
    this.setState({ captchaToken: token, captcha: true });
    window.dispatchEvent(new Event('captcha-solved'));
  }

  private renderNavigation(currentStep: SignInStep) {
    if (currentStep === SignInStep.DEMOGRAPHIC_SURVEY) {
      const { enableCaptcha } = serverConfigStore.get().config;
      const { captcha, errors, loading } = this.state;
      const invalid = !!errors || (enableCaptcha && !captcha);
      return (
        <div
          style={{
            marginTop: '3rem',
            marginBottom: '1.5rem',
            marginLeft: '1.5rem',
          }}
        >
          {enableCaptcha && (
            <div style={{ paddingBottom: '1.5rem' }}>
              <ReCAPTCHA
                sitekey={environment.captchaSiteKey}
                ref={this.captchaRef}
                onChange={(value) => this.captureCaptchaResponse(value)}
              />
            </div>
          )}
          <FlexRow>
            <Button
              aria-label='Previous'
              type='secondary'
              style={{ marginRight: '1.5rem' }}
              onClick={() => {
                this.setState({
                  currentStep: this.getPreviousStep(currentStep),
                  isPreviousStep: true,
                });
              }}
            >
              Previous
            </Button>
            <TooltipTrigger
              content={
                invalid && (
                  <DemographicSurveyValidationMessage
                    {...{ captcha, errors }}
                    isAccountCreation
                  />
                )
              }
            >
              <Button
                aria-label='Submit'
                disabled={invalid || loading}
                type='primary'
                data-test-id='submit-button'
                onClick={this.onSubmit(this.state.profile)}
              >
                Submit
              </Button>
            </TooltipTrigger>
          </FlexRow>
        </div>
      );
    }

    return <></>;
  }
}

export const SignIn = fp.flow(
  withWindowSize(),
  withProfileErrorModal,
  withNewUserSatisfactionSurveyModal
)(SignInImpl);
