import * as React from 'react';
import * as fp from 'lodash/fp';

import { CopyRequest, WorkspaceResource } from 'generated/fetch';

import { CopyModal } from 'app/components/copy-modal';
import { RenameModal } from 'app/components/rename-modal';
import {
  Action,
  ResourceActionsMenu,
} from 'app/components/resource-actions-menu';
import {
  canDelete,
  canWrite,
  ResourceCard,
} from 'app/components/resource-card';
import {
  withConfirmDeleteModal,
  WithConfirmDeleteModalProps,
} from 'app/components/with-confirm-delete-modal';
import {
  WithErrorModalProps,
  withErrorModalWrapper,
} from 'app/components/with-error-modal-wrapper';
import {
  withSpinnerOverlay,
  WithSpinnerOverlayProps,
} from 'app/components/with-spinner-overlay';
import { appendNotebookFileSuffixByOldName } from 'app/pages/analysis/util';
import { notebooksApi } from 'app/services/swagger-fetch-clients';
import { AnalyticsTracker } from 'app/utils/analytics';
import { getDisplayName, getType } from 'app/utils/resources';
import { ACTION_DISABLED_INVALID_BILLING } from 'app/utils/strings';

interface Props
  extends WithConfirmDeleteModalProps,
    WithErrorModalProps,
    WithSpinnerOverlayProps {
  resource: WorkspaceResource;
  existingNameList: string[];
  onUpdate: () => Promise<void>;
  disableDuplicate: boolean;
  menuOnly: boolean;
  onAnalysisAppsPage?: boolean;
}

interface State {
  showCopyNotebookModal: boolean;
  showRenameModal: boolean;
}

export const NotebookResourceCard = fp.flow(
  withErrorModalWrapper(),
  withConfirmDeleteModal(),
  withSpinnerOverlay()
)(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        showCopyNotebookModal: false,
        showRenameModal: false,
      };
    }

    get actions(): Action[] {
      const { resource } = this.props;
      return [
        {
          icon: 'pencil',
          displayName: 'Rename',
          onClick: () => {
            AnalyticsTracker.Notebooks.OpenRenameModal();
            this.setState({ showRenameModal: true });
          },
          disabled: !canWrite(resource),
        },
        {
          icon: 'copy',
          displayName: 'Duplicate',
          onClick: () => {
            AnalyticsTracker.Notebooks.Duplicate();
            this.duplicateNotebook();
          },
          disabled: this.props.disableDuplicate || !canWrite(resource),
          hoverText:
            this.props.disableDuplicate && ACTION_DISABLED_INVALID_BILLING,
        },
        {
          icon: 'copy',
          displayName: 'Copy to another Workspace',
          onClick: () => {
            AnalyticsTracker.Notebooks.OpenCopyModal();
            this.setState({ showCopyNotebookModal: true });
          },
          disabled: false,
        },
        {
          icon: 'trash',
          displayName: 'Delete',
          onClick: () => {
            AnalyticsTracker.Notebooks.OpenDeleteModal();
            this.props.showConfirmDeleteModal(
              getDisplayName(resource),
              getType(resource),
              () => {
                AnalyticsTracker.Notebooks.Delete();
                return this.deleteNotebook();
              }
            );
          },
          disabled: !canDelete(resource),
        },
      ];
    }

    renameNotebook(newName, oldName) {
      const { resource } = this.props;
      return notebooksApi()
        .renameNotebook(
          resource.workspaceNamespace,
          resource.workspaceFirecloudName,
          {
            name: resource.notebook.name,
            newName: appendNotebookFileSuffixByOldName(newName, oldName),
          }
        )
        .then(() => this.props.onUpdate())
        .catch((error) => console.error(error))
        .finally(() => {
          this.setState({ showRenameModal: false });
        });
    }

    duplicateNotebook() {
      this.props.showSpinner();

      return notebooksApi()
        .cloneNotebook(
          this.props.resource.workspaceNamespace,
          this.props.resource.workspaceFirecloudName,
          this.props.resource.notebook.name
        )
        .then(() => {
          this.props.onUpdate();
        })
        .catch(() => {
          this.props.showErrorModal(
            'Duplicating Notebook Error',
            'Notebook with the same name already exists.'
          );
        })
        .finally(() => {
          this.props.hideSpinner();
        });
    }

    deleteNotebook() {
      return notebooksApi()
        .deleteNotebook(
          this.props.resource.workspaceNamespace,
          this.props.resource.workspaceFirecloudName,
          this.props.resource.notebook.name
        )
        .then(() => {
          this.props.onUpdate();
        });
    }

    copyNotebook(copyRequest: CopyRequest) {
      AnalyticsTracker.Notebooks.Copy();

      return notebooksApi().copyNotebook(
        this.props.resource.workspaceNamespace,
        this.props.resource.workspaceFirecloudName,
        this.props.resource.notebook.name,
        copyRequest
      );
    }

    render() {
      const {
        resource,
        menuOnly,
        onUpdate,
        existingNameList,
        onAnalysisAppsPage,
      } = this.props;
      const actions = this.actions;
      const oldName = getDisplayName(resource);
      return (
        <React.Fragment>
          {this.state.showCopyNotebookModal && (
            <CopyModal
              fromWorkspaceNamespace={resource.workspaceNamespace}
              fromWorkspaceFirecloudName={resource.workspaceFirecloudName}
              fromResourceName={getDisplayName(resource)}
              fromCdrVersionId={resource.cdrVersionId}
              fromAccessTierShortName={resource.accessTierShortName}
              resourceType={getType(resource)}
              onClose={() => this.setState({ showCopyNotebookModal: false })}
              onCopy={() => onUpdate()}
              saveFunction={(copyRequest: CopyRequest) =>
                this.copyNotebook(copyRequest)
              }
            />
          )}

          {this.state.showRenameModal && (
            <RenameModal
              resourceType={getType(resource)}
              onRename={(newName) => {
                AnalyticsTracker.Notebooks.Rename();
                this.renameNotebook(newName, oldName);
              }}
              onCancel={() => this.setState({ showRenameModal: false })}
              hideDescription={true}
              oldName={oldName}
              nameFormat={(name) =>
                appendNotebookFileSuffixByOldName(name, oldName)
              }
              existingNames={existingNameList}
            />
          )}
          {menuOnly ? (
            <ResourceActionsMenu
              {...{ onAnalysisAppsPage, actions }}
              disabled={resource.adminLocked}
            />
          ) : (
            <ResourceCard {...{ resource, actions }} />
          )}
        </React.Fragment>
      );
    }
  }
);
