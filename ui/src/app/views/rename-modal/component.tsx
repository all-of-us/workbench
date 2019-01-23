import {Component, Input} from '@angular/core';
import * as React from 'react';

import {
  Button
} from 'app/components/buttons';
import {styles as headerStyles} from 'app/components/headers';
import {TextInput} from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';

import {ReactWrapperBase} from 'app/utils';


interface RenameModalProps {
  resource: {name: string};
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
      name: this.props.resource.name,
      newName: this.state.newName
    });
  }

  render() {
    return <React.Fragment>
      <Modal>
        <ModalTitle>Please enter the new name for {this.props.resource.name}</ModalTitle>
        <ModalBody>
          <div style={headerStyles.formLabel}>New Name:</div>
          <TextInput id='new-name'
             onChange={v => this.setState({newName: v})}
          />
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
  @Input('resource') resource: RenameModalProps['resource'];
  @Input('onRename') onRename: RenameModalProps['onRename'];
  @Input('onCancel') onCancel: RenameModalProps['onCancel'];

  constructor() {
    super(RenameModal, ['resource', 'onRename', 'onCancel']);
  }
}
