import * as React from 'react';

import {Button} from 'app/components/buttons';
import {Error, FieldInput, styles as inputStyles} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {profileApi} from 'app/services/swagger-fetch-clients';

interface AccountCreationResendModalProps {
  username: string;
  creationNonce: string;
  onClose: Function;
}

export class AccountCreationResendModal extends React.Component<
  AccountCreationResendModalProps
> {
  send() {
    const {username, creationNonce, onClose} = this.props;
    profileApi().resendWelcomeEmail({username, creationNonce})
      .catch(error => console.log(error));
    onClose();
  }

  render() {
    const {onClose} = this.props;
    return <Modal onRequestClose={onClose}>
      <ModalTitle>Resend Instructions</ModalTitle>
      <ModalFooter>
        <Button type='secondary' onClick={onClose}>Cancel</Button>
        <Button style={{marginLeft: '0.5rem'}} onClick={() => this.send()}>Send</Button>
      </ModalFooter>
    </Modal>;
  }
}

interface AccountCreationUpdateModalProps {
  username: string;
  creationNonce: string;
  onDone: Function;
  onClose: Function;
}

interface AccountCreationUpdateModalState {
  contactEmail: string;
  emailOffFocus: boolean;
}

export class AccountCreationUpdateModal extends React.Component<
  AccountCreationUpdateModalProps,
  AccountCreationUpdateModalState
> {
  constructor(props: AccountCreationUpdateModalProps) {
    super(props);
    this.state = {
      contactEmail: '',
      emailOffFocus: true,
    };
  }

  updateAndSend(): void {
    const {username, creationNonce, onDone} = this.props;
    const {contactEmail} = this.state;
    profileApi().updateContactEmail({username, contactEmail, creationNonce})
      .catch(error => console.log(error));
    onDone(contactEmail);
  }

  render() {
    const {onClose} = this.props;
    const {contactEmail, emailOffFocus} = this.state;
    const emailValid = new RegExp(/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/).test(contactEmail);
    const showEmailError = !emailValid && !!contactEmail.trim() && emailOffFocus;
    return <Modal onRequestClose={onClose}>
      <ModalTitle>Change contact email</ModalTitle>
      <ModalBody>
        <table style={{width: '100%'}}>
          <tbody>
          <tr>
            <td><label>Contact Email:</label></td>
            <td style={{width: '70%'}}><FieldInput
              value={contactEmail}
              style={showEmailError ? inputStyles.unsuccessfulInput : {}}
              onChange={(e) => this.setState({contactEmail: e.target.value})}
              onBlur={() => this.setState({emailOffFocus: true})}
              onFocus={() => this.setState({emailOffFocus: false})}
            />
            </td>
          </tr>
          <tr>
            <td></td>
            <td style={{width: '70%'}}>
              {showEmailError && <Error>Email is not valid.</Error>}
            </td>
          </tr>
          </tbody>
        </table>
      </ModalBody>
      <ModalFooter>
        <Button type='secondary' onClick={onClose}>Cancel</Button>
        <Button
          style={{marginLeft: '0.5rem'}}
          disabled={!emailValid}
          onClick={() => this.updateAndSend()}
        >Apply</Button>
      </ModalFooter>
    </Modal>;
  }

}
