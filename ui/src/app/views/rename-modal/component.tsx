import {Component, Input} from '@angular/core';
import * as React from 'react';

import {
  Button
} from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';

import {ReactWrapperBase} from 'app/utils';


interface RenameModalProps {
  resourceName: string;
  onRename: Function;
  onCancel: Function;
}
interface RenameModalState {
  loading: boolean;
  newName: string;
}

export class RenameModal extends React.Component<RenameModalProps, RenameModalState> {
  constructor(props: RenameModalProps) {
    super(props);
    this.state = {
      loading: false,
      newName: ''
    };
  }

  private rename(): void {
    this.setState({
      loading: true
    });
    this.props.onRename({
      name: this.props.resourceName,
      newName: this.state.newName
    });
  }

  render() {
    return <React.Fragment>
      <Modal>
        <ModalTitle>Please enter the new name for {this.props.resourceName}</ModalTitle>
        <ModalBody>
          <label>New Name: </label>
          <input id='new-name' type='text'
                 onChange={(e) => this.setState({newName: e.target.value})}>
          </input>
        </ModalBody>
        <ModalFooter>
          <Button type='secondary' onClick={() => this.props.onCancel()}>Cancel</Button>
          {/* TODO: Use a loading spinner here in addition to disabling. */}
          <Button data-test-id='rename-button'
                  disabled={this.state.loading}
                  style={{marginLeft: '.5rem'}}
                  onClick={() => this.rename()}>Rename Notebook</Button>
        </ModalFooter>
      </Modal>
    </React.Fragment>;
  }
}

@Component({
  selector: 'app-rename-modal',
  template: '<div #root></div>'
})
export class RenameModalComponent extends ReactWrapperBase {
  @Input('resource') resourceName: RenameModalProps['resourceName'];
  @Input('onRename') onRename: RenameModalProps['onRename'];
  @Input('onCancel') onCancel: RenameModalProps['onCancel'];

  constructor() {
    super(RenameModal, ['resourceName', 'onRename', 'onCancel']);
  }
}
