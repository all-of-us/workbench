import {Component} from '@angular/core';
import {Router} from '@angular/router';

import {AccountCreationSuccess} from 'app/pages/login/account-creation/account-creation-success';
import {InvitationKey} from 'app/pages/login/invitation-key';
import {LoginReactComponent} from 'app/pages/login/login';
import {SignInService} from 'app/services/sign-in.service';
import colors from 'app/styles/colors';
import {ReactWrapperBase, withWindowSize} from 'app/utils';
import {AccountCreation} from './account-creation/account-creation';

import {DataAccessLevel, Degree, Profile} from 'generated/fetch';

import {FlexColumn} from 'app/components/flex';
import * as React from 'react';
import {AccountCreationSurvey} from './account-creation/account-creation-survey';

const styles = {
  template: (windowSize, imageConfig?: BackgroundImageConfig) => {
    // Lower bounds to prevent the small and large images from covering the
    // creation controls, respectively.
    const bgWidthMinPx = 900;
    const bgWidthSmallLimitPx = 1600;

    return {
      backgroundImage: calculateImage(),
      backgroundColor: colors.light,
      backgroundRepeat: 'no-repeat',
      width: '100%',
      minHeight: '100vh',
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
  },
  signInContainer: {
    backgroundSize: 'contain',
    backgroundRepeat: 'no-repeat',
    backgroundPosition: 'center',
    display: 'flex',
    justifyContent: 'space-around',
    alignItems: 'flex-start',
    width: 'auto'
  },

};

enum SignInStep {
  LANDING,
  INVITATION_KEY,
  ACCOUNT_CREATION,
  DEMOGRAPHIC_SURVEY,
  SUCCESS_PAGE
}

interface BackgroundImageConfig {
  backgroundImgSrc: string;
  smallerBackgroundImgSrc: string;
}

const STEP_TO_IMAGE_CONFIG = {
  [SignInStep.LANDING]: {
    backgroundImgSrc: '/assets/images/login-group.png',
    smallerBackgroundImgSrc: '/assets/images/login-standing.png'
  },
  [SignInStep.SUCCESS_PAGE]: {
    backgroundImgSrc: '/assets/images/congrats-female.png',
    smallerBackgroundImgSrc: 'assets/images/congrats-female-standing.png'
  }
};

const HEADER_IMAGE = '/assets/images/logo-registration-non-signed-in.svg';

export interface SignInProps {
  onInit: () => void;
  signIn: () => void;
  windowSize: { width: number, height: number };
}

interface SignInState {
  currentStep: SignInStep;
  invitationKey: string;
  profile: Profile;
  tosVersion?: number;
}

export const SignInReact = withWindowSize()(
  class extends React.Component<SignInProps, SignInState> {

    constructor(props: SignInProps) {
      super(props);
      this.state = {
        currentStep: SignInStep.LANDING,
        invitationKey: null,
        profile: {
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
        },
        tosVersion: null
      };
    }

    componentDidMount() {
      document.body.style.backgroundColor = colors.light;
      this.props.onInit();
    }

    renderSignInStep(currentStep: SignInStep) {
      switch (currentStep) {
        case SignInStep.LANDING:
          return <LoginReactComponent signIn={this.props.signIn} onCreateAccount={() =>
            this.setState({
              currentStep: SignInStep.INVITATION_KEY
            })}/>;
        case SignInStep.INVITATION_KEY:
          return <InvitationKey onInvitationKeyVerified={(key: string) => this.setState({
            invitationKey: key,
            currentStep: SignInStep.ACCOUNT_CREATION
          })}/>;
        case SignInStep.ACCOUNT_CREATION:
          return <AccountCreation invitationKey={this.state.invitationKey} profile={this.state.profile}
                                  onComplete={(profile: Profile) => this.setState({
                                    profile: profile,
                                    currentStep: SignInStep.DEMOGRAPHIC_SURVEY
                                  })}/>;
        case SignInStep.DEMOGRAPHIC_SURVEY:
          return <AccountCreationSurvey
            profile={this.state.profile}
            invitationKey={this.state.invitationKey}
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
      const backgroundImages = STEP_TO_IMAGE_CONFIG[this.state.currentStep];

      const maxWidth = backgroundImages === undefined ? '100%' : '41.66667%';
      return <div style={styles.signInContainer} data-test-id={'sign-in-container'}>
        <FlexColumn style={{width: '100%'}}>
          <div data-test-id='sign'
               style={styles.template(this.props.windowSize, backgroundImages)}>
            <img style={{height: '1.75rem', marginLeft: '1rem', marginTop: '1rem'}}
                 src={HEADER_IMAGE}/>
            <div style={{flex: `0 0 ${maxWidth}`,
              maxWidth: maxWidth, minWidth: '25rem'}}>
              {this.renderSignInStep(this.state.currentStep)}
            </div>
          </div>
        </FlexColumn>
      </div>;
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
