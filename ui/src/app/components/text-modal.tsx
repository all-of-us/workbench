import {
  Button
} from 'app/components/buttons';

import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';


import * as React from 'react';


export interface TextModalProps {
  title: string;
  body: string;
  onConfirm: Function;
}

export class TextModal extends React.Component<TextModalProps> {

  constructor(props: TextModalProps) {
    super(props);
  }

  render() {
    return (
      <React.Fragment>
        <Modal>
          <ModalTitle>{this.props.title}</ModalTitle>
          <ModalBody>{this.props.body}</ModalBody>
          <ModalFooter>
            <Button onClick={this.props.onConfirm}>OK</Button>
          </ModalFooter>
        </Modal>
      </React.Fragment>
    );
  }
}
