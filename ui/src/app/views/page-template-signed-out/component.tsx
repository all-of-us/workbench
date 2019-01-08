import {
  Component,
  OnChanges,
  OnInit,
} from '@angular/core';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import {withWindowSize} from '../../utils';
import {InvitationKeyReact} from '../invitation-key/component';
import {LoginReactComponent} from '../login/component';
import {Content, Header, SignedIn, Template} from './image';
import {SignInService} from '../../services/sign-in.service';
import {Router} from '@angular/router';

interface ImagesInformation {
  backgroundImgSrc: string;
  smallerBackgroundImgSrc: string;
}

interface PageTemplateState {
  currentStep: number;
  invitationKey: string;
}

@withWindowSize()
export class PageTemplateSignedOutReact extends React.Component<any, PageTemplateState> {
  state: PageTemplateState;
  stepsImages: Array<ImagesInformation>;
  headerImg = '/assets/images/logo-registration-non-signed-in.svg';

  constructor(props: object) {
    super(props);
    this.state = {
      currentStep: 0,
      invitationKey: ''
    };
    this.stepsImages = [
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
        <Template images={this.stepsImages[this.state.currentStep]}
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

export default PageTemplateSignedOutReact;

@Component({
  templateUrl: './component.html'
})
export class PageTemplateSignedOutComponent implements OnChanges, OnInit {
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
    ReactDOM.render(React.createElement(PageTemplateSignedOutReact, {signIn: () => this.signIn()}),
        document.getElementById('reactcomp'));
  }

  ngOnChanges() {
    ReactDOM.render(React.createElement(PageTemplateSignedOutReact, {signIn: () => this.signIn()}),
        document.getElementById('reactcomp'));
  }

  signIn(): void {
    this.signInService.signIn();
  }
}
