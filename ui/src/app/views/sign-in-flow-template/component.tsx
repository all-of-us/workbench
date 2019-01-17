import {Component, Inject, OnChanges, OnInit} from '@angular/core';
import {Router} from '@angular/router';

import {SignInService} from 'app/services/sign-in.service';
import {withWindowSize} from 'app/utils';
import {InvitationKeyReact} from 'app/views/invitation-key/component';
import {LoginReactComponent} from 'app/views/login/component';

import * as React from 'react';

import {ReactWrapperBase} from 'app/utils';
import {styles} from './style';

interface ImagesSource {
  backgroundImgSrc: string;
  smallerBackgroundImgSrc: string;
}

export interface PageTemplateProps {
  signIn: () => void;
  windowSize: { width: number, height: number };
}

interface PageTemplateState {
  currentStep: string;
  invitationKey: string;
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
  'createAccount': {
    backgroundImgSrc: '/assets/images/create-account-male.png',
    smallerBackgroundImgSrc: '/assets/images/create-account-male-standing.png'
  }};

const headerImg = '/assets/images/logo-registration-non-signed-in.svg';

const RegistrationPageTemplateReact = withWindowSize()(
  class extends React.Component<PageTemplateProps, PageTemplateState> {

    constructor(props: PageTemplateProps) {
      super(props);
      this.state = {
        currentStep: 'login',
        invitationKey: ''
      };
      this.setCurrentStep = this.setCurrentStep.bind(this);
      this.onKeyVerified = this.onKeyVerified.bind(this);
    }

    nextDirective(index) {
      switch (index) {
        case 'login':
          return <LoginReactComponent signIn={this.props.signIn} onCreateAccount={() =>
                                     this.setCurrentStep('invitationKey')}/>;
        case 'invitationKey':
          return <InvitationKeyReact onInvitationKeyVerify={(key) => this.onKeyVerified(key)}/>;
        // case 'accountCreation': return <AccountCreationReact setCurrentStep={this.setCurrentStep}
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
        currentStep: 'createAccount'
      });
    }

    render() {
      return <div style={styles.signedInContainer}>
        <div style={{width: '100%', display: 'flex', flexDirection: 'column'}}>
          <div id='template' style={styles.template(this.props.windowSize, pageImages[this.state.currentStep])}>
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
export class SignInTemplateComponent extends ReactWrapperBase implements OnInit {

  constructor(private signInService: SignInService, private router: Router) {
    super(RegistrationPageTemplateReact, ['signIn']);
    this.signIn = this.signIn.bind(this);
  }

  ngOnInit() {
    document.body.style.backgroundColor = '#e2e3e5';

    this.signInService.isSignedIn$.subscribe((signedIn) => {
      if (signedIn) {
        this.router.navigateByUrl('/');
      }
    });
    super.ngOnInit();
  }

  signIn(): void {
    this.signInService.signIn();
  }

}
