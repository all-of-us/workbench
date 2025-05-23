import * as React from 'react';

import { Button } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { Spinner } from 'app/components/spinners';
import colors from 'app/styles/colors';

interface Props {
  email: string;
  onCancel: Function;
  onCopy: Function;
}

interface State {
  copyLoading: boolean;
}

export class NotebookInUseModal extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      copyLoading: false,
    };
  }

  render() {
    return (
      <Modal
        onRequestClose={() => {
          this.props.onCancel();
        }}
        width={600}
      >
        <ModalTitle>File is in use</ModalTitle>
        <ModalBody>
          <p style={{ color: colors.primary }}>
            This file is currently being edited by
            <span style={{ color: colors.accent }}> {this.props.email}</span>.
          </p>
          <p style={{ color: colors.primary }}>
            You can make a copy to explore and execute its contents without
            saving any changes to the original.
          </p>
        </ModalBody>
        <ModalFooter style={{ alignItems: 'center' }}>
          <Button
            type='secondary'
            style={{ width: '9rem', margin: '0 10px' }}
            onClick={() => {
              this.props.onCancel();
            }}
          >
            Cancel
          </Button>
          <Button
            type='secondary'
            style={{ width: '12rem', margin: '0 10px' }}
            onClick={() => {
              this.setState({ copyLoading: true });
              this.props.onCopy();
            }}
          >
            Make a copy{' '}
            {this.state.copyLoading && (
              <Spinner
                style={{ marginLeft: '0.45rem', height: '21px', width: '21px' }}
              />
            )}
          </Button>
        </ModalFooter>
      </Modal>
    );
  }
}
