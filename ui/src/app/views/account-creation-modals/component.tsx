import {Component, DoCheck, Input, OnInit} from '@angular/core';

import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {environment} from 'environments/environment';

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

class AccountCreationModalsReact extends React.Component<any, any> {

  state: {
    changingEmail: boolean,
    resendingEmail: boolean,
    contactEmail: string,
    emailOffFocus: boolean,
    };
  props: {
    username: string,
    creationNonce: string,
    passNewEmail: Function,
    update: boolean,
    resend: boolean,
    closeFunction: Function,
    };

  constructor(props: Object) {
    super(props);
    this.state = {
      changingEmail: false,
      resendingEmail: false,
      contactEmail: '',
      emailOffFocus: true,
    };
  }

  fullUrl(url: string): string {
    return environment.allOfUsApiUrl + url;
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
    fetch(this.fullUrl(args.url), args.options).then(() => {
      this.setState({resendingEmail: false, changingEmail: false});
    });
    this.props.closeFunction();
  }

  send() {
    const request: ResendWelcomeEmailRequest = {
      username: this.props.username,
      creationNonce: this.props.creationNonce
    };
    const args: FetchArgs = ProfileApiFetchParamCreator().resendWelcomeEmail(request);
    fetch(this.fullUrl(args.url), args.options).then(() => {
      this.setState({resending: false});
    });
    this.props.closeFunction();
  }

  leaveFocusEmail(evt): void {
    this.setState({emailOffFocus: true, contactEmail: evt.target.value});
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
                  id={'change-contact-email'}
                  style={this.showEmailValidationError() ? styles.unsuccessfulInput : {}}
                  onBlur={(e) => this.leaveFocusEmail(e)}
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
            <button type={'button'} className='btn btn-outline'
                    onClick={() => this.close()}>Cancel</button>
            <button id={'change_email'} type={'button'}
                    className={'btn btn-primary'}
                    onClick={() => this.updateAndSend()}>Apply</button>
          </ModalFooter>
        </Modal>}
      {this.props.resend &&
      <Modal id={'resend-instructions>'}>
        <ModalTitle>Resend Instructions</ModalTitle>
        <ModalFooter>
          <button type={'button'} className='btn btn-outline'
                  onClick={() => this.close()}>Cancel</button>
          <button type={'button'} id={'resend_instructions'}
                  className={'btn btn-primary'}
                  onClick={() => this.send()}>Send</button>
        </ModalFooter>
      </Modal>}
    </React.Fragment>;
  }

}

@Component({
  selector: 'app-account-creation-modals',
  templateUrl: './component.html',
  styleUrls: ['./component.css',
    '../../styles/buttons.css'],
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

  constructor() {}

  ngOnInit(): void {
    ReactDOM.render(React.createElement(AccountCreationModalsReact,
      {username: this.userName, creationNonce: this.creationNonce,
        passNewEmail: this.updateEmail, update: this.update, resend: this.resend,
        closeFunction: this.close}),
      document.getElementById('account-creation-modal'));
  }

  ngDoCheck(): void {
    ReactDOM.render(React.createElement(AccountCreationModalsReact,
      {username: this.userName, creationNonce: this.creationNonce,
        passNewEmail: this.updateEmail, update: this.update, resend: this.resend,
        closeFunction: this.close}),
      document.getElementById('account-creation-modal'));
  }
}
