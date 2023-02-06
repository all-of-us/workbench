import * as React from 'react';

import { ResourceType } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { toDisplay } from 'app/utils/resources';

import { TextInput } from './inputs';

export interface ConfirmDeleteModalProps {
  closeFunction: Function;
  resourceType: ResourceType;
  receiveDelete: Function;
  resourceName: string;
}

export interface ConfirmDeleteModalState {
  loading: boolean;
  deleteDisabled: boolean;
}

export class ConfirmDeleteModal extends React.Component<
  ConfirmDeleteModalProps,
  ConfirmDeleteModalState
> {
  constructor(props: ConfirmDeleteModalProps) {
    super(props);
    this.state = {
      loading: false,
      deleteDisabled: true,
    };
  }

  emitDelete(): void {
    this.setState({ loading: true });
    this.props.receiveDelete();
  }

  render() {
    return (
        <Modal loading={this.state.loading}>
          <ModalTitle style={{ lineHeight: '28px' }}>
            Are you sure you want to delete {toDisplay(this.props.resourceType)}
            : {this.props.resourceName}?
          </ModalTitle>
          <ModalBody style={{ marginTop: '0.3rem', lineHeight: '28.px' }}>
            This will permanently delete the{' '}
            {toDisplay(this.props.resourceType)}
            {this.props.resourceType === ResourceType.COHORT && (
              <span> and all associated review sets</span>
            )}
            .
          </ModalBody>
          <ModalFooter style={{ paddingTop: '1.5rem' }}>
            <Button type='secondary' onClick={() => this.props.closeFunction()}>
              Cancel
            </Button>
            <Button
              disabled={this.state.loading}
              style={{ marginLeft: '0.75rem' }}
              data-test-id='confirm-delete'
              onClick={() => this.emitDelete()}
            >
              Delete {toDisplay(this.props.resourceType)}
            </Button>
          </ModalFooter>
        </Modal>
      );
    }
}
