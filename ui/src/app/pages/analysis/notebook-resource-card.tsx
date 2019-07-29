import {RenameModal} from 'app/components/rename-modal';
import {CopyNotebookModal} from 'app/pages/analysis/copy-notebook-modal';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {formatRecentResourceDisplayDate} from 'app/utils';
import {navigateByUrl} from 'app/utils/navigation';
import {Action, ResourceCardTemplate} from 'app/components/resource-card-template';
import {withConfirmDeleteModal, WithConfirmDeleteModalProps} from 'app/components/with-confirm-delete-modal';
import {withErrorModal, WithErrorModalProps} from 'app/components/with-error-modal';
import {withSpinnerOverlay, WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {RecentResource} from 'generated/fetch';
import * as fp from 'lodash';
import * as React from 'react';

interface Props extends WithConfirmDeleteModalProps, WithErrorModalProps, WithSpinnerOverlayProps {
  resourceCard: RecentResource;
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
    return this.props.resourceCard.notebook.name.replace(/\.ipynb$/, '');
  }

  get readOnly(): boolean {
    return this.props.resourceCard.permission === 'READER';
  }

  get writePermission(): boolean {
    return this.props.resourceCard.permission === 'OWNER'
      || this.props.resourceCard.permission === 'WRITER';
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
      },
      {
        icon: 'grid-view',
        displayName: 'Open in Jupyter Lab',
        onClick: () => navigateByUrl(this.resourceUrl(true)),
      }];
  }

  fullNotebookName(name) {
    return !name || /^.+\.ipynb$/.test(name) ? name : `${name}.ipynb`;
  }

  resourceUrl(jupyterLab = false): string {
    const {workspaceNamespace, workspaceFirecloudName, notebook} =
      this.props.resourceCard;
    const workspacePrefix = `/workspaces/${workspaceNamespace}/${workspaceFirecloudName}`;

    const queryParams = new URLSearchParams([
      ['playgroundMode', 'false'],
      ['jupyterLabMode', String(jupyterLab)]
    ]);

    if (this.readOnly) {
      queryParams.set('jupyterLabMode', 'true');
    }
    return `${workspacePrefix}/notebooks/preview/${encodeURIComponent(notebook.name)}`;
  }

  renameNotebook(newName) {
    const {resourceCard} = this.props;
    return workspacesApi().renameNotebook(
      resourceCard.workspaceNamespace,
      resourceCard.workspaceFirecloudName,
      {
        name: resourceCard.notebook.name,
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
      this.props.resourceCard.workspaceNamespace,
      this.props.resourceCard.workspaceFirecloudName,
      this.props.resourceCard.notebook.name)
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
      this.props.resourceCard.workspaceNamespace,
      this.props.resourceCard.workspaceFirecloudName,
      this.props.resourceCard.notebook.name)
      .then(() => {
        this.props.onUpdate();
      });
  }

  render() {
    return <React.Fragment>
      {this.state.showCopyNotebookModal &&
      <CopyNotebookModal
        fromWorkspaceNamespace={this.props.resourceCard.workspaceNamespace}
        fromWorkspaceName={this.props.resourceCard.workspaceFirecloudName}
        fromNotebook={this.props.resourceCard.notebook}
        onClose={() => this.setState({showCopyNotebookModal: false})}
        onCopy={() => this.props.onUpdate()}/>
      }

      {this.state.showRenameModal &&
      <RenameModal type={this.resourceType}
                   onRename={(newName) => this.renameNotebook(newName)}
                   onCancel={() => this.setState({showRenameModal: false})}
                   hideDescription={true}
                   oldName={this.props.resourceCard.notebook.name}
                   nameFormat={(name) => this.fullNotebookName(name)}/>
      }

      <ResourceCardTemplate
        actions={this.actions}
        actionsDisabled={!this.writePermission}
        disabled={false} // Notebook Cards are always at least readable
        resourceUrl={this.resourceUrl()}
        displayName={this.displayName}
        description={''}
        displayDate={formatRecentResourceDisplayDate(this.props.resourceCard.modifiedTime)}
        footerText={this.resourceType}
        footerColor={colors.resourceCardHighlights.notebook}
      />
    </React.Fragment>;
  }
});
