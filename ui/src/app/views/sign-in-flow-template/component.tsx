import {Component, Inject, OnChanges, OnInit} from '@angular/core';
import {Router} from '@angular/router';

import {SignInService} from 'app/services/sign-in.service';
import {withWindowSize} from 'app/utils';
import {AccountCreationSuccess} from 'app/views/account-creation-success/component';
import {AccountCreation} from 'app/views/account-creation/component';
import {InvitationKeyReact} from 'app/views/invitation-key/component';
import {LoginReactComponent} from 'app/views/login/component';

import * as React from 'react';

import {ReactWrapperBase} from 'app/utils';
import {styles} from './style';

import {Profile} from 'generated/fetch';

interface ImagesSource {
  backgroundImgSrc: string;
  smallerBackgroundImgSrc: string;
}

export interface PageTemplateProps {
  onInit: () => void;
  signIn: () => void;
  windowSize: { width: number, height: number };
}

interface PageTemplateState {
  currentStep: string;
  invitationKey: string;
  profile: Profile;
}

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
  }};

const headerImg = '/assets/images/logo-registration-non-signed-in.svg';

export const RegistrationPageTemplateReact = withWindowSize()(
  class extends React.Component<PageTemplateProps, PageTemplateState> {

    constructor(props: PageTemplateProps) {
      super(props);
      this.state = {
        currentStep: 'login',
        invitationKey: '',
        profile: {} as Profile
      };
      this.setCurrentStep = this.setCurrentStep.bind(this);
      this.onKeyVerified = this.onKeyVerified.bind(this);
      this.setProfile = this.setProfile.bind(this);
    }

    componentDidMount() {
      document.body.style.backgroundColor = '#e2e3e5';
      this.props.onInit();
    }

    nextDirective(index) {
      switch (index) {
        case 'login':
          return <LoginReactComponent signIn={this.props.signIn} onCreateAccount={() =>
                                     this.setCurrentStep('invitationKey')}/>;
        case 'invitationKey':
          return <InvitationKeyReact onInvitationKeyVerify={(key) => this.onKeyVerified(key)}/>;
        case 'accountCreation':
          return <AccountCreation invitationKey={this.state.invitationKey}
                                  setProfile={this.setProfile}/>;
        case 'accountCreationSuccess':
          return <AccountCreationSuccess profile={this.state.profile}/>;
        default:
          return;
        }
      }

    setCurrentStep(nextStep) {
      this.setState({
        currentStep: nextStep
      });
    }

    onKeyVerified(invitationKey) {
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
          <div style={styles.template(this.props.windowSize, pageImages[this.state.currentStep])}>
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

export default RegistrationPageTemplateReact;

@Component({
  template: '<div #root></div>'
})
export class SignInTemplateComponent extends ReactWrapperBase {

  constructor(private signInService: SignInService, private router: Router) {
    super(RegistrationPageTemplateReact, ['onInit', 'signIn']);
    this.onInit = this.onInit.bind(this);
    this.signIn = this.signIn.bind(this);
  }

  onInit(): void  {
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
