import {Component, DoCheck, Input, OnInit} from '@angular/core';

import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {fullUrl, handleErrors} from 'app/utils/fetch';

import {
  FetchArgs,
  ProfileApiFetchParamCreator,
  ResendWelcomeEmailRequest,
  UpdateContactEmailRequest
} from 'generated/fetch/api';

import {
  Error,
  FieldInput,
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
  styles,
} from 'app/react-components/modals';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

class AccountCreationResendModalReact extends React.Component<any, any> {

  state: {};
  props: {
    username: string,
    creationNonce: string,
    resend: boolean,
    closeFunction: Function,
  };

  constructor(props: Object) {
    super(props);
    this.state = {};
  }

  send() {
    const request: ResendWelcomeEmailRequest = {
      username: this.props.username,
      creationNonce: this.props.creationNonce
    };
    const args: FetchArgs = ProfileApiFetchParamCreator().resendWelcomeEmail(request);
    fetch(fullUrl(args.url), args.options)
      .then(handleErrors)
      .catch(error => console.log(error));
    this.close();
  }

  close(): void {
    this.props.closeFunction();
  }

  render() {
    return <React.Fragment>
      {this.props.resend &&
      <Modal id='resend-instructions'>
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

class AccountCreationUpdateModalReact extends React.Component<any, any> {

  state: {
    contactEmail: string,
    emailOffFocus: boolean,
  };
  props: {
    username: string,
    creationNonce: string,
    passNewEmail: Function,
    update: boolean,
    closeFunction: Function,
  };

  constructor(props: Object) {
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
    const request: UpdateContactEmailRequest = {
      username: this.props.username,
      contactEmail: this.state.contactEmail,
      creationNonce: this.props.creationNonce
    };
    const args: FetchArgs = ProfileApiFetchParamCreator().updateContactEmail(request);
    fetch(fullUrl(args.url), args.options)
      .then(handleErrors)
      .catch(error => console.log(error));
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
    return <React.Fragment>
      {this.props.update &&
      <Modal>
        <ModalTitle>Change contact email</ModalTitle>
        <ModalBody>
          <table style={{width: '100%'}}>
            <tbody>
            <tr>
              <td><label>Contact Email:</label></td>
              <td style={{width: '70%'}}><FieldInput
                id='change-contact-email'
                style={this.showEmailValidationError() ? styles.unsuccessfulInput : {}}
                onChange={(e) => this.setState({contactEmail: e.target.value})}
                onBlur={() => this.leaveFocusEmail()}
                onFocus={() => this.enterFocusEmail()}
              >
              </FieldInput>
              </td>
            </tr>
            <tr>
              <td></td>
              <td style={{width: '76%'}}>
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

  constructor(
  ) {}

  renderModal(): void {
    let component;
    const props = {username: this.userName, creationNonce: this.creationNonce,
                   closeFunction: this.close};
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
