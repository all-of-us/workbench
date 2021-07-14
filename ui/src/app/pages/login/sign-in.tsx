import * as fp from 'lodash/fp';

import {PUBLIC_HEADER_IMAGE} from 'app/components/public-layout';
import {AccountCreation} from 'app/pages/login/account-creation/account-creation';
import {AccountCreationSuccess} from 'app/pages/login/account-creation/account-creation-success';
import {AccountCreationSurvey} from 'app/pages/login/account-creation/account-creation-survey';
import {AccountCreationTos} from 'app/pages/login/account-creation/account-creation-tos';
import {LoginReactComponent} from 'app/pages/login/login';
import colors from 'app/styles/colors';
import {
  reactStyles,
  WindowSizeProps,
  withWindowSize,
} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';

import {Degree, Profile} from 'generated/fetch';

import {FlexColumn} from 'app/components/flex';
import {Footer, FooterTypeEnum} from 'app/components/footer';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {AccountCreationInstitution} from 'app/pages/login/account-creation/account-creation-institution';
import {environment} from 'environments/environment';
import * as React from 'react';

// A template function which returns the appropriate style config based on window size and
// background images.
export const backgroundStyleTemplate = (windowSize, imageConfig?: BackgroundImageConfig) => {
  // Lower bounds to prevent the small and large images from covering the
  // creation controls, respectively.
  const bgWidthMinPx = 900;
  const bgWidthSmallLimitPx = 1600;

  return {
    backgroundImage: calculateImage(),
    backgroundColor: colors.light,
    backgroundRepeat: 'no-repeat',
    flex: 1,
    width: '100%',
    backgroundSize: windowSize.width <= bgWidthMinPx ? '0% 0%' : 'contain',
    backgroundPosition: calculateBackgroundPosition()
  };

  function calculateImage() {
    if (!imageConfig) {
      return null;
    }
    let imageUrl = 'url(\'' + imageConfig.backgroundImgSrc + '\')';
    if (windowSize.width > bgWidthMinPx && windowSize.width <= bgWidthSmallLimitPx) {
      imageUrl = 'url(\'' + imageConfig.smallerBackgroundImgSrc + '\')';
    }
    return imageUrl;
  }

  function calculateBackgroundPosition() {
    let position = 'bottom right -1rem';
    if (windowSize.width > bgWidthMinPx && windowSize.width <= bgWidthSmallLimitPx) {
      position = 'bottom right';
    }
    return position;
  }
};

