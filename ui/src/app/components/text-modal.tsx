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
  closeFunction: Function;
}

export class TextModal extends React.Component<TextModalProps> {

  constructor(props: TextModalProps) {
    super(props);
  }

  render() {
    console.log(this.props);
    return (
      <React.Fragment>
        <Modal onRequestClose={() => { console.log("from request closeFunction"); this.props.closeFunction()}}>
          <ModalTitle>{this.props.title}</ModalTitle>
          <ModalBody>{this.props.body}</ModalBody>
          <ModalFooter>
            <Button onClick={() => { console.log("from on click "); this.props.closeFunction()}}>OK</Button>
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
  @Input('closeFunction') closeFunction: TextModalProps['closeFunction'];

  constructor() {
    super(TextModal, ['title', 'body', 'closeFunction']);
  }
}
