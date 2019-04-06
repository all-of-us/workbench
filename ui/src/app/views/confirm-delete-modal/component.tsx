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
  decamelize,
  ReactWrapperBase
} from 'app/utils';
import {ResourceType} from 'app/utils/resourceActions';
import * as fp from 'lodash/fp';
import * as React from 'react';

export interface ConfirmDeleteModalProps {
  closeFunction: Function;
  resourceType: string;
  receiveDelete: Function;
  resourceName: string;
}

export interface ConfirmDeleteModalState {
  loading: boolean;
}

export class ConfirmDeleteModal
  extends React.Component<ConfirmDeleteModalProps, ConfirmDeleteModalState> {

  static transformResourceTypeName(resourceType: string): string {
    return fp.startCase(decamelize(resourceType, ' '));
  }

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
          delete {ConfirmDeleteModal.transformResourceTypeName(this.props.resourceType)}
          : {this.props.resourceName}?
        </ModalTitle>
        <ModalBody style={{marginTop: '0.2rem', lineHeight: '28.px'}}>
          This will permanently delete
          the {ConfirmDeleteModal.transformResourceTypeName(this.props.resourceType)}
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
              Delete {ConfirmDeleteModal.transformResourceTypeName(this.props.resourceType)}
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
