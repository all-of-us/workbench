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
import * as fp from 'lodash/fp';
import * as React from 'react';

export interface ConfirmDeleteModalProps {
  deleting: boolean;
  closeFunction: Function;
  resourceType: string;
  receiveDelete: Function;
  resource: { name: string };
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
    return <React.Fragment>
      {this.props.deleting &&
      <Modal className='confirmDeleteModal'>
        <ModalTitle style={{lineHeight: '28px'}}>
          Are you sure you want to
          delete {ConfirmDeleteModal.transformResourceTypeName(this.props.resourceType)}
          : {this.props.resource.name}?
        </ModalTitle>
        <ModalBody style={{marginTop: '0.2rem', lineHeight: '28.px'}}>
          This will permanently delete
          the {ConfirmDeleteModal.transformResourceTypeName(this.props.resourceType)}.
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
      </Modal>}
    </React.Fragment>;
  }
}

@Component({
  selector: 'app-confirm-delete-modal',
  template: '<div #root></div>',
})
export class ConfirmDeleteModalComponent extends ReactWrapperBase {
  @Input('resourceType') resourceType: ConfirmDeleteModalProps['resourceType'];
  @Input('resource') resource: ConfirmDeleteModalProps['resource'];
  @Input('deleting') deleting: ConfirmDeleteModalProps['deleting'];
  @Input('closeFunction') closeFunction: ConfirmDeleteModalProps['closeFunction'];
  @Input('receiveDelete') receiveDelete: ConfirmDeleteModalProps['receiveDelete'];

  constructor() {
    super(ConfirmDeleteModal, ['resourceType', 'resource', 'deleting',
      'closeFunction', 'receiveDelete']);
  }
}
