import * as React from 'react';

import { Button } from 'app/components/buttons';
import { styles as headerStyles } from 'app/components/headers';
import { Select, TextInput } from 'app/components/inputs';
import { Modal, ModalBody, ModalFooter, ModalTitle } from 'app/components/modals';
import { FileDetail, Workspace } from 'generated/fetch';

import { Spinner } from 'app/components/spinners';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { WorkspacePermissions } from 'app/utils/workspace-permissions';

enum State { FORM, ERROR, SUCCESS }

export interface CopyNotebookModalProps {
  fromWorkspaceNamespace: string;
  fromWorkspaceName: string;
  fromNotebook: FileDetail;
  onClose: Function;
  onCopy: Function;
}

export interface CopyNotebookModalState {
  writeableWorkspaces: Array<Workspace>;
  destination: Workspace;
  newName: string;
  state: State;
  errorMsg: string;
  loading: boolean;
}

const boldStyle = {
  fontWeight: 700
};

export class CopyNotebookModal extends React.Component<CopyNotebookModalProps,
CopyNotebookModalState> {
  constructor(props: CopyNotebookModalProps) {
    super(props);

    this.state = {
      writeableWorkspaces: [],
      newName: props.fromNotebook.name,
      destination: null,
      state: State.FORM,
      errorMsg: '',
      loading: true
    };
  }

  componentDidMount() {
    workspacesApi().getWorkspaces()
      .then((response) => {
        this.setState({
          writeableWorkspaces: response.items
            .filter(item => new WorkspacePermissions(item).canWrite)
            .map(workspaceResponse => workspaceResponse.workspace),
          loading: false
        });
      });
  }

  save() {
    this.setState({ loading: true });

    workspacesApi().copyNotebook(
      this.props.fromWorkspaceNamespace,
      this.props.fromWorkspaceName,
      this.props.fromNotebook.name,
      {
        toWorkspaceName: this.state.destination.id,
        toWorkspaceNamespace: this.state.destination.namespace,
        newName: this.state.newName
      }
      ).then((response) => {
        this.setState({ state: State.SUCCESS, loading: false });
        this.props.onCopy(response);
      }).catch((response) => {
        const errorMsg = response.status === 400 ?
          'Notebook with the same name already exists in the targeted workspace.' :
          'An error occurred while copying. Please try again.';

        this.setState({
          errorMsg: errorMsg,
          state: State.ERROR,
          loading: false
        });
      });
  }

  goToDestinationWorkspace() {
    window.location.href = this.getWorkspaceUrl(this.state.destination);
  }

  getWorkspaceUrl(workspace: Workspace) {
    return ['workspaces', workspace.namespace, workspace.id, 'notebooks'].join('/');
  }

  render() {
    return (
      <Modal onRequestClose={this.props.onClose}>
        <ModalTitle>Copy to Workspace</ModalTitle>
          { this.state.loading ?
            <ModalBody style={{ textAlign: 'center' }}><Spinner /></ModalBody> :
            <ModalBody>
                {this.state.state === State.FORM && this.renderFormBody()}
                {this.state.state === State.ERROR && this.renderErrorBody()}
                {this.state.state === State.SUCCESS && this.renderSuccessBody()}
            </ModalBody>
        }
        <ModalFooter>
          <Button type='secondary' onClick={this.props.onClose}>
              {this.getCloseButtonText()}
          </Button>
          {this.renderActionButton()}
        </ModalFooter>
      </Modal>
    );
  }

  getCloseButtonText() {
    if (this.state.state === State.FORM) {
      return 'Cancel';
    } else if (this.state.state === State.ERROR) {
      return 'Close';
    } else if (this.state.state === State.SUCCESS) {
      return 'Stay Here';
    }
  }

  renderActionButton() {
    if (this.state.state === State.FORM) {
      return (
        <Button style={{ marginLeft: '0.5rem' }}
          disabled={this.state.destination === null}
          onClick={() => this.save()}>
          Copy Notebook
        </Button>
      );
    } else if (this.state.state === State.ERROR) {
      return null;
    } else if (this.state.state === State.SUCCESS) {
      return (
        <Button style={{ marginLeft: '0.5rem' }}
          onClick={this.goToDestinationWorkspace}>
          Go to Copied Notebook
        </Button>
      );
    }
  }

  renderFormBody() {
    return (
      <div>
        <div style={headerStyles.formLabel}>Destination *</div>
        <Select
            value=''
            options={this.state.writeableWorkspaces.map(workspace => ({
              'value': workspace,
              'label': workspace.name
            }))}
            onChange={(value) => { this.setState({ destination: value }); }} />
        <div style={headerStyles.formLabel}>Name *</div>
        <TextInput
            autoFocus
            value={this.state.newName}
            onChange={v => this.setState({ newName: v })}
        />
      </div>
    );
  }

  renderErrorBody() {
    return <div style={headerStyles.formLabel}>{this.state.errorMsg}</div>;
  }

  renderSuccessBody() {
    return (
      <div> Successfully copied
      <b style={boldStyle}> {this.props.fromNotebook.name} </b> to
      <b style={boldStyle}> {this.state.destination.name} </b>.
      Do you want to view the copied notebook?</div>
    );
  }
}
