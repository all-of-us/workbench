import {Component, DoCheck, Input, OnInit} from '@angular/core';

import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {
  ProfileApi,
} from 'generated/fetch/api';

import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';

import {
  Error,
  FieldInput,
  styles as inputStyles,
} from 'app/components/inputs';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

interface AccountCreationResendModalProps {
  username: string;
  creationNonce: string;
  resend: boolean;
  closeFunction: Function;
  profileApi: ProfileApi;
}

export class AccountCreationResendModalReact extends
    React.Component<AccountCreationResendModalProps, {}> {
  constructor(props: AccountCreationResendModalProps) {
    super(props);
    this.state = {};
  }

  send() {
    this.props.profileApi.resendWelcomeEmail({
      username: this.props.username,
      creationNonce: this.props.creationNonce
    });
    this.close();
  }

  close(): void {
    this.props.closeFunction();
  }

  render() {
    const { closeFunction } = this.props;
    return <React.Fragment>
      {this.props.resend &&
      <Modal onRequestClose={closeFunction}>
        <ModalTitle>Resend Instructions</ModalTitle>
        <ModalFooter>
          <button type='button' className='btn btn-outline'
                  onClick={() => this.close()}>Cancel</button>
          <button type='button' id='resend_instructions'
                  className='btn btn-primary'
                  onClick={() => this.send()}>Send</button>
        </ModalFooter>
      </Modal>}
    </React.Fragment>;
  }
}

interface AccountCreationUpdateModalProps {
  username: string;
  creationNonce: string;
  passNewEmail: Function;
  update: boolean;
  closeFunction: Function;
  profileApi: ProfileApi;
}

interface AccountCreationUpdateModalState {
  contactEmail: string;
  emailOffFocus: boolean;
}

export class AccountCreationUpdateModalReact extends
    React.Component<AccountCreationUpdateModalProps, AccountCreationUpdateModalState> {
  state: AccountCreationUpdateModalState;
  props: AccountCreationUpdateModalProps;

  constructor(props: AccountCreationUpdateModalProps) {
    super(props);
    this.state = {
      contactEmail: '',
      emailOffFocus: true,
    };
  }

  updateAndSend(): void {
    if (this.contactEmailInvalidError()) {
      return;
    }
    this.props.passNewEmail(this.state.contactEmail);
    this.props.profileApi.updateContactEmail({
      username: this.props.username,
      contactEmail: this.state.contactEmail,
      creationNonce: this.props.creationNonce
    });
    this.props.closeFunction();
  }

  leaveFocusEmail(): void {
    this.setState({emailOffFocus: true});
  }

  enterFocusEmail(): void { this.setState({emailOffFocus: false}); }

  updateContactEmail(evt: React.ChangeEvent<HTMLInputElement>): void {
    this.setState({contactEmail: evt.target.value});
  }

  contactEmailInvalidError(): boolean {
    return !(new RegExp(/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/).test(this.state.contactEmail));
  }

  showEmailValidationError(): boolean {
    if (isBlank(this.state.contactEmail) || !this.state.emailOffFocus) {
      return false;
    }
    return this.contactEmailInvalidError();
  }

  close(): void {
    this.props.closeFunction();
  }

  render() {
    const { closeFunction } = this.props;
    return <React.Fragment>
      {this.props.update &&
      <Modal onRequestClose={closeFunction}>
        <ModalTitle>Change contact email</ModalTitle>
        <ModalBody>
          <table style={{width: '100%'}}>
            <tbody>
            <tr>
              <td><label>Contact Email:</label></td>
              <td style={{width: '70%'}}><FieldInput
                id='change-contact-email'
                style={this.showEmailValidationError() ? inputStyles.unsuccessfulInput : {}}
                onChange={(e) => this.setState({contactEmail: e.target.value})}
                onBlur={() => this.leaveFocusEmail()}
                onFocus={() => this.enterFocusEmail()}
              />
              </td>
            </tr>
            <tr>
              <td></td>
              <td style={{width: '70%'}}>
                {this.showEmailValidationError() &&
                <Error>Email is not valid.</Error>
                }
              </td>
            </tr>
            </tbody>
          </table>
        </ModalBody>
        <ModalFooter>
          <button type='button' className='btn btn-outline'
                  onClick={() => this.close()}>Cancel</button>
          <button id='change_email' type='button'
                  className='btn btn-primary'
                  onClick={() => this.updateAndSend()}>Apply</button>
        </ModalFooter>
      </Modal>}
    </React.Fragment>;
  }

}

@Component({
  selector: 'app-account-creation-modals',
  templateUrl: './component.html',
})

export class AccountCreationModalsComponent implements OnInit, DoCheck {
  @Input('updateEmail')
  public updateEmail: Function;
  @Input('close')
  public close: Function;
  @Input('username')
  public userName: string;
  @Input('creationNonce')
  public creationNonce: string;
  @Input('update')
  public update: boolean;
  @Input('resend')
  public resend: boolean;

  constructor(private profileApi: ProfileApi) {}

  renderModal(): void {
    let component;
    const props = {username: this.userName, creationNonce: this.creationNonce,
                   closeFunction: this.close, profileApi: this.profileApi};
    if (this.resend) {
      component = AccountCreationResendModalReact;
      props['resend'] = this.resend;
    } else {
      component = AccountCreationUpdateModalReact;
      props['passNewEmail'] = this.updateEmail;
      props['update'] = this.update;
    }
    ReactDOM.render(React.createElement(component, props),
      document.getElementById('account-creation-modal'));
  }

  ngOnInit(): void {
    this.renderModal();
  }

  ngDoCheck(): void {
    this.renderModal();
  }
}
