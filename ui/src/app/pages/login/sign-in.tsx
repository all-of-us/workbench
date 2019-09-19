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

import * as React from 'react';

export interface SignInProps {
  onInit: () => void;
  signIn: () => void;
  windowSize: { width: number, height: number };
}

interface SignInState {
  currentStep: string;
  invitationKey: string;
  profile: Profile;
}

const styles = {
  template: (windowSize, images) => {
    return {
      backgroundImage: calculateImage(),
      backgroundColor: colors.light,
      backgroundRepeat: 'no-repeat',
      width: '100%',
      minHeight: '100vh',
      backgroundSize: windowSize.width <= 900 ? '0% 0%' : 'contain',
      backgroundPosition: calculateBackgroundPosition()
    };

    function calculateImage() {
      let imageUrl = 'url(\'' + images.backgroundImgSrc + '\')';
      if (windowSize.width > 900 && windowSize.width <= 1300) {
        imageUrl = 'url(\'' + images.smallerBackgroundImgSrc + '\')';
      }
      return imageUrl;
    }

    function calculateBackgroundPosition() {
      let position = 'bottom right -1rem';
      if (windowSize.width > 900 && windowSize.width <= 1300) {
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


export const pageImages = {
  'login': {
    backgroundImgSrc: '/assets/images/login-group.png',
    smallerBackgroundImgSrc: '/assets/images/login-standing.png'
  },
  'invitationKey': {
    backgroundImgSrc: '/assets/images/invitation-female.png',
    smallerBackgroundImgSrc: '/assets/images/invitation-female-standing.png'
  },
  'accountCreation': {
    backgroundImgSrc: '/assets/images/create-account-male.png',
    smallerBackgroundImgSrc: '/assets/images/create-account-male-standing.png'
  },
  'accountCreationSuccess': {
    backgroundImgSrc: '/assets/images/congrats-female.png',
    smallerBackgroundImgSrc: 'assets/images/congrats-female-standing.png'
  }
};

const headerImg = '/assets/images/logo-registration-non-signed-in.svg';

export const SignInReact = withWindowSize()(
  class extends React.Component<SignInProps, SignInState> {

    constructor(props: SignInProps) {
      super(props);
      this.state = {
        currentStep: 'login',
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
            this.setCurrentStep('invitationKey')}/>;
        case 'invitationKey':
          return <InvitationKey onInvitationKeyVerify={(key) => this.onKeyVerified(key)}/>;
        case 'accountCreation':
          return <AccountCreation invitationKey={this.state.invitationKey}
                                  setProfile={this.setProfile}/>;
        case 'accountCreationSuccess':
          return <AccountCreationSuccess profile={this.state.profile}/>;
        default:
          return;
      }
    }

    setCurrentStep(nextStep: string) {
      this.setState({
        currentStep: nextStep
      });
    }

    onKeyVerified(invitationKey: string) {
      this.setState({
        invitationKey: invitationKey,
        currentStep: 'accountCreation'
      });
    }

    setProfile(profile) {
      this.setState({
        profile: profile,
        currentStep: 'accountCreationSuccess'
      });
    }

    render() {
      return <div style={styles.signedInContainer}>
        <div style={{width: '100%', display: 'flex', flexDirection: 'column'}}>
          <div data-test-id='template'
               style={styles.template(this.props.windowSize, pageImages[this.state.currentStep])}>
            <img style={{height: '1.75rem', marginLeft: '1rem', marginTop: '1rem'}}
                 src={headerImg}/>
            <div style={{flex: '0 0 41.66667%', maxWidth: '41.66667%', minWidth: '25rem'}}>
              {this.nextDirective(this.state.currentStep)}
            </div>
          </div>
        </div>
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
