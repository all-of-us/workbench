import {
  Button
} from 'app/components/buttons';

import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';


import {Component, Input} from '@angular/core';
import {ReactWrapperBase} from 'app/utils';
import * as React from 'react';


export interface TextModalProps {
  title: string;
  body: string;
  buttonText: string;
  closeFunction: Function;
}

export class TextModal extends React.Component<TextModalProps> {

  static defaultProps = {
    buttonText: 'OK'
  };

  constructor(props: TextModalProps) {
    super(props);
  }

  render() {
    return (
      <React.Fragment>
        <Modal onRequestClose={this.props.closeFunction}>
          <ModalTitle>{this.props.title}</ModalTitle>
          <ModalBody>{this.props.body}</ModalBody>
          <ModalFooter>
            <Button onClick={this.props.closeFunction}>{this.props.buttonText}</Button>
          </ModalFooter>
        </Modal>
      </React.Fragment>
    );
  }
}

@Component({
  selector: 'app-text-modal',
  template: '<div #root></div>',
})
export class TextModalComponent extends ReactWrapperBase {
  @Input('title') title: TextModalProps['title'];
  @Input('body') body: TextModalProps['body'];
  @Input('buttonText') buttonText: TextModalProps['buttonText'];
  @Input('closeFunction') closeFunction: TextModalProps['closeFunction'];

  constructor() {
    super(TextModal, ['title', 'body', 'buttonText', 'closeFunction']);
  }
}
