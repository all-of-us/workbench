import * as React from 'react';

import { Button } from 'app/components/buttons';
import { styles as headerStyles } from 'app/components/headers';
import { Select, TextInput, ValidationError } from 'app/components/inputs';
import { Modal, ModalBody, ModalFooter, ModalTitle } from 'app/components/modals';
import {ConceptSet, FileDetail, Workspace} from 'generated/fetch';

import { Spinner } from 'app/components/spinners';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { navigate } from 'app/utils/navigation';
import {ResourceType} from 'app/utils/resourceActions';
import { WorkspacePermissions } from 'app/utils/workspace-permissions';

enum RequestState { UNSENT, ERROR, SUCCESS }

const ResourceTypeHomeTabs = new Map()
  .set(ResourceType.NOTEBOOK, 'notebooks')
  .set(ResourceType.COHORT, 'data')
  .set(ResourceType.CONCEPT_SET, 'data')
  .set(ResourceType.DATA_SET, 'data');

export interface Props {
  fromWorkspaceNamespace: string;
  fromWorkspaceName: string;
  fromResourceName: string;
  onClose: Function;
  onCopy: Function;
  resourceType: ResourceType;
  saveFunction: (CopyRequest) => Promise<FileDetail | ConceptSet>;
}

interface State {
  writeableWorkspaces: Array<Workspace>;
  destination: Workspace;
  newName: string;
  requestState: RequestState;
  errorMsg: string;
  loading: boolean;
}

const boldStyle = {
  fontWeight: 600
};

class CopyModal extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);

    this.state = {
      writeableWorkspaces: [],
      newName: props.fromResourceName,
      destination: null,
      requestState: RequestState.UNSENT,
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
    const {saveFunction, resourceType} = this.props;

    saveFunction({
      toWorkspaceName: this.state.destination.id,
      toWorkspaceNamespace: this.state.destination.namespace,
      newName: this.state.newName
    }).then((response) => {
      this.setState({ requestState: RequestState.SUCCESS, loading: false });
      this.props.onCopy(response);
    }).catch((response) => {
      const errorMsg = response.status === 409 ?
        `${resourceType} with the same ` +
        `name already exists in the targeted workspace.` :
        response.status === 404 ?
          `${resourceType} not found in the ` +
            `original workspace.` :
          'An error occurred while copying. Please try again.';

      this.setState({
        errorMsg: errorMsg,
        requestState: RequestState.ERROR,
        loading: false
      });
    });
  }

  goToDestinationWorkspace() {
    navigate(
      [
        'workspaces',
        this.state.destination.namespace,
        this.state.destination.id,
        ResourceTypeHomeTabs.get(this.props.resourceType)
      ]
    );
  }

  render() {
    return (
      <Modal onRequestClose={this.props.onClose}>
        <ModalTitle>Copy to Workspace</ModalTitle>
        { this.state.loading ?
          <ModalBody style={{ textAlign: 'center' }}><Spinner /></ModalBody> :
          <ModalBody>
            {(this.state.requestState === RequestState.UNSENT ||
            this.state.requestState === RequestState.ERROR) && this.renderFormBody()}
            {this.state.requestState === RequestState.SUCCESS && this.renderSuccessBody()}
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
    if (this.state.requestState === RequestState.UNSENT ||
      this.state.requestState === RequestState.ERROR) {
      return 'Close';
    } else if (this.state.requestState === RequestState.SUCCESS) {
      return 'Stay Here';
    }
  }

  renderActionButton() {
    const {resourceType} = this.props;
    if (this.state.requestState === RequestState.UNSENT ||
      this.state.requestState === RequestState.ERROR) {
      return (
        <Button style={{ marginLeft: '0.5rem' }}
                disabled={this.state.destination === null || this.state.loading}
                onClick={() => this.save()}
                data-test-id='copy-button'>
          Copy {resourceType}
        </Button>
      );
    } else if (this.state.requestState === RequestState.SUCCESS) {
      return (
        <Button style={{ marginLeft: '0.5rem' }}
                onClick={() => this.goToDestinationWorkspace()}>
          Go to Copied {resourceType}
        </Button>
      );
    }
  }

  renderFormBody() {
    return (
      <div>
        <div style={headerStyles.formLabel}>Destination *</div>
        <Select
          value={this.state.destination}
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
        {this.state.requestState === RequestState.ERROR &&
        <ValidationError> {this.state.errorMsg} </ValidationError>}
      </div>
    );
  }

  renderSuccessBody() {
    const {fromResourceName, resourceType} = this.props;
    return (
      <div> Successfully copied
        <b style={boldStyle}> {fromResourceName} </b> to
        <b style={boldStyle}> {this.state.destination.name} </b>.
        Do you want to view the copied {resourceType}?</div>
    );
  }
}

export {
  CopyModal,
  Props as CopyModalProps,
  State as CopyModalState,
};
