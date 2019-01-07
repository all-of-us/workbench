import {Component, DoCheck, Input, OnInit} from '@angular/core';

import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {
  BolderHeader,
  Header,
  SmallHeader
} from 'app/components/headers';
import {
  AccountCreationResendModalReact,
  AccountCreationUpdateModalReact
} from 'app/views/account-creation-modals/component';
import {AccountCreationComponent} from 'app/views/account-creation/component';
import {LoginComponent} from 'app/views/login/component';


const styles = {
  buttonLinkStyling: {
    border: 'none',
    cursor: 'pointer',
    outlineColor: 'transparent',
    color: '#2691D0',
    backgroundColor: 'transparent',
  }
};

interface AccountCreationSuccessProps {
  username: string;
  contactEmailOnCreation: string;
  creationNonce: string;
}

interface AccountCreationSuccessState {
  resendModal: boolean;
  updateModal: boolean;
  contactEmail: string;
}

export class AccountCreationSuccessReact
    extends React.Component<AccountCreationSuccessProps, AccountCreationSuccessState> {
  state: AccountCreationSuccessState;
  props: AccountCreationSuccessProps;

  constructor(props: AccountCreationSuccessProps) {
    super(props);
    this.state = {
      contactEmail: this.props.contactEmailOnCreation,
      resendModal: false,
      updateModal: false,
    };
  }

  public closeModals(): void {
    this.setState({resendModal: false});
    this.setState({updateModal: false});
  }

  updateEmail(newEmail: string): void {
    this.setState({contactEmail: newEmail});
  }

  render() {
    return <React.Fragment>
      <div style={{marginLeft: '-0.5rem', marginRight: '-0.5rem'}}>
        <BolderHeader>
          CONGRATULATIONS!
        </BolderHeader>
        <div>
          <SmallHeader>
            Your All of Us research account has been created!
          </SmallHeader>
        </div>
        <div>
          <Header style={{fontWeight: 400}}>
            Your new account
          </Header>
        </div>
        <div style={{whiteSpace: 'nowrap'}}>
          <Header style={{fontWeight: 400, marginTop: '0.5rem'}}>
            {this.props.username}
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
            Your contact email is: {this.state.contactEmail}
          </SmallHeader>
        </div>
        <div style={{paddingTop: '0.5rem'}}>
          <button style={styles.buttonLinkStyling}
                  onClick={() => this.setState({resendModal: true})}>
            Resend Instructions
          </button>
          |
          <button style={styles.buttonLinkStyling}
                  onClick={() => this.setState({updateModal: true})}>
            Change contact email
          </button>
        </div>
      </div>
      <AccountCreationResendModalReact
        username={this.props.username}
        creationNonce={this.props.creationNonce}
        resend={this.state.resendModal}
        closeFunction={() => this.closeModals()}
      />
      <AccountCreationUpdateModalReact
        username={this.props.username}
        creationNonce={this.props.creationNonce}
        passNewEmail={(newEmail: string) => this.updateEmail(newEmail)}
        update={this.state.updateModal}
        closeFunction={() => this.closeModals()}
      />
    </React.Fragment>;
  }
}
@Component({
  selector : 'app-account-creation-success',
  styleUrls: ['../../styles/template.css'],
  templateUrl: './component.html'
})
export class AccountCreationSuccessComponent implements DoCheck, OnInit {
  username: string;
  @Input('contactEmail')
  contactEmail: string;
  constructor(
    private loginComponent: LoginComponent,
    private account: AccountCreationComponent
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
    ReactDOM.render(<AccountCreationSuccessReact
        contactEmailOnCreation={this.contactEmail}
        username={this.account.profile.username}
        creationNonce = {this.account.profile.creationNonce}/>,
      document.getElementById('account-creation-success'));
  }

  public getEmail(contactEmail: string) {
    this.contactEmail = contactEmail;
  }
}
