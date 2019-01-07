import {
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnInit,
  ViewChild
} from '@angular/core';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import {withWindowSize} from '../../utils';
import InvitationKeyReactComponent, {InvitationKeyReact} from '../invitation-key/component';
import {LoginReactComponent} from '../login/component';
import {Header, Signedin, Template} from './image';

@withWindowSize()
export class PageTemplateSignedOutReact extends React.Component<any, {currentStep: number }> {
  headerImg = '/assets/images/logo-registration-non-signed-in.svg';
  state: { currentStep: number };
  invitationKey: string;
  stepsImages: [
   {
     backgroundImgSrc: '/assets/images/login-group.png',
     smallerBackgroundImgSrc: '/assets/images/login-standing.png'
   },
   {
     backgroundImgSrc: '/assets/images/invitation-female.png',
     smallerBackgroundImgSrc: '/assets/images/invitation-female-standing.png'
   }];

  constructor(props: object) {
    super(props);
    this.state = {
      currentStep: 0
    };
    this.stepsImages = [{
      backgroundImgSrc: '/assets/images/login-group.png',
      smallerBackgroundImgSrc: '/assets/images/login-standing.png'
    },
      {
        backgroundImgSrc: '/assets/images/invitation-female.png',
        smallerBackgroundImgSrc: '/assets/images/invitation-female-standing.png'
      }];
    this.updateNext = this.updateNext.bind(this);
    this.setInvitationKey = this.setInvitationKey.bind(this);
    this.invitationKey = '';
  }

  nextDirective(index) {
    switch (index) {
      case 0: return <LoginReactComponent updateNext={this.updateNext}/>;
      case 1: return <InvitationKeyReact updateNext={this.updateNext}
                                         setInvitationKey={this.setInvitationKey}/>;
      // case 2: return <AccountCreationReact updateNext={this.updateNext}
      //                                      invitationKey={this.invitationKey}>
      //                </AccountCreationReact>;
    }
  }

  updateNext(nextStep) {
    this.setState({
      currentStep: nextStep
    });
  }

  setInvitationKey(invitationKey) {
    this.invitationKey = this.invitationKey;
  }

  render() {
    return <Template images={this.stepsImages[this.state.currentStep]}
      windowsize={this.props.windowSize}>
        <div className='row'>
          <Header src={this.headerImg}/>
        </div>
        <div className='row'>
          <Signedin>
            {this.nextDirective(this.state.currentStep)}
          </Signedin>
        </div>
      </Template>;
  }
}

export default PageTemplateSignedOutReact;

@Component({
  templateUrl: './component.html'
})
export class PageTemplateSignedOutComponent implements OnChanges, OnInit {
  constructor() {}

  ngOnInit() {
    ReactDOM.render(React.createElement(PageTemplateSignedOutReact),
        document.getElementById('reactcomp'));
  }

  ngOnChanges() {
    ReactDOM.render(React.createElement(PageTemplateSignedOutReact),
        document.getElementById('reactcomp'));
  }
}
