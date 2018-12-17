import {Component, DoCheck, Input, OnInit, ViewChild} from '@angular/core';

import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {SignInService} from 'app/services/sign-in.service';

import {
  BolderHeader,
  Header,
  SmallHeader
} from 'app/common/common';
import {AccountCreationModalsComponent} from 'app/views/account-creation-modals/component';
import {AccountCreationComponent} from 'app/views/account-creation/component';
import {LoginComponent} from 'app/views/login/component';

export class AccountCreationSuccessReact extends React.Component<any, any> {
  state: {};
  props: {
    contactEmail: string,
    account: AccountCreationComponent,
    signInService: SignInService,
    accountCreationModalsComponent: AccountCreationModalsComponent,
  };
  buttonLinkStyling = {
    border: 'none',
    cursor: 'pointer',
    outlineColor: 'transparent',
    color: '#2691D0',
    backgroundColor: 'transparent',
  };
  constructor(props: Object) {
    super(props);
  }
  render() {
    return <div style={{marginLeft: '-0.5rem', marginRight: '-0.5rem'}}>
      <BolderHeader>
        CONGRATULATIONS!
      </BolderHeader>
      <div>
        <SmallHeader>
          Your All of Us research account has been created!
        </SmallHeader>
      </div>
      <div style={{height: '1.5rem'}}>
        <Header>
          Your new account
        </Header>
      </div>
      <div style={{whiteSpace: 'nowrap'}}>
        <Header style={{fontWeight: 400}}>
          {this.props.account.profile.username}
        </Header>
      </div>
      <div>
        <Header style={{marginTop: '.5rem', fontWeight: 400}}>
          is hosted by Google.
        </Header>
      </div>
      <div>
        <SmallHeader>
          Check your contact email for instructions on getting started.
        </SmallHeader>
      </div>
      <div>
        <SmallHeader>
          Your contact email is: {this.props.contactEmail}
        </SmallHeader>
      </div>
      <div style={{paddingTop: '0.5rem'}}>
        <button style={this.buttonLinkStyling} onClick={
          () => this.props.accountCreationModalsComponent.resendInstructions()
        }>
          Resend Instructions
        </button>
        |
        <button style={this.buttonLinkStyling} onClick={
          () => this.props.accountCreationModalsComponent.updateAndSendEmail()
        }>
          Change contact email
        </button>
      </div>
    </div>;
  }
}
@Component({
  selector : 'app-account-creation-success',
  styleUrls: ['../../styles/template.css'],
  templateUrl: './component.html'
})
export class AccountCreationSuccessComponent implements DoCheck, OnInit {
  @Input('contactEmail') contactEmail: string;
  @ViewChild(AccountCreationModalsComponent)
  accountCreationModalsComponent: AccountCreationModalsComponent;
  constructor(
    private loginComponent: LoginComponent,
    private account: AccountCreationComponent,
    private signInService: SignInService
  ) {
    setTimeout(() => {
      loginComponent.smallerBackgroundImgSrc = '/assets/images/congrats-female-standing.png';
      loginComponent.backgroundImgSrc = '/assets/images/congrats-female.png';
    }, 0);
  }
  ngOnInit(): void {
    this.renderReactComponent();
  }
  ngDoCheck(): void {
    this.renderReactComponent();
  }
  renderReactComponent(): void {
    ReactDOM.render(React.createElement(AccountCreationSuccessReact,
      {
        contactEmail: this.contactEmail,
        account: this.account,
        signInService: this.signInService,
        accountCreationModalsComponent: this.accountCreationModalsComponent
      }),
      document.getElementById('account-creation-success'));
  }
  receiveMessage($event) {
    this.contactEmail = $event;
    this.renderReactComponent();
  }

  get getAccount() {
    return this.account;
  }
}
