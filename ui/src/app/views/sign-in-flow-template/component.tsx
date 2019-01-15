import {Component, Inject, OnChanges, OnInit} from '@angular/core';
import {Router} from '@angular/router';

import {SignInService} from 'app/services/sign-in.service';
import {withWindowSize} from 'app/utils';
import {InvitationKeyReact} from 'app/views/invitation-key/component';

import {AccountCreationReact} from 'app/views/account-creation/component';
import LoginReactComponent from 'app/views/login/component';

import {DataAccessLevel, Profile} from 'generated/fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {ReactWrapperBase} from 'app/utils';
import {styles} from './style';

interface ImagesSource {
  backgroundImgSrc: string;
  smallerBackgroundImgSrc: string;
}

interface PageTemplateProps {
  signIn: any;
  windowSize: { width: number, height: number };
}

interface PageTemplateState {
  currentStep: string;
  invitationKey: string;
  profile: Profile;
}

const pageImages = {
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

const SignPageTemplateReact = withWindowSize()(
  class extends React.Component<PageTemplateProps, PageTemplateState> {

    constructor(props: PageTemplateProps) {
      super(props);
      this.state = {
        currentStep: 'login',
        invitationKey: '',
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
          areaOfResearch: ''
        }
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
        case 'createAccount': return <AccountCreationReact
                          onAccountCreation={() => this.setCurrentStep('accountCreationSuccess')}
                          invitationKey={this.state.invitationKey}
                          setProfile={this.setProfile}/>;
        default:
          return;
        }
      }

    setCurrentStep(nextStep) {
      this.setState({
        currentStep: nextStep
      });
    }

    setProfile(profile) {
      this.setState({profile: profile});
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

export default SignPageTemplateReact;

@Component({
  template: '<div #root></div>'
})
export class SignInTemplateComponent extends ReactWrapperBase implements OnInit {

  constructor(private signInService: SignInService, private router: Router) {
    super(SignPageTemplateReact, ['signIn']);
    this.signIn = this.signIn.bind(this);
  }

  ngOnInit(): void  {
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
