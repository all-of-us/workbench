import {workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {navigateByUrl} from 'app/utils/navigation';
import {ResourceType} from 'app/utils/resourceActions';
import {CopyNotebookModal} from 'app/views/copy-notebook-modal';
import {RenameModal} from 'app/views/rename-modal';
import {Action, ResourceCardTemplate} from 'app/views/resource-card-template';
import {RecentResource} from 'generated/fetch';
import * as React from 'react';
import * as fp from 'lodash';
import {withErrorModal, WithErrorModalProps} from "app/views/with-error-modal";
import {withConfirmDeleteModal, WithConfirmDeleteModalProps} from "app/views/with-confirm-delete-modal";
import {withSpinnerOverlay, WithSpinnerOverlayProps} from "app/views/with-spinner-overlay";
import {formatRecentResourceDisplayDate} from "app/utils";

interface Props extends WithConfirmDeleteModalProps, WithErrorModalProps, WithSpinnerOverlayProps {
  resourceCard: RecentResource; // Destructure this into used parameters only
  onNotebookUpdate: Function;
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

    get displayDate(): string {
      if (!this.props.resourceCard.modifiedTime) {
        return '';
      }

      const date = new Date(this.props.resourceCard.modifiedTime);
      // datetime formatting to slice off weekday from readable date string
      return date.toDateString().split(' ').slice(1).join(' ');
    }

    get actions(): Action[] {
      return [
        {
          displayName: 'Rename',
          onClick: () => {
            this.setState({showRenameModal: true});
          },
        },
        {
          displayName: 'Duplicate',
          onClick: () => this.duplicateNotebook(),
        },
        {
          displayName: 'Copy to another Workspace',
          onClick: () => this.setState({showCopyNotebookModal: true}),
        },
        {
          displayName: 'Delete',
          onClick: () => {
            this.props.showConfirmDeleteModal(this.displayName, this.resourceType, () => this.deleteNotebook());
          },
        },
        {
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

    receiveNotebookRename(newName) {
      const {resourceCard} = this.props;
      return workspacesApi().renameNotebook(
        resourceCard.workspaceNamespace,
        resourceCard.workspaceFirecloudName,
        {
          name: resourceCard.notebook.name,
          newName: this.fullNotebookName(newName)
        }).then(() => this.props.onNotebookUpdate())
        .catch(error => console.error(error))
        .finally(() => {
          this.setState({showRenameModal: false});
        })
    }

    duplicateNotebook() {
      this.props.showSpinner();

      return workspacesApi().cloneNotebook(
        this.props.resourceCard.workspaceNamespace,
        this.props.resourceCard.workspaceFirecloudName,
        this.props.resourceCard.notebook.name)
        .then(() => {
          this.props.onNotebookUpdate();
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
          this.props.onNotebookUpdate();
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
          onCopy={() => this.props.onNotebookUpdate()}/>
        }

        {this.state.showRenameModal &&
        <RenameModal type={this.resourceType}
                     onRename={(newName) => this.receiveNotebookRename(newName)}
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
          footerText={ResourceType.NOTEBOOK}
          footerColor={colors.resourceCardHighlights.notebook}
        />
      </React.Fragment>;
    }
  });
