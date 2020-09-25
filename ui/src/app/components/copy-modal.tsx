import * as fp from 'lodash/fp';
import * as React from 'react';

import { Button } from 'app/components/buttons';
import { styles as headerStyles } from 'app/components/headers';
import { Select, TextInput, ValidationError } from 'app/components/inputs';
import { Modal, ModalBody, ModalFooter, ModalTitle } from 'app/components/modals';
import {CdrVersionListResponse, ConceptSet, FileDetail, ResourceType, Workspace} from 'generated/fetch';

import { Spinner } from 'app/components/spinners';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import {reactStyles, withCdrVersions} from 'app/utils';
import { navigate } from 'app/utils/navigation';
import {toDisplay} from 'app/utils/resourceActions';
import { WorkspacePermissions } from 'app/utils/workspace-permissions';

enum RequestState { UNSENT, COPY_ERROR, SUCCESS }

const ResourceTypeHomeTabs = new Map()
  .set(ResourceType.NOTEBOOK, 'notebooks')
  .set(ResourceType.COHORT, 'data')
  .set(ResourceType.CONCEPTSET, 'data')
  .set(ResourceType.DATASET, 'data');

export interface Props {
  cdrVersionListResponse: CdrVersionListResponse;
  fromWorkspaceNamespace: string;
  fromWorkspaceFCName: string;
  fromResourceName: string;
  fromCdrVersionId: string;
  onClose: Function;
  onCopy: Function;
  resourceType: ResourceType;
  saveFunction: (CopyRequest) => Promise<FileDetail | ConceptSet>;
}

interface WorkspaceOptions {
  label: string;
  options: Array<{label: string, value: Workspace}>;
}

interface State {
  workspaceOptions: Array<WorkspaceOptions>;
  destination: Workspace;
  newName: string;
  requestState: RequestState;
  copyErrorMsg: string;
  loading: boolean;
}

const styles = reactStyles({
  bold: {
    fontWeight: 600
  },
});

class CopyModalComponent extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);

    this.state = {
      workspaceOptions: [],
      newName: props.fromResourceName,
      destination: null,
      requestState: RequestState.UNSENT,
      copyErrorMsg: '',
      loading: true,
    };
  }

  cdrName(cdrVersionId: string): string {
    const {cdrVersionListResponse} = this.props;
    return cdrVersionListResponse.items.find(version => version.cdrVersionId === cdrVersionId).name;
  }

  groupWorkspacesByCdrVersion(workspaces: Workspace[]): Array<WorkspaceOptions> {
    const {fromCdrVersionId} = this.props;
    const workspacesByCdr = fp.groupBy(w => w.cdrVersionId, workspaces);
    const cdrVersions = Array.from(new Set(workspaces.map(w => w.cdrVersionId)));

    // list the "from" CDR version first.
    const fromCdrVersionFirst = (cdrv1: string, cdrv2: string) => {
      if (cdrv1 === fromCdrVersionId && cdrv2 !== fromCdrVersionId) {
        return -1;
      } else if (cdrv1 !== fromCdrVersionId && cdrv2 === fromCdrVersionId) {
        return 1;
      } else {
        // TODO: a meaningful ordering, possibly as part of RW-5563
        return cdrv1.localeCompare(cdrv2);
      }
    };

    return cdrVersions.sort(fromCdrVersionFirst).map(versionId => ({
      label: this.cdrName(versionId),
      options: workspacesByCdr[versionId].map(workspace => ({
        'value': workspace,
        'label': workspace.name,
      })),
    }));
  }

  componentDidMount() {
    workspacesApi().getWorkspaces()
      .then((response) => {
        const writeableWorkspaces = response.items
            .filter(item => new WorkspacePermissions(item).canWrite)
            .map(workspaceResponse => workspaceResponse.workspace);

        this.setState({
          workspaceOptions: this.groupWorkspacesByCdrVersion(writeableWorkspaces),
          loading: false
        });
      });
  }

  setCopyError(errorMsg: string) {
    this.setState({
      copyErrorMsg: errorMsg,
      requestState: RequestState.COPY_ERROR,
      loading: false
    });
  }

  clearCopyError() {
    this.setState({
      copyErrorMsg: '',
      requestState: RequestState.UNSENT,
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
        `${toDisplay(resourceType)} with the same ` +
        `name already exists in the targeted workspace.` :
        response.status === 404 ?
          `${toDisplay(resourceType)} not found in the ` +
            `original workspace.` :
          'An error occurred while copying. Please try again.';

      this.setCopyError(errorMsg);
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
    const {loading, requestState} = this.state;

    return (
      <Modal onRequestClose={this.props.onClose}>
        <ModalTitle>Copy to Workspace</ModalTitle>
        {loading ?
          <ModalBody style={{ textAlign: 'center' }}><Spinner /></ModalBody> :
          <ModalBody>
            {(requestState === RequestState.UNSENT || requestState === RequestState.COPY_ERROR) && this.renderFormBody()}
            {requestState === RequestState.SUCCESS && this.renderSuccessBody()}
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
      this.state.requestState === RequestState.COPY_ERROR) {
      return 'Close';
    } else if (this.state.requestState === RequestState.SUCCESS) {
      return 'Stay Here';
    }
  }

  renderActionButton() {
    const resourceType = toDisplay(this.props.resourceType);
    if (this.state.requestState === RequestState.UNSENT ||
      this.state.requestState === RequestState.COPY_ERROR) {
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

  setDestination(destination: Workspace) {
    this.clearCopyError();
    this.setState({destination: destination});
  }

  renderFormBody() {
    const {destination, workspaceOptions, requestState, copyErrorMsg, newName} = this.state;
    return (
      <div>
        <div style={headerStyles.formLabel}>Destination *</div>
        <Select
          value={destination}
          options={workspaceOptions}
          onChange={(destWorkspace) => this.setDestination(destWorkspace)}
        />
        <div style={headerStyles.formLabel}>Name *</div>
        <TextInput
          autoFocus
          value={newName}
          onChange={v => this.setState({ newName: v })}
        />
        {requestState === RequestState.COPY_ERROR &&
        <ValidationError> {copyErrorMsg} </ValidationError>}
      </div>
    );
  }

  renderSuccessBody() {
    const {fromResourceName, resourceType} = this.props;
    return (
      <div> Successfully copied
        <b style={styles.bold}> {fromResourceName} </b> to
        <b style={styles.bold}> {this.state.destination.name} </b>.
        Do you want to view the copied {toDisplay(resourceType)}?</div>
    );
  }
}

const CopyModal = fp.flow(withCdrVersions())(CopyModalComponent);

export {
  CopyModal,
  CopyModalComponent,
  Props as CopyModalProps,
  State as CopyModalState,
};
