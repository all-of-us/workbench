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
import {ReactComponent} from 'app/utils';


interface RenameModalProps {
  resource: any;
  resourceType: string;
  onRename: Function;
  onCancel: Function;
}
interface RenameModalState {
  loading: boolean;
  newName: string;
}

@ReactComponent({
  selector: 'app-rename-modal',
  propNames: ['resource', 'resourceType', 'onRename', 'onCancel']
})
export class RenameModalComponent extends React.Component<RenameModalProps, RenameModalState> {
  constructor(props: RenameModalProps) {
    super(props);
    this.state = {
      loading: false,
      newName: ''
    };
  }

  private emitRename(): void {
    if (this.state.loading) {
      return;
    }
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
          <label>New Name: </label>
          <input id='new-name' type='text'
                 onChange={(e) => this.setState({newName: e.target.value})}>
          </input>
        </ModalBody>
        <ModalFooter>
          <Button type='secondary' onClick={() => this.props.onCancel()}>Cancel</Button>
          {/* TODO: Use a loading spinner here in addition to disabling. */}
          <Button id='rename-button'
                  disabled={this.state.loading}
                  style={{marginLeft: '.5rem'}}
                  onClick={() => this.emitRename()}>Rename Notebook</Button>
        </ModalFooter>
      </Modal>
    </React.Fragment>;
  }
}
