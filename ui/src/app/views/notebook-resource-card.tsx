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
import {withErrorModal} from "app/views/with-error-modal";
import {withConfirmDeleteModal} from "app/views/with-confirm-delete-modal";

interface Props {
  resourceCard: RecentResource; // Destructure this into used parameters only
  onUpdate: Function; // TODO eric: this could use a better name
  onDuplicateResource: Function;
  showErrorModal: Function;
  showConfirmDeleteModal: Function;
}

interface State {
  showCopyNotebookModal: boolean;
  renaming: boolean;
}

export const NotebookResourceCard = fp.flow(
  withErrorModal(),
  withConfirmDeleteModal(),
)(class extends React.Component<Props, State> {

    constructor(props: Props) {
      super(props);
      this.state = {
        showCopyNotebookModal: false,
        renaming: false
      };
    }

    get actions(): Action[] {
      return [
        {
          displayName: 'Rename',
          onClick: () => {
            this.setState({renaming: true});
          },
        },
        {
          displayName: 'Duplicate',
          onClick: (resourceCardFns) => this.duplicateNotebook(),
        },
        {
          displayName: 'Copy to another Workspace',
          onClick: () => this.setState({showCopyNotebookModal: true}),
        },
        {
          displayName: 'Delete',
          onClick: () => {
            this.props.showConfirmDeleteModal(this.displayName, 'Notebook',
              (onComplete) => this.deleteNotebook(onComplete));
          },
        },
        {
          displayName: 'Open in Jupyter Lab',
          onClick: () => navigateByUrl(this.resourceUrl(true)),
        }];
    }

    get readOnly(): boolean {
      return this.props.resourceCard.permission === 'READER';
    }

    get writePermission(): boolean {
      return this.props.resourceCard.permission === 'OWNER'
        || this.props.resourceCard.permission === 'WRITER';
    }

    get displayName(): string {
      return this.props.resourceCard.notebook.name.replace(/\.ipynb$/, '');
    }

    get displayDate(): string {
      if (!this.props.resourceCard.modifiedTime) {
        return '';
      }

      const date = new Date(this.props.resourceCard.modifiedTime);
      // datetime formatting to slice off weekday from readable date string
      return date.toDateString().split(' ').slice(1).join(' ');
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

    async receiveNotebookRename(newName) {
      const {resourceCard} = this.props;
      try {
        await workspacesApi().renameNotebook(
          resourceCard.workspaceNamespace,
          resourceCard.workspaceFirecloudName,
          {
            name: resourceCard.notebook.name,
            newName: this.fullNotebookName(newName)
          });
      } catch (error) {
        console.error(error); // TODO: better error handling
      } finally {
        this.setState({renaming: false});
        this.props.onUpdate();
      }
    }

    async deleteNotebook(onComplete: Function) {
      workspacesApi().deleteNotebook(
        this.props.resourceCard.workspaceNamespace,
        this.props.resourceCard.workspaceFirecloudName,
        this.props.resourceCard.notebook.name)
        .then(() => {
          onComplete();
          this.props.onUpdate();
        });
    }

    async duplicateNotebook() {
      workspacesApi().cloneNotebook(
        this.props.resourceCard.workspaceNamespace,
        this.props.resourceCard.workspaceFirecloudName,
        this.props.resourceCard.notebook.name)
        .then(() => {
          this.props.onUpdate();
        }).catch(e => {
          this.props.onDuplicateResource(false);
          this.props.showErrorModal('Duplicating Notebook Error',
            'Notebook with the same name already exists.');
        });
    }

    fullNotebookName(name) {
      return !name || /^.+\.ipynb$/.test(name) ? name : `${name}.ipynb`;
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

        {this.state.renaming &&
        <RenameModal type='Notebook'
                     onRename={(newName) => this.receiveNotebookRename(newName)}
                     onCancel={() => this.setState({renaming: false})}
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
          displayDate={this.displayDate}
          footerText={ResourceType.NOTEBOOK}
          footerColor={colors.resourceCardHighlights.notebook}
        />
      </React.Fragment>;
    }
  });
