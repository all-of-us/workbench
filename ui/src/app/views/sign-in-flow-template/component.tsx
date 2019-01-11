import {Component, OnChanges, OnInit} from '@angular/core';
import {Router} from '@angular/router';

import {SignInService} from 'app/services/sign-in.service';
import {withWindowSize} from 'app/utils';
import {InvitationKeyReact} from 'app/views/invitation-key/component';
import {LoginReactComponent} from 'app/views/login/component';

import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {styles} from './style';

interface ImagesSource {
  backgroundImgSrc: string;
  smallerBackgroundImgSrc: string;
}

interface PageTemplateState {
  currentStep: string;
  invitationKey: string;
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
  'accountCreation': {
    backgroundImgSrc: '/assets/images/create-account-male.png',
    smallerBackgroundImgSrc: '/assets/images/create-account-male-standing.png'
  }};

@withWindowSize()
export class SignPageTemplateReact extends React.Component<any, PageTemplateState> {
  headerImg = '/assets/images/logo-registration-non-signed-in.svg';

  constructor(props: object) {
    super(props);
    this.state = {
      currentStep: 'login',
      invitationKey: ''
    };
    this.setCurrentStep = this.setCurrentStep.bind(this);
    this.setInvitationKey = this.setInvitationKey.bind(this);
  }

  nextDirective(index) {
    switch (index) {
      case 'login': return <LoginReactComponent onCreateAccount={() =>
                                                          this.setCurrentStep('invitationKey')}
                                          signIn={this.props.signIn}/>;
      case 'invitationKey': return <InvitationKeyReact onInvitationkeyVerify={() =>
                                                          this.setCurrentStep('accountCreation')}
                                         setInvitationKey={this.setInvitationKey}/>;
      // case 'accountCreation': return <AccountCreationReact setCurrentStep={this.setCurrentStep}
      //                                      invitationKey={this.state.invitationKey}>
      //                </AccountCreationReact>;
      default: return;
    }
  }

  setCurrentStep(nextStep) {
    this.setState({
      currentStep: nextStep
    });
  }

  setInvitationKey(invitationKey) {
    this.setState({
      invitationKey: invitationKey
    });
  }

  render() {
    return <div style={styles.signedInContainer}>
      <div style={{width: '100%', display: 'flex', flexDirection: 'column'}}>
        <div style={styles.template(this.props.windowSize, pageImages[this.state.currentStep])}>
          <img style={styles.headerImage} src={this.headerImg}/>
          <div style={styles.content}>
            {this.nextDirective(this.state.currentStep)}
          </div>
        </div>
      </div>
    </div>;
  }
}

export default SignPageTemplateReact;

@Component({
  templateUrl: './component.html'
})
export class SignInTemplateComponent implements OnChanges, OnInit {
  constructor(
    private signInService: SignInService,
    private router: Router,
  ) {}

  ngOnInit() {
    document.body.style.backgroundColor = '#e2e3e5';

    this.signInService.isSignedIn$.subscribe((signedIn) => {
      if (signedIn) {
        this.router.navigateByUrl('/');
      }
    });
    this.renderReact();
  }

  ngOnChanges() {
    this.renderReact();
  }

  renderReact() {
    ReactDOM.render(React.createElement(SignPageTemplateReact, {signIn: () => this.signIn()}),
        document.getElementById('pagetemplate'));
  }

  signIn(): void {
    this.signInService.signIn();
  }
}
