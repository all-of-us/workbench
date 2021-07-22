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
  buttonText: string;
  closeFunction: Function;
  role?: 'dialog' | 'alertdialog';
}

export class TextModal extends React.Component<TextModalProps> {

  static defaultProps = {
    buttonText: 'OK'
  };

  constructor(props: TextModalProps) {
    super(props);
  }

  render() {
    const {closeFunction, title, body, buttonText, role = 'dialog'} = this.props;
    return (
      <React.Fragment>
        <Modal onRequestClose={closeFunction} role={role}>
          <ModalTitle>{title}</ModalTitle>
          <ModalBody>{body}</ModalBody>
          <ModalFooter>
            <Button onClick={closeFunction}>{buttonText}</Button>
          </ModalFooter>
        </Modal>
      </React.Fragment>
    );
  }
}