const styles = reactStyles({
  signInContainer: {
    backgroundSize: 'contain',
    backgroundRepeat: 'no-repeat',
    backgroundPosition: 'center',
    justifyContent: 'space-around',
    alignItems: 'flex-start',
    width: 'auto',
    minHeight: '100vh'
  }
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
  SUCCESS_PAGE
}

interface BackgroundImageConfig {
  backgroundImgSrc: string;
  smallerBackgroundImgSrc: string;
}

export const StepToImageConfig: Map<SignInStep, BackgroundImageConfig> = new Map([
  [SignInStep.LANDING, {
    backgroundImgSrc: '/assets/images/login-group.png',
    smallerBackgroundImgSrc: '/assets/images/login-standing.png'
  }],
  [SignInStep.SUCCESS_PAGE, {
    backgroundImgSrc: '/assets/images/congrats-female.png',
    smallerBackgroundImgSrc: 'assets/images/congrats-female-standing.png'
  }]]
);


export interface SignInProps extends WindowSizeProps, WithSpinnerOverlayProps {
  initialStep?: SignInStep;
  onSignIn: () => void;
  signIn: () => void;
}

interface SignInState {
  currentStep: SignInStep;
  profile: Profile;
  // Tracks the Terms of Service version that was viewed and acknowledged by the user.
  // This is an optional parameter in the createUser API call.
  termsOfServiceVersion?: number;
  // Page has been loaded by clicking Previous Button
  isPreviousStep: boolean;
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
    degrees: [] as Degree[],
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
  constructor(props: SignInProps) {
    super(props);
    this.state = {
      currentStep: props.initialStep ? props.initialStep : SignInStep.LANDING,
      termsOfServiceVersion: null,
      // This defines the profile state for a new user flow. This will get passed to each
      // step component as a prop. When each sub-step completes, it will pass the updated Profile
      // data in its onComplete callback.
      profile: createEmptyProfile(),
      isPreviousStep: false
    };
  }

  componentDidMount() {
    this.props.hideSpinner();
    document.body.style.backgroundColor = colors.light;
    this.props.onSignIn();
  }

  componentDidUpdate(prevProps: SignInProps, prevState: SignInState, snapshot) {
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
      SignInStep.SUCCESS_PAGE
    ];
  }

  private getNextStep(currentStep: SignInStep) {
    const steps = this.getAccountCreationSteps();
    const index = steps.indexOf(currentStep);
    if (index === -1) {
      throw new Error('Unexpected sign-in step: ' + currentStep);
    }
    if (index === steps.length) {
      throw new Error('No sign-in steps remaining after step ' + currentStep);
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

  private renderSignInStep(currentStep: SignInStep) {
    const onComplete = (profile: Profile) => {
      this.setState({
        profile: profile,
        currentStep: this.getNextStep(currentStep),
        isPreviousStep: false
      });
    };
    const onPrevious = (profile: Profile) => {
      this.setState({
        profile: profile,
        currentStep: this.getPreviousStep(currentStep),
        isPreviousStep: true
      });
    };

    switch (currentStep) {
      case SignInStep.LANDING:
        return <LoginReactComponent signIn={this.props.signIn} onCreateAccount={async() => {
          AnalyticsTracker.Registration.CreateAccount();
          await this.setState({
            currentStep: this.getNextStep(currentStep)
          });
        }}/>;
      case SignInStep.TERMS_OF_SERVICE:
        return <AccountCreationTos
          filePath='/assets/documents/aou-tos.html'
          onComplete={() => {
            AnalyticsTracker.Registration.TOS();
            this.setState({
              termsOfServiceVersion: 1,
              currentStep: this.getNextStep(currentStep),
              isPreviousStep: false
            });
          }} afterPrev={this.state.isPreviousStep}/>;
      case SignInStep.INSTITUTIONAL_AFFILIATION:
        return <AccountCreationInstitution
          profile={this.state.profile}
          onComplete={onComplete}
          onPreviousClick={onPrevious}/>;
      case SignInStep.ACCOUNT_DETAILS:
        return <AccountCreation profile={this.state.profile}
                                onComplete={onComplete}
                                onPreviousClick={onPrevious}/>;
      case SignInStep.DEMOGRAPHIC_SURVEY:
        return <AccountCreationSurvey
          profile={this.state.profile}
          termsOfServiceVersion={this.state.termsOfServiceVersion}
          onComplete={onComplete}
          onPreviousClick={onPrevious}/>;
      case SignInStep.SUCCESS_PAGE:
        return <AccountCreationSuccess profile={this.state.profile}/>;
      default:
        throw new Error('Unknown sign-in step: ' + currentStep);
    }
  }



  render() {
    const showFooter = environment.enableFooter && this.state.currentStep !== SignInStep.TERMS_OF_SERVICE;
    const backgroundImages = StepToImageConfig.get(this.state.currentStep);
    return <FlexColumn style={styles.signInContainer} data-test-id='sign-in-container'>
      <FlexColumn data-test-id='sign-in-page'
                  style={backgroundStyleTemplate(this.props.windowSize, backgroundImages)}>
        <div><img style={{height: '1.75rem', marginLeft: '1rem', marginTop: '1rem'}}
                  src={PUBLIC_HEADER_IMAGE}/></div>
        {this.renderSignInStep(this.state.currentStep)}
      </FlexColumn>
      {showFooter && <Footer type={FooterTypeEnum.Registration} />}
    </FlexColumn>;
  }
}

export const SignIn = fp.flow(withWindowSize())(SignInImpl);
