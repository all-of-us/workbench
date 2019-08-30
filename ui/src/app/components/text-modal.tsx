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
import {Component, Input} from "@angular/core";
import {ReactWrapperBase} from "app/utils";


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
    console.log(this.props);
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

@Component({
  selector: 'text-modal',
  template: '<div #root></div>',
})
export class TextModalComponent extends ReactWrapperBase {
  @Input('title') title: TextModalProps['title'];
  @Input('body') body: TextModalProps['body'];
  @Input('onConfirm') onConfirm: TextModalProps['onConfirm'];

  constructor() {
    super(TextModal, ['title', 'body', 'onConfirm']);
  }
}
