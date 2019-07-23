import * as React from 'react';
import colors from 'app/styles/colors';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {Button} from 'app/components/buttons';
import {Spinner} from 'app/components/spinners';


export interface Props {
  email: string;
  onCancel: Function;
  onCopy: Function;
  onPlaygroundMode: Function;
}

interface State {
  copyLoading: boolean;
}

export class NotebookInUseModal extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
    this.state = {
      copyLoading: false
    };
  }

  render() {
    return <Modal onRequestClose={() => { this.props.onCancel(); }} width={600}>
      <ModalTitle>File is in use</ModalTitle>
      <ModalBody>
        <p style={{color: colors.primary}}>
          This file is currently being edited by
          <span style={{color: colors.accent}}> {this.props.email}</span>.
        </p>
        <p style={{color: colors.primary}}>
          You can make a copy, or run it in playground mode to explore
          and execute its contents without saving any changes.
        </p>
      </ModalBody>
      <ModalFooter style={{alignItems: 'center'}}>
        <Button type='secondary'
                style={{width: '6rem', margin: '0 10px'}}
                onClick={() => { this.props.onCancel(); }}>
          Cancel
        </Button>
        <Button type='secondary'
                style={{width: '8rem', margin: '0 10px'}}
                onClick={() => { this.setState({copyLoading: true}); this.props.onCopy(); }}>
          Make a copy {this.state.copyLoading && <Spinner
          style={{marginLeft: '0.3rem', height: '21px', width: '21px'}} />}
        </Button>
        <Button style={{width: '10rem', margin: '0 10px'}}
                onClick={() => { this.props.onPlaygroundMode(); }}>
          Run Playground Mode
        </Button>
      </ModalFooter>
    </Modal>;
  }

}
