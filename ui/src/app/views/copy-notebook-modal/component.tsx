import * as React from 'react';

import {Button} from 'app/components/buttons';
import {styles as headerStyles} from 'app/components/headers';
import {TextInput, Select} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import { FileDetail, Workspace } from 'generated/fetch';

import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { WorkspacePermissions } from 'app/utils/workspace-permissions';
import { Spinner } from 'app/components/spinners';

enum State { FORM, ERROR, SUCCESS }

export interface CopyNotebookProps {
    fromWorkspaceNamespace: string,
    fromWorkspaceName: string,
    fromNotebook: FileDetail,
    onClose: Function,
    onCopy: Function
}
  
export interface CopyNotebookState {
    writeableWorkspaces: Array<Workspace>,
    destination: Workspace,
    newName: string,
    state: State,
    errorMsg: string,
    loading: boolean
}

const boldStyle = {
    fontWeight: 700
}

export class CopyNotebookModal extends React.Component<CopyNotebookProps, CopyNotebookState> {
  constructor(props: CopyNotebookProps) {
    super(props);

    this.state = {
        writeableWorkspaces: [],
        newName: props.fromNotebook.name,
        destination: null,
        state: State.FORM,
        errorMsg: "",
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
        })
    });
  }

  save() {
      this.setState({ loading: true })
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
        this.setState({ state: State.SUCCESS, loading: false })
        this.props.onCopy(response);
    }).catch((response) => {
        console.log(response)
        let errorMsg = response.status == 400 ?
          'Notebook with the same name already exists in the targeted workspace.' :
          'An error occurred while copying. Please try again.'

        this.setState({
            errorMsg: errorMsg,
            state: State.ERROR,
            loading: false
        })
    });
  }

  getCloseButtonText() {
      if (this.state.state == State.FORM) {
          return "Cancel"
      } else if (this.state.state == State.ERROR) {
          return "Close"
      } else if (this.state.state == State.SUCCESS) {
          return "Stay Here"
      }
  }

  renderActionButton() {
      if (this.state.state == State.FORM) {
          return <Button disabled={this.state.destination == null} style={{marginLeft: '0.5rem'}} onClick={() => this.save()}>Copy Notebook</Button>
      } else if (this.state.state == State.ERROR) {
          return null
      } else if (this.state.state == State.SUCCESS) {
          return (
            <Button
                style={{marginLeft: '0.5rem'}}
                onClick={() => window.location.href = ['workspaces', this.state.destination.namespace,
            this.state.destination.id, 'notebooks'].join("/") }>
                Go to Copied Notebook
            </Button>
          )
      }
  }

  render() {
    return <Modal onRequestClose={this.props.onClose}>
      <ModalTitle>Copy to Workspace</ModalTitle>
      {this.state.loading ? <ModalBody style={{ textAlign: "center" }}><Spinner /></ModalBody> :
      <ModalBody>
        {this.state.state == State.FORM &&
        <div>
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
        </div>
        }
        {this.state.state == State.ERROR &&
        <div style={headerStyles.formLabel}>{this.state.errorMsg}</div>
        }
        {this.state.state == State.SUCCESS &&
        <div>Successfully copied <b style={boldStyle}>{this.props.fromNotebook.name}</b> to <b style={boldStyle}>{this.state.destination.name}</b>. Do you want to view the copied notebook?</div>
        }
      </ModalBody>
      }
      <ModalFooter>
        <Button
          type='secondary'
          onClick={this.props.onClose}>
          {this.getCloseButtonText()}
        </Button>
        {this.renderActionButton()}
      </ModalFooter>
    </Modal>;
  }
}