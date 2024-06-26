import * as React from 'react';
import * as fp from 'lodash/fp';

import { RenameModal } from 'app/components/rename-modal';
import {
  ResourceAction,
  ResourceActionsMenu,
} from 'app/components/resources/resource-actions-menu';
import { CommonActionMenuProps } from 'app/components/resources/resource-list-action-menu';
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
import {
  canDelete,
  canWrite,
  getDescription,
  getDisplayName,
  getType,
} from 'app/utils/resources';

interface Props
  extends CommonActionMenuProps,
    WithConfirmDeleteModalProps,
    WithErrorModalProps,
    WithSpinnerOverlayProps {}

interface State {
  showRenameModal: boolean;
}

export const CohortReviewActionMenu = fp.flow(
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

    get actions(): ResourceAction[] {
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
      const { resource } = this.props;
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
          <ResourceActionsMenu
            actions={this.actions}
            disabled={resource.adminLocked}
            title='Cohort Review Action Menu'
          />
        </React.Fragment>
      );
    }
  }
);
