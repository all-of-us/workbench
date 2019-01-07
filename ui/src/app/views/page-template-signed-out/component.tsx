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
import InvitationKeyReactComponent from '../invitation-key/component';
import {LoginReactComponent} from '../login/component';
import {Header, Signedin, Template} from './image';

@withWindowSize()
export class PageTemplateSignedOutReactComponent extends React.Component<any, any> {
  headerImg = '/assets/images/logo-registration-non-signed-in.svg';
  state: { currentStep: number };
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
  }

  nextDirective(index) {
    switch (index) {
      case 0: return <LoginReactComponent updateNext={this.updateNext}/>;
      case 1: return <InvitationKeyReactComponent updateNext={this.updateNext}/>;
    }
  }

  updateNext(nextStep) {
    this.setState({
      currentStep: nextStep
    });
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

export default PageTemplateSignedOutReactComponent;

@Component({
  templateUrl: './component.html'
})
export class PageTemplateSignedOutComponent implements OnChanges, OnInit {
  constructor() {}

  ngOnInit() {
    ReactDOM.render(React.createElement(PageTemplateSignedOutReactComponent),
        document.getElementById('reactcomp'));
  }

  ngOnChanges() {
    ReactDOM.render(React.createElement(PageTemplateSignedOutReactComponent),
        document.getElementById('reactcomp'));
  }
}
