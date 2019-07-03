import * as React from 'react';
import * as Cookies from 'js-cookie';
import {Button} from 'app/components/buttons';
import {CheckBox} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';


export interface Props {
  onClose: Function;
  onContinue: Function;
}

interface State {
  checked: boolean;
}

export class ConfirmPlaygroundModeModal extends React.Component<Props, State> {

  private KEY = 'DID_USER_CONFIRM_PLAYGROUND_MODE';

  constructor(props: Props) {
    super(props);

    this.state = {
      checked:  Cookies.get(this.KEY) === String(true)
    };
  }

  render() {
    return <Modal>
      <ModalTitle>Playground Mode</ModalTitle>
      <ModalBody>
        <p style={{color: '#262262'}}>
          Playground mode allows you to explore, change and run the code,
          but your edits will not be saved.
        </p>
        <p style={{color: '#262262'}}>
          To save your work, choose <b>Make a Copy</b> from the <b>File Menu</b>
          to make your own version.
        </p>
      </ModalBody>
      <ModalFooter style={{alignItems: 'center'}}>
        <CheckBox checked={this.state.checked} onChange={() => {this.setState({checked: !this.state.checked})}} />
        <label style={{marginLeft: '8px', marginRight: 'auto', color: '#262262'}}>
          Don't show again
        </label>
        <Button type='secondary' style={{margin: '0 10px'}}>
          Cancel
        </Button>
        <Button type='primary' onClick={() => {Cookies.set(this.KEY, String(this.state.checked));} }>
          Continue
        </Button>
      </ModalFooter>
    </Modal>;
  }

}
