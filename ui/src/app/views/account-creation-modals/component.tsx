import {Component, Input} from '@angular/core';

import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {
  FetchArgs,
  ProfileApiFetchParamCreator,
  ResendWelcomeEmailRequest,
  UpdateContactEmailRequest
} from 'generated/fetch/api';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

class AccountCreationModalsReact extends React.Component<any, any> {

  state: {
    changingEmail: boolean,
    resendingEmail: boolean,
    contactEmail: string,
    emailOffFocus: boolean,
    waiting: boolean,
    loading: boolean
    };
  props: {
    username: string,
    creationNonce: string,
    passNewEmail: Function
    };

  constructor(props: Object) {
    super(props);
    this.state = {
      changingEmail: false,
      resendingEmail: false,
      contactEmail: '',
      emailOffFocus: true,
      waiting: false,
      loading: false
    };
  }

  updateAndSendEmail(): void {
    this.setState({changingEmail: true});
  }

  cancelChangeEmail(): void {
    this.setState({changingEmail: false});
  }

  resendInstructions(): void {
    this.setState({resendingEmail: true});
  }

  cancelResend(): void {
    this.setState({resendingEmail: false});
  }

  updateAndSend(): void {
    this.setState({waiting: true});
    if (this.contactEmailInvalidError) {
      return;
    }
    this.props.passNewEmail(this.state.contactEmail);
    const request: UpdateContactEmailRequest = {username: this.props.username, contactEmail: this.state.contactEmail, creationNonce: this.props.creationNonce};
    const args: FetchArgs = ProfileApiFetchParamCreator().updateContactEmail(request);
    fetch(args.url, args.options).then(() => {
      this.setState({resendingEmail: false, waiting: false, changingEmail: false});
    });
  }

  send() {
    this.setState({waiting: true});
    const request: ResendWelcomeEmailRequest = {username: this.props.username, creationNonce: this.props.creationNonce};
    const args: FetchArgs = ProfileApiFetchParamCreator().resendWelcomeEmail(request);
    fetch(args.url, args.options).then(() => {
      this.setState({resending: false, waiting: false});
      this.toggleLoading();
    });
  }

  leaveFocusEmail(): void { this.setState({emailOffFocus: true}); }

  enterFocusEmail(): void { this.setState({emailOffFocus: false}); }

  contactEmailInvalidError(): boolean {
    return !(new RegExp(/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/).test(this.state.contactEmail))
  }

  showEmailValidationError(): boolean {
    if (isBlank(this.state.contactEmail) || !this.state.emailOffFocus) {
      return false;
    }
    return this.contactEmailInvalidError();
  }

  toggleLoading(): void {
    this.setState((prevState, props) => {loading: !prevState.loading});
  }

  render() {
    return <React.Fragment>
        {this.state.changingEmail &&
        <div className="change-account-email" id={'change-account-email'}>
          <h3 className="modal-title">Change contact email</h3>
          <div className="modal-body">
            <div className="form-section">
              <label>Contact Email:</label>
              <input id={"change-contact-email"} type={"text"}
                     className={this.showEmailValidationError() ? 'input unsuccessfulInput' : 'input'}
                     name={"contact-email"} onBlur={() => this.leaveFocusEmail()}
                     onFocus={() => this.enterFocusEmail()}/>
            </div>
            {this.showEmailValidationError &&
            <div className="error" id="invalid-email-error">
              Email is not valid.
            </div>}
          </div>
          <div className="modal-footer">
            <button type={"button"} className="btn btn-outline" onClick={() => this.cancelChangeEmail()}>Cancel</button>
            <button id={"change_email"} type={"button"}
                    className={"btn btn-primary" + (this.state.loading ? 'is-loading' : '')}
                    onClick={() => this.updateAndSend()}>Apply</button>
          </div>
        </div>}
      {this.state.resendingEmail &&
      <div className="resend_welcome" id={'resend-instructions'}>
        <h3 className="modal-title">Resend Instructions</h3>
        <div className="modal-footer">
          <button type={'button'} className="btn btn-outline" onClick={() => this.cancelResend()}>Cancel</button>
          <button type={'button'} id={'resend_instructions'}
                  className={'btn btn-primary' + (this.state.loading ? 'is-loading' : '')}
                  onClick={() => this.send()}>Send</button>
        </div>
      </div>}
    </React.Fragment>;
  }

}

@Component({
  selector: 'app-account-creation-modals',
  templateUrl: './component.html',
  styleUrls: ['./component.css',
    '../../styles/buttons.css'],
})

export class AccountCreationModalsComponent {
  @Input('updateEmail')
  public updateEmail: Function;
  @Input('username')
  public userName: string;
  @Input('creationNonce')
  public creationNonce: string;

  constructor() {}

  ngOnInit(): void {
    ReactDOM.render(React.createElement(AccountCreationModalsReact,
      {username: this.userName, creationNonce: this.creationNonce, updateEmail: this.creationNonce}),
      document.getElementById('account-creation-modal'));
  }

  ngDoCheck(): void {
    ReactDOM.render(React.createElement(AccountCreationModalsReact,
      {username: this.userName, creationNonce: this.creationNonce, updateEmail: this.creationNonce}),
      document.getElementById('account-creation-modal'));
  }
}
