import {Component} from '@angular/core';
import {Router} from '@angular/router';

import {AccountCreationSuccess} from 'app/pages/login/account-creation/account-creation-success';
import {InvitationKey} from 'app/pages/login/invitation-key';
import {LoginReactComponent} from 'app/pages/login/login';
import {SignInService} from 'app/services/sign-in.service';
import colors from 'app/styles/colors';
import {ReactWrapperBase, withWindowSize} from 'app/utils';
import {AccountCreation} from './account-creation/account-creation';

import {Profile} from 'generated/fetch';

import {FlexColumn} from 'app/components/flex';
import {SignedOutImages, signedOutImages} from 'app/pages/login/signed-out-images';
import * as React from 'react';
import {AccountCreationSurvey} from './account-creation/account-creation-survey';



interface Step {
  stepName: string;
  backgroundImages?: SignedOutImages;
}

export interface SignInProps {
  onInit: () => void;
  signIn: () => void;
  windowSize: { width: number, height: number };
}

interface SignInState {
  currentStep: Step;
  invitationKey: string;
  profile: Profile;
}

const styles = {
  template: (windowSize, images: SignedOutImages) => {
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
      if (images === undefined) {
        return null;
      }
      let imageUrl = 'url(\'' + images.backgroundImgSrc + '\')';
      if (windowSize.width > bgWidthMinPx && windowSize.width <= bgWidthSmallLimitPx) {
        imageUrl = 'url(\'' + images.smallerBackgroundImgSrc + '\')';
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
  signedInContainer: {
    backgroundSize: 'contain',
    backgroundRepeat: 'no-repeat',
    backgroundPosition: 'center',
    display: 'flex',
    justifyContent: 'space-around',
    alignItems: 'flex-start',
    width: 'auto'
  },

};




const headerImg = '/assets/images/logo-registration-non-signed-in.svg';

export const SignInReact = withWindowSize()(
  class extends React.Component<SignInProps, SignInState> {

    constructor(props: SignInProps) {
      super(props);
      this.state = {
        currentStep: {stepName: 'login', backgroundImages: signedOutImages.login},
        invitationKey: '',
        profile: {} as Profile
      };
      this.setProfile = this.setProfile.bind(this);
    }

    componentDidMount() {
      document.body.style.backgroundColor = colors.light;
      this.props.onInit();
    }

    nextDirective(index: string) {
      switch (index) {
        case 'login':
          return <LoginReactComponent signIn={this.props.signIn} onCreateAccount={() =>
            this.setCurrentStep({stepName: 'invitationKey'})}/>;
        case 'invitationKey':
          return <InvitationKey onInvitationKeyVerify={(key) => this.onKeyVerified(key)}/>;
        case 'accountCreation':
          return <AccountCreation invitationKey={this.state.invitationKey} profile={this.state.profile}
                                  setProfile={(profile, nextStep) => this.setProfile(profile, nextStep)}/>;
        case 'accountCreationSurvey':
          return <AccountCreationSurvey profile={this.state.profile}
            invitationKey={this.state.invitationKey} setProfile={(profile, nextStep) => this.setProfile(profile, nextStep)}/>;
        case 'accountCreationSuccess':
          return <AccountCreationSuccess profile={this.state.profile}/>;
        default:
          return;
      }
    }

    setCurrentStep(nextStep: Step) {
      this.setState({
        currentStep: nextStep
      });
    }

    onKeyVerified(invitationKey: string) {
      this.setState({
        invitationKey: invitationKey,
        currentStep: {stepName: 'accountCreation'}
      });
    }

    setProfile(profile, currentStep) {
      this.setState({
        profile: profile,
        currentStep: currentStep
      });
    }



    render() {
      const {stepName, backgroundImages} = this.state.currentStep;
      const maxWidth = backgroundImages === undefined ? '100%' : '41.66667%';
      return <div style={styles.signedInContainer}>
        <FlexColumn style={{width: '100%'}}>
          <div data-test-id='template'
               style={styles.template(this.props.windowSize, backgroundImages)}>
            <img style={{height: '1.75rem', marginLeft: '1rem', marginTop: '1rem'}}
                 src={headerImg}/>
            <div style={{flex: `0 0 ${maxWidth}`,
              maxWidth: maxWidth, minWidth: '25rem'}}>
              {this.nextDirective(stepName)}
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
