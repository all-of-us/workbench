import * as React from 'react';

import {Button} from 'app/components/buttons';
import {styles as headerStyles} from 'app/components/headers';
import {TextInput, Select} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import { FileDetail, Workspace } from 'generated/fetch';

import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { WorkspacePermissions } from 'app/utils/workspace-permissions';

export interface CopyNotebookProps {
    originalNotebook: FileDetail,
    onClose: Function
}
  
export interface CopyNotebookState {
    writeableWorkspaces: Array<Workspace>,
    destination: Workspace,
    newName: string,
}

export class CopyNotebookModal extends React.Component<CopyNotebookProps, CopyNotebookState> {
  constructor(props: CopyNotebookProps) {
    super(props);

    this.state = {
        writeableWorkspaces: [],
        newName: props.originalNotebook.name,
        destination: null
    };
  }

  componentDidMount() {
    workspacesApi().getWorkspaces()
    .then((response) => {
        this.setState({
            writeableWorkspaces: response.items
            .filter(item => new WorkspacePermissions(item).canWrite)
            .map(workspaceResponse => workspaceResponse.workspace)
        })
    });
  }

  save() {
    workspacesApi().cloneNotebook(
    this.state.destination.namespace,
    this.state.destination.name,
    this.state.newName);
  }

  render() {
    return <Modal onRequestClose={this.props.onClose}>
      <ModalTitle>Copy to Workspace</ModalTitle>
      <ModalBody>
        <div style={headerStyles.formLabel}>Destination *</div>
        <Select
          value=""
          options={this.state.writeableWorkspaces.map(workspace => ({
              'value': workspace,
              'label': workspace.name
          }))}
          onChange={(value) => { console.log(value); this.setState({ destination: value}) }} />
        <div style={headerStyles.formLabel}>Name *</div>
        <TextInput
          autoFocus
          value={this.state.newName}
          onChange={v => this.setState({ newName: v})}
        />
      </ModalBody>
      <ModalFooter>
        <Button type='secondary' onClick={this.props.onClose}>Cancel</Button>
        <Button style={{marginLeft: '0.5rem'}} onClick={() => this.save()}>Copy Notebook</Button>
      </ModalFooter>
    </Modal>;
  }
}