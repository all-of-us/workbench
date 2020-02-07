import {Component} from '@angular/core';
import {Router} from '@angular/router';
import * as fp from 'lodash/fp';

import {AccountCreation} from 'app/pages/login/account-creation/account-creation';
import {AccountCreationSuccess} from 'app/pages/login/account-creation/account-creation-success';
import {AccountCreationSurvey} from 'app/pages/login/account-creation/account-creation-survey';
import {AccountCreationTos} from 'app/pages/login/account-creation/account-creation-tos';
import {InvitationKey} from 'app/pages/login/invitation-key';
import {LoginReactComponent} from 'app/pages/login/login';
import {SignInService} from 'app/services/sign-in.service';
import colors from 'app/styles/colors';
import {
  reactStyles,
  ReactWrapperBase, ServerConfigProps,
  WindowSizeProps,
  withServerConfig,
  withWindowSize,
} from 'app/utils';

import {DataAccessLevel, Degree, Profile} from 'generated/fetch';

import {FlexColumn} from 'app/components/flex';
import * as React from 'react';

// A template function which returns the appropriate style config based on window size and
// background images.
const backgroundStyleTemplate = (windowSize, imageConfig?: BackgroundImageConfig) => {
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
  },
});

// Tracks each major stage in the sign-in / sign-up flow. Most of the steps are related to new
// account creation.
enum SignInStep {
  // Landing page. User can choose to sign in or create an account.
  LANDING,
  // Interstitial step, where a user must enter their invitation key.
  //
  // TODO: this needs to be controllable per-environment before beta launch!
  INVITATION_KEY,
  // Terms of Service page. User must read and acknowledge the privacy statement & TOS.
  TERMS_OF_SERVICE,
  // Basic account creation page. User chooses a username and provides basic name / address info.
  ACCOUNT_CREATION,
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

const HEADER_IMAGE = '/assets/images/logo-registration-non-signed-in.svg';

export interface SignInProps extends ServerConfigProps, WindowSizeProps {
  initialStep?: SignInStep;
  onInit: () => void;
  signIn: () => void;
}

interface SignInState {
  currentStep: SignInStep;
  // Tracks the invitation key provided by the user. This is a required parameter in the createUser
  // API call.
  invitationKey: string;
  profile: Profile;
  // Tracks the Terms of Service version that was viewed and acknowledged by the user.
  // This is an optional parameter in the createUser API call.
  termsOfServiceVersion?: number;
}

export const SignInReact = fp.flow(withServerConfig(), withWindowSize())(
  class extends React.Component<SignInProps, SignInState> {

    constructor(props: SignInProps) {
      super(props);
      this.state = {
        currentStep: props.initialStep ? props.initialStep : SignInStep.LANDING,
        invitationKey: null,
        termsOfServiceVersion: null,
        // This defines the profile state for a new user flow. This will get passed to each
        // step component as a prop. When each sub-step completes, it will pass the updated Profile
        // data in its onComplete callback.
        profile: this.createEmptyProfile()
      };
    }

    createEmptyProfile(): Profile {
      return {
        // Note: We abuse the "username" field here by omitting "@domain.org". After
        // profile creation, this field is populated with the full email address.
        username: '',
        dataAccessLevel: DataAccessLevel.Unregistered,
        givenName: '',
        familyName: '',
        contactEmail: '',
        currentPosition: '',
        organization: '',
        areaOfResearch: '',
        address: {
          streetAddress1: '',
          streetAddress2: '',
          city: '',
          state: '',
          country: '',
          zipCode: '',
        },
        institutionalAffiliations: [
          // We only allow entering a single institutional affiliation from the creat account
          // page, so we pre-fill a single entry which will be bound to the form.
          {
            institution: undefined,
            nonAcademicAffiliation: undefined,
            role: undefined,
          },
        ],
        demographicSurvey: {},
        degrees: [] as Degree[],
      };
    }

    componentDidMount() {
      document.body.style.backgroundColor = colors.light;
      this.props.onInit();
    }

    renderSignInStep(currentStep: SignInStep) {
      const {enableNewAccountCreation} = this.props.serverConfig;

      switch (currentStep) {
        case SignInStep.LANDING:
          return <LoginReactComponent signIn={this.props.signIn} onCreateAccount={() =>
            this.setState({
              currentStep: SignInStep.INVITATION_KEY
            })}/>;
        case SignInStep.INVITATION_KEY:
          return <InvitationKey onInvitationKeyVerified={(key: string) => this.setState({
            invitationKey: key,
            // We skip over TERMS_OF_SERVICE if new-style account creation isn't enabled.
            currentStep: enableNewAccountCreation ? SignInStep.TERMS_OF_SERVICE : SignInStep.ACCOUNT_CREATION
          })}/>;
        case SignInStep.TERMS_OF_SERVICE:
          return <AccountCreationTos
            pdfPath='/assets/documents/terms of service (draft).pdf'
            onComplete={() => this.setState({
              termsOfServiceVersion: 1,
              currentStep: SignInStep.ACCOUNT_CREATION
            })}/>;
        case SignInStep.ACCOUNT_CREATION:
          return <AccountCreation invitationKey={this.state.invitationKey}
                                  profile={this.state.profile}
                                  onComplete={(profile: Profile) => this.setState({
                                    profile: profile,
                                    // Skip over the demographic survey if new-style form isn't enabled.
                                    currentStep: enableNewAccountCreation ? SignInStep.DEMOGRAPHIC_SURVEY :
                                      SignInStep.SUCCESS_PAGE
                                  })}/>;
        case SignInStep.DEMOGRAPHIC_SURVEY:
          return <AccountCreationSurvey
            profile={this.state.profile}
            invitationKey={this.state.invitationKey}
            termsOfServiceVersion={this.state.termsOfServiceVersion}
            onComplete={(profile: Profile) => this.setState({
              profile: profile,
              currentStep: SignInStep.SUCCESS_PAGE
            })}
            onPreviousClick={(profile: Profile) => this.setState({
              profile: profile,
              currentStep: SignInStep.ACCOUNT_CREATION
            })}/>;
        case SignInStep.SUCCESS_PAGE:
          return <AccountCreationSuccess profile={this.state.profile}/>;
        default:
          return;
      }
    }

    render() {
      const backgroundImages = StepToImageConfig.get(this.state.currentStep);
      return <FlexColumn style={styles.signInContainer} data-test-id='sign-in-container'>
        <FlexColumn data-test-id='sign-in-page'
             style={backgroundStyleTemplate(this.props.windowSize, backgroundImages)}>
          <div><img style={{height: '1.75rem', marginLeft: '1rem', marginTop: '1rem'}}
                    src={HEADER_IMAGE}/></div>
          {this.renderSignInStep(this.state.currentStep)}
        </FlexColumn>
      </FlexColumn>;
    }
  });

export default SignInReact;

@Component({
  template: '<div #root></div>'
})
export class SignInComponent extends ReactWrapperBase {

  constructor(private signInService: SignInService, private router: Router) {
    super(SignInReact, ['onInit', 'signIn']);
    this.onInit = this.onInit.bind(this);
    this.signIn = this.signIn.bind(this);
  }

  onInit(): void {
    this.signInService.isSignedIn$.subscribe((signedIn) => {
      if (signedIn) {
        this.router.navigateByUrl('/');
      }
    });
  }

  signIn(): void {
    this.signInService.signIn();
  }

}
