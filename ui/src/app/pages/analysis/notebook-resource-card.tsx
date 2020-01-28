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
import {AnalyticsTracker} from 'app/utils/analytics';
import {encodeURIComponentStrict} from 'app/utils/navigation';
import {toDisplay} from 'app/utils/resourceActions';
import {CopyRequest, RecentResource, ResourceType} from 'generated/fetch';
import * as fp from 'lodash';
import * as React from 'react';

interface Props extends WithConfirmDeleteModalProps, WithErrorModalProps, WithSpinnerOverlayProps {
  resource: RecentResource;
  existingNameList: string[];
  onUpdate: Function;
  disableDuplicate: boolean;
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

  get resourceType(): ResourceType {
    return ResourceType.NOTEBOOK;
  }

  get displayName(): string {
    return dropNotebookFileSuffix(this.props.resource.notebook.name);
  }

  get resourceUrl(): string {
    const {workspaceNamespace, workspaceFirecloudName, notebook} =
      this.props.resource;

    return `/workspaces/${workspaceNamespace}/${workspaceFirecloudName}` +
    `/notebooks/preview/${encodeURIComponentStrict(notebook.name)}`;
  }

  get writePermission(): boolean {
    return this.props.resource.permission === 'OWNER'
      || this.props.resource.permission === 'WRITER';
  }

  get deletePermission(): boolean {
    return this.props.resource.permission === 'OWNER';
  }

  get actions(): Action[] {
    return [
      {
        icon: 'pencil',
        displayName: 'Rename',
        onClick: () => {
          AnalyticsTracker.Notebooks.OpenRenameModal();
          this.setState({showRenameModal: true});
        },
        disabled: !this.writePermission,
      },
      {
        icon: 'copy',
        displayName: 'Duplicate',
        onClick: () => {
          AnalyticsTracker.Notebooks.Duplicate();
          this.duplicateNotebook();
        },
        disabled: this.props.disableDuplicate || !this.writePermission,
      },
      {
        icon: 'copy',
        displayName: 'Copy to another Workspace',
        onClick: () => {
          AnalyticsTracker.Notebooks.OpenCopyModal();
          this.setState({showCopyNotebookModal: true});
        },
        disabled: false,
      },
      {
        icon: 'trash',
        displayName: 'Delete',
        onClick: () => {
          AnalyticsTracker.Notebooks.OpenDeleteModal();
          this.props.showConfirmDeleteModal(
            this.displayName,
            this.resourceType,
            () => {
              AnalyticsTracker.Notebooks.Delete();
              return this.deleteNotebook();
            });
        },
        disabled: !this.deletePermission,
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

  copyNotebook(copyRequest: CopyRequest) {
    AnalyticsTracker.Notebooks.Copy();

    return workspacesApi().copyNotebook(
      this.props.resource.workspaceNamespace,
      this.props.resource.workspaceFirecloudName,
      dropNotebookFileSuffix(this.props.resource.notebook.name),
      copyRequest
    );
  }

  render() {
    return <React.Fragment>
      {this.state.showCopyNotebookModal &&
      <CopyModal
        fromWorkspaceNamespace={this.props.resource.workspaceNamespace}
        fromWorkspaceName={this.props.resource.workspaceFirecloudName}
        fromResourceName={dropNotebookFileSuffix(this.props.resource.notebook.name)}
        resourceType={this.resourceType}
        onClose={() => this.setState({showCopyNotebookModal: false})}
        onCopy={() => this.props.onUpdate()}
        saveFunction={(copyRequest: CopyRequest) => this.copyNotebook(copyRequest)}/>
      }

      {this.state.showRenameModal &&
      <RenameModal resourceType={this.resourceType}
                   onRename={(newName) => {
                     AnalyticsTracker.Notebooks.Rename();
                     this.renameNotebook(newName);
                   }}
                   onCancel={() => this.setState({showRenameModal: false})}
                   hideDescription={true}
                   oldName={this.props.resource.notebook.name}
                   nameFormat={(name) => this.fullNotebookName(name)}
                   existingNames={this.props.existingNameList}/>
      }

      <ResourceCardTemplate
        actions={this.actions}
        disabled={false} // Notebook Cards are always at least readable
        resourceUrl={this.resourceUrl}
        onNavigate={() => AnalyticsTracker.Notebooks.Preview()}
        displayName={this.displayName}
        description={''}
        displayDate={formatRecentResourceDisplayDate(this.props.resource.modifiedTime)}
        footerText={toDisplay(this.resourceType)}
        footerColor={colors.resourceCardHighlights.notebook}
      />
    </React.Fragment>;
  }
});
