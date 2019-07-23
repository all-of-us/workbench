import * as React from 'react';
import colors from 'app/styles/colors';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {Button} from 'app/components/buttons'


export interface Props {
  email: string
  onCancel: Function;
}

interface State {
}

export class NotebookInUseModal extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
  }

  render() {
    return <Modal onRequestClose={() => { this.props.onCancel(); }}>
      <ModalTitle>File is in use</ModalTitle>
      <ModalBody>
        <p style={{color: colors.primary}}>
          This file is currently being edited by
          <span style={{color: colors.accent}}>{this.props.email}</span>.
        </p>
        <p style={{color: colors.primary}}>
          You can make a copy, or run it in playground mode to explore
          and execute its contents without saving any changes.
        </p>
      </ModalBody>
      <ModalFooter style={{alignItems: 'center'}}>
        <Button type='secondary'
                style={{margin: '0 10px'}}
                onClick={() => { this.props.onCancel(); }}>
          Cancel
        </Button>
      </ModalFooter>
    </Modal>;
  }

}
