import {
  Component, Input,
} from '@angular/core';

import {
  Button
} from 'app/components/buttons';

import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';

import {
  ReactWrapperBase
} from 'app/utils';
import {toDisplay} from 'app/utils/resourceActions';
import * as React from 'react';
import {ResourceType} from "generated/fetch";

export interface ConfirmDeleteModalProps {
  closeFunction: Function;
  resourceType: ResourceType;
  receiveDelete: Function;
  resourceName: string;
}

export interface ConfirmDeleteModalState {
  loading: boolean;
}

export class ConfirmDeleteModal
  extends React.Component<ConfirmDeleteModalProps, ConfirmDeleteModalState> {

  constructor(props: ConfirmDeleteModalProps) {
    super(props);
    this.state = {loading: false};
  }

  emitDelete(): void {
    this.setState({loading: true});
    this.props.receiveDelete();
  }

  render() {
    return <Modal loading={this.state.loading}>
        <ModalTitle style={{lineHeight: '28px'}}>
          Are you sure you want to
          delete {toDisplay(this.props.resourceType)}
          : {this.props.resourceName}?
        </ModalTitle>
        <ModalBody style={{marginTop: '0.2rem', lineHeight: '28.px'}}>
          This will permanently delete
          the {toDisplay(this.props.resourceType)}
          {this.props.resourceType === ResourceType.COHORT &&
          <span> and all associated review sets</span>}.
        </ModalBody>
        <ModalFooter style={{paddingTop: '1rem'}}>
          <Button
            type='secondary'
            onClick={() => this.props.closeFunction()}>Cancel</Button>
          <Button
            disabled={this.state.loading}
            style={{marginLeft: '0.5rem'}}
            data-test-id='confirm-delete'
            onClick={() => this.emitDelete()}>
              Delete {toDisplay(this.props.resourceType)}
          </Button>
        </ModalFooter>
      </Modal>;
  }
}

@Component({
  selector: 'app-confirm-delete-modal',
  template: '<div #root></div>',
})
export class ConfirmDeleteModalComponent extends ReactWrapperBase {
  @Input('resourceType') resourceType: ConfirmDeleteModalProps['resourceType'];
  @Input('resourceName') resourceName: ConfirmDeleteModalProps['resourceName'];
  @Input('closeFunction') closeFunction: ConfirmDeleteModalProps['closeFunction'];
  @Input('receiveDelete') receiveDelete: ConfirmDeleteModalProps['receiveDelete'];

  constructor() {
    super(ConfirmDeleteModal, ['resourceType', 'resourceName',
      'closeFunction', 'receiveDelete']);
  }
}
