import * as React from 'react';
import * as fp from 'lodash/fp';

import { WorkspaceResource } from 'generated/fetch';

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
import { cohortReviewApi } from 'app/services/swagger-fetch-clients';
import { getDescription, getDisplayName, getType } from 'app/utils/resources';

interface Props
  extends WithConfirmDeleteModalProps,
    WithErrorModalProps,
    WithSpinnerOverlayProps {
  resource: WorkspaceResource;
  existingNameList: string[];
  onUpdate: () => Promise<void>;
  menuOnly: boolean;
}

interface State {
  showRenameModal: boolean;
}

export const CohortReviewResourceCard = fp.flow(
  withErrorModalWrapper(),
  withConfirmDeleteModal(),
  withSpinnerOverlay()
)(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        showRenameModal: false,
      };
    }

    get actions(): Action[] {
      const { resource } = this.props;
      return [
        {
          icon: 'note',
          displayName: 'Rename',
          onClick: () => {
            this.setState({ showRenameModal: true });
          },
          disabled: !canWrite(resource),
        },
        {
          icon: 'trash',
          displayName: 'Delete',
          onClick: () => {
            this.props.showConfirmDeleteModal(
              getDisplayName(resource),
              getType(resource),
              () => this.delete()
            );
          },
          disabled: !canDelete(resource),
        },
      ];
    }

    async delete() {
      return cohortReviewApi()
        .deleteCohortReview(
          this.props.resource.workspaceNamespace,
          this.props.resource.workspaceFirecloudName,
          this.props.resource.cohortReview.cohortReviewId
        )
        .then(() => {
          this.props.onUpdate();
        });
    }

    rename(name, description) {
      const request = {
        ...this.props.resource.cohortReview,
        participantCohortStatuses: [], // prevents error trying to map null value
        cohortName: name,
        description: description,
      };
      return cohortReviewApi()
        .updateCohortReview(
          this.props.resource.workspaceNamespace,
          this.props.resource.workspaceFirecloudName,
          this.props.resource.cohortReview.cohortReviewId,
          request
        )
        .then(() => {
          this.props.onUpdate();
        })
        .catch((error) => console.error(error))
        .finally(() => {
          this.setState({ showRenameModal: false });
        });
    }

    render() {
      const { resource, menuOnly } = this.props;
      return (
        <React.Fragment>
          {this.state.showRenameModal && (
            <RenameModal
              onRename={(name, description) => this.rename(name, description)}
              resourceType={getType(resource)}
              onCancel={() => this.setState({ showRenameModal: false })}
              oldDescription={getDescription(resource)}
              oldName={getDisplayName(resource)}
              existingNames={this.props.existingNameList}
            />
          )}
          {menuOnly ? (
            <ResourceActionsMenu
              actions={this.actions}
              disabled={resource.adminLocked}
            />
          ) : (
            <ResourceCard resource={resource} actions={this.actions} />
          )}
        </React.Fragment>
      );
    }
  }
);
