import {CopyModal} from 'app/components/copy-modal';
import {RenameModal} from 'app/components/rename-modal';
import {Action, ResourceCardTemplate} from 'app/components/resource-card-template';
import {withConfirmDeleteModal, WithConfirmDeleteModalProps} from 'app/components/with-confirm-delete-modal';
import {withErrorModal, WithErrorModalProps} from 'app/components/with-error-modal';
import {withSpinnerOverlay, WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {dropNotebookFileSuffix} from 'app/pages/analysis/util';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {formatRecentResourceDisplayDate} from 'app/utils';
import {ResourceType} from 'app/utils/resourceActionsReact';
import {RecentResource} from 'generated/fetch';
import * as fp from 'lodash';
import * as React from 'react';
import {Workspace} from "../../../generated/fetch/api";

interface Props extends WithConfirmDeleteModalProps, WithErrorModalProps, WithSpinnerOverlayProps {
  resource: RecentResource;
  onUpdate: Function;
}

interface State {
  showCopyNotebookModal: boolean;
  showRenameModal: boolean;
}

export const NotebookResourceCard = fp.flow(
  withErrorModal(),
  withConfirmDeleteModal(),
  withSpinnerOverlay(),
)(class extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
    this.state = {
      showCopyNotebookModal: false,
      showRenameModal: false
    };
  }

  get resourceType(): string {
    return 'Notebook';
  }

  get displayName(): string {
    return dropNotebookFileSuffix(this.props.resource.notebook.name);
  }

  get resourceUrl(): string {
    const {workspaceNamespace, workspaceFirecloudName, notebook} =
      this.props.resource;

    return `/workspaces/${workspaceNamespace}/${workspaceFirecloudName}` +
    `/notebooks/preview/${encodeURIComponent(notebook.name)}`;
  }

  get writePermission(): boolean {
    return this.props.resource.permission === 'OWNER'
      || this.props.resource.permission === 'WRITER';
  }

  get actions(): Action[] {
    return [
      {
        icon: 'pencil',
        displayName: 'Rename',
        onClick: () => {
          this.setState({showRenameModal: true});
        },
      },
      {
        icon: 'copy',
        displayName: 'Duplicate',
        onClick: () => this.duplicateNotebook(),
      },
      {
        icon: 'copy',
        displayName: 'Copy to another Workspace',
        onClick: () => this.setState({showCopyNotebookModal: true}),
      },
      {
        icon: 'trash',
        displayName: 'Delete',
        onClick: () => {
          this.props.showConfirmDeleteModal(this.displayName,
            this.resourceType, () => this.deleteNotebook());
        },
      }];
  }

  fullNotebookName(name) {
    return !name || /^.+\.ipynb$/.test(name) ? name : `${name}.ipynb`;
  }

  renameNotebook(newName) {
    const {resource} = this.props;
    return workspacesApi().renameNotebook(
      resource.workspaceNamespace,
      resource.workspaceFirecloudName,
      {
        name: resource.notebook.name,
        newName: this.fullNotebookName(newName)
      }).then(() => this.props.onUpdate())
      .catch(error => console.error(error))
      .finally(() => {
        this.setState({showRenameModal: false});
      });
  }

  duplicateNotebook() {
    this.props.showSpinner();

    return workspacesApi().cloneNotebook(
      this.props.resource.workspaceNamespace,
      this.props.resource.workspaceFirecloudName,
      this.props.resource.notebook.name)
      .then(() => {
        this.props.onUpdate();
      })
      .catch(() => {
        this.props.showErrorModal('Duplicating Notebook Error',
          'Notebook with the same name already exists.');
      })
      .finally(() => {
        this.props.hideSpinner();
      });
  }

  deleteNotebook() {
    return workspacesApi().deleteNotebook(
      this.props.resource.workspaceNamespace,
      this.props.resource.workspaceFirecloudName,
      this.props.resource.notebook.name)
      .then(() => {
        this.props.onUpdate();
      });
  }

  copyNotebook(destination: Workspace, newName: string) {
    return workspacesApi().copyNotebook(
      this.props.resource.workspaceNamespace,
      this.props.resource.workspaceFirecloudName,
      dropNotebookFileSuffix(this.props.resource.notebook.name),
      {
        toWorkspaceName: destination.id,
        toWorkspaceNamespace: destination.namespace,
        newName: newName
      }
    );
  }

  render() {
    return <React.Fragment>
      {this.state.showCopyNotebookModal &&
      <CopyModal
        destinationTab='notebooks'
        fromWorkspaceNamespace={this.props.resource.workspaceNamespace}
        fromWorkspaceName={this.props.resource.workspaceFirecloudName}
        fromResourceName={dropNotebookFileSuffix(this.props.resource.notebook.name)}
        resourceType={ResourceType.NOTEBOOK}
        onClose={() => this.setState({showCopyNotebookModal: false})}
        onCopy={() => this.props.onUpdate()}
        saveFunction={(destination: Workspace, newName: string) => this.copyNotebook(destination, newName}/>
      }

      {this.state.showRenameModal &&
      <RenameModal type={this.resourceType}
                   onRename={(newName) => this.renameNotebook(newName)}
                   onCancel={() => this.setState({showRenameModal: false})}
                   hideDescription={true}
                   oldName={this.props.resource.notebook.name}
                   nameFormat={(name) => this.fullNotebookName(name)}/>
      }

      <ResourceCardTemplate
        actions={this.actions}
        actionsDisabled={!this.writePermission}
        disabled={false} // Notebook Cards are always at least readable
        resourceUrl={this.resourceUrl}
        displayName={this.displayName}
        description={''}
        displayDate={formatRecentResourceDisplayDate(this.props.resource.modifiedTime)}
        footerText={this.resourceType}
        footerColor={colors.resourceCardHighlights.notebook}
      />
    </React.Fragment>;
  }
});
