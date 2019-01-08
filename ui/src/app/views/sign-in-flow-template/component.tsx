import {Component, OnChanges, OnInit} from '@angular/core';
import {Router} from '@angular/router';

import {SignInService} from 'app/services/sign-in.service';
import {withWindowSize} from 'app/utils';
import {InvitationKeyReact} from 'app/views/invitation-key/component';
import {LoginReactComponent} from 'app/views/login/component';

import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {Content, Header, SignedIn, Template} from './style';

interface ImagesSource {
  backgroundImgSrc: string;
  smallerBackgroundImgSrc: string;
}

interface PageTemplateState {
  currentStep: number;
  invitationKey: string;
}

@withWindowSize()
export class SignPageTemplateReact extends React.Component<any, PageTemplateState> {
  state: PageTemplateState;
  pageImages: Array<ImagesSource>;
  headerImg = '/assets/images/logo-registration-non-signed-in.svg';

  constructor(props: object) {
    super(props);
    this.state = {
      currentStep: 0,
      invitationKey: ''
    };
    this.pageImages = [
      {
        backgroundImgSrc: '/assets/images/login-group.png',
        smallerBackgroundImgSrc: '/assets/images/login-standing.png'
      },
      {
        backgroundImgSrc: '/assets/images/invitation-female.png',
        smallerBackgroundImgSrc: '/assets/images/invitation-female-standing.png'
      },
      {
        backgroundImgSrc: '/assets/images/create-account-male.png',
        smallerBackgroundImgSrc: '/assets/images/create-account-male-standing.png'
      },
      {
        backgroundImgSrc: '/assets/images/congrats-female.png',
        smallerBackgroundImgSrc: '/assets/images/congrats-female-standing.png'
      }];
    this.updateNext = this.updateNext.bind(this);
    this.setInvitationKey = this.setInvitationKey.bind(this);
  }

  nextDirective(index) {
    switch (index) {
      case 0: return <LoginReactComponent updateNext={this.updateNext}
                                          signIn={this.props.signIn}/>;
      case 1: return <InvitationKeyReact updateNext={this.updateNext}
                                         setInvitationKey={this.setInvitationKey}/>;
      // case 2: return <AccountCreationReact updateNext={this.updateNext}
      //                                      invitationKey={this.state.invitationKey}>
      //                </AccountCreationReact>;
      default: return;
    }
  }

  updateNext(nextStep) {
    this.setState({
      currentStep: nextStep
    });
  }

  setInvitationKey(invitationKey) {
    this.state.invitationKey = invitationKey;
  }

  render() {
    return <SignedIn>
      <div style={{width: '100%', display: 'flex', flexDirection: 'column'}}>
        <Template images={this.pageImages[this.state.currentStep]}
                  windowsize={this.props.windowSize}>
            <Header src={this.headerImg}/>
            <Content>
              {this.nextDirective(this.state.currentStep)}
            </Content>
          </Template>
      </div>
    </SignedIn>;
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
