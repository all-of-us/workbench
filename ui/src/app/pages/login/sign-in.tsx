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
  ReactWrapperBase,
  ServerConfigProps,
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
  }
});

// Tracks each major stage in the sign-in / sign-up flow. Most of the steps are related to new
// account creation.
export enum SignInStep {
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

export const SIGNED_OUT_HEADER_IMAGE = '/assets/images/all-of-us-logo.svg';


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

export const createEmptyProfile = (requireInstitutionalVerification: boolean = false): Profile => {
  const profile: Profile = {
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
    demographicSurvey: {},
    degrees: [] as Degree[],
  };

  if (!requireInstitutionalVerification) {
    // initialize the institutional affiliations from the old unverified flow
    // (the new flow does not require it)
    profile.institutionalAffiliations = [
      // We only allow entering a single institutional affiliation from the create account
      // page, so we pre-fill a single empty entry which will be bound to the AccountCreation
      // form.
      {
        institution: undefined,
        nonAcademicAffiliation: undefined,
        role: undefined,
      },
    ];
  }
  return profile;
};

/**
 * The inner / implementation SignIn component. This class should only be rendered via the
 * SignInReact method, which wraps this with the expected higher-order components.
 */
export class SignInReactImpl extends React.Component<SignInProps, SignInState> {

  constructor(props: SignInProps) {
    super(props);
    this.state = {
      currentStep: props.initialStep ? props.initialStep : SignInStep.LANDING,
      invitationKey: null,
      termsOfServiceVersion: null,
      // This defines the profile state for a new user flow. This will get passed to each
      // step component as a prop. When each sub-step completes, it will pass the updated Profile
      // data in its onComplete callback.
      profile: createEmptyProfile(this.props.serverConfig.requireInstitutionalVerification)
    };
  }

  componentDidMount() {
    document.body.style.backgroundColor = colors.light;
    this.props.onInit();
  }

  componentDidUpdate(prevProps: SignInProps, prevState: SignInState, snapshot) {
    if (prevState.currentStep !== this.state.currentStep) {
      window.scrollTo(0, 0);
    }
  }

  /**
   * Creates the appropriate set of steps based on the server-side config.
   *
   * Made visible for ease of unit-testing.
   */
  public getAccountCreationSteps(): Array<SignInStep> {
    const {enableNewAccountCreation, requireInvitationKey} = this.props.serverConfig;

    let steps: Array<SignInStep>;
    if (enableNewAccountCreation) {
      steps = [
        SignInStep.LANDING,
        SignInStep.INVITATION_KEY,
        SignInStep.TERMS_OF_SERVICE,
        SignInStep.ACCOUNT_CREATION,
        SignInStep.DEMOGRAPHIC_SURVEY,
        SignInStep.SUCCESS_PAGE
      ];
    } else {
      steps = [
        SignInStep.LANDING,
        SignInStep.INVITATION_KEY,
        SignInStep.ACCOUNT_CREATION,
        SignInStep.SUCCESS_PAGE
      ];
    }

    if (!requireInvitationKey) {
      steps = fp.remove(step => step === SignInStep.INVITATION_KEY, steps);
    }
    return steps;
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
    switch (currentStep) {
      case SignInStep.LANDING:
        return <LoginReactComponent signIn={this.props.signIn} onCreateAccount={() =>
          this.setState({
            currentStep: this.getNextStep(currentStep)
          })}/>;
      case SignInStep.INVITATION_KEY:
        return <InvitationKey onInvitationKeyVerified={(key: string) => this.setState({
          invitationKey: key,
          currentStep: this.getNextStep(currentStep)
        })}/>;
      case SignInStep.TERMS_OF_SERVICE:
        return <AccountCreationTos
          pdfPath='/assets/documents/terms of service (draft).pdf'
          onComplete={() => this.setState({
            termsOfServiceVersion: 1,
            currentStep: this.getNextStep(currentStep)
          })}/>;
      case SignInStep.ACCOUNT_CREATION:
        return <AccountCreation invitationKey={this.state.invitationKey}
                                profile={this.state.profile}
                                onComplete={(profile: Profile) => this.setState({
                                  profile: profile,
                                  currentStep: this.getNextStep(currentStep)
                                })}/>;
      case SignInStep.DEMOGRAPHIC_SURVEY:
        return <AccountCreationSurvey
          profile={this.state.profile}
          invitationKey={this.state.invitationKey}
          termsOfServiceVersion={this.state.termsOfServiceVersion}
          onComplete={(profile: Profile) => this.setState({
            profile: profile,
            currentStep: this.getNextStep(currentStep)
          })}
          onPreviousClick={(profile: Profile) => this.setState({
            profile: profile,
            currentStep: this.getPreviousStep(currentStep)
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
                  src={SIGNED_OUT_HEADER_IMAGE}/></div>
        {this.renderSignInStep(this.state.currentStep)}
      </FlexColumn>
    </FlexColumn>;
  }
}

export const SignInReact = fp.flow(withServerConfig(), withWindowSize())(SignInReactImpl);

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
