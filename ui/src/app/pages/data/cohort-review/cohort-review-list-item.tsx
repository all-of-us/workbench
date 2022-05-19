import * as React from 'react';
import { useParams } from 'react-router';
import * as fp from 'lodash/fp';
import { RadioButton } from 'primereact/radiobutton';

const { useState } = React;

import { ResourceType, WorkspaceAccessLevel } from 'generated/fetch';

import { RenameModal } from 'app/components/rename-modal';
import {
  Action,
  ResourceActionsMenu,
} from 'app/components/resource-actions-menu';
import { withConfirmDeleteModal } from 'app/components/with-confirm-delete-modal';
import { cohortReviewApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { withCurrentWorkspace } from 'app/utils';
import { MatchParams } from 'app/utils/stores';

export const CohortReviewListItem = fp.flow(
  withConfirmDeleteModal(),
  withCurrentWorkspace()
)(
  ({
    showConfirmDeleteModal,
    workspace,
    cohortReview,
    onUpdate,
    selected,
    onSelect,
  }) => {
    const { ns, wsid } = useParams<MatchParams>();
    const [showRenameModal, setShowRenameModal] = useState(false);
    const readOnly =
      workspace.accessLevel !== WorkspaceAccessLevel.OWNER &&
      workspace.accessLevel !== WorkspaceAccessLevel.WRITER;

    const deleteReview = async (reviewId) => {
      return cohortReviewApi()
        .deleteCohortReview(ns, wsid, reviewId)
        .then(() => onUpdate());
    };

    const rename = (name, description) => {
      const request = {
        ...cohortReview,
        cohortName: name,
        description: description,
      };
      cohortReviewApi()
        .updateCohortReview(ns, wsid, cohortReview.cohortReviewId, request)
        .then(() => onUpdate())
        .catch((error) => console.error(error))
        .finally(() => setShowRenameModal(false));
    };

    const actions = (): Action[] => {
      return [
        {
          icon: 'note',
          displayName: 'Rename',
          onClick: () => setShowRenameModal(true),
          disabled: readOnly,
        },
        {
          icon: 'trash',
          displayName: 'Delete',
          onClick: () =>
            showConfirmDeleteModal(
              cohortReview.cohortName,
              ResourceType.COHORTREVIEW,
              () => deleteReview(cohortReview.cohortReviewId)
            ),
          disabled: readOnly,
        },
      ];
    };

    return (
      <React.Fragment>
        <div
          style={{
            border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
            borderRadius: '2px',
            display: 'flex',
            height: '2.25rem',
            marginTop: '0.25rem',
          }}
        >
          <div
            style={{
              borderRight: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
              flex: '0 0 10%',
              padding: '10px 0 10px 10px',
            }}
          >
            <ResourceActionsMenu actions={actions()} disabled={readOnly} />
          </div>
          <div
            style={{
              color: colors.primary,
              flex: '0 0 90%',
              fontSize: '13px',
              padding: '0.5rem 0.25rem',
            }}
          >
            <RadioButton
              style={{ marginRight: '0.25rem' }}
              name='reviewItem'
              onChange={() => onSelect(cohortReview.cohortReviewId)}
              checked={selected}
            />
            {cohortReview.cohortName}
          </div>
        </div>
        {showRenameModal && (
          <RenameModal
            existingNames={[]}
            oldName={cohortReview.cohortName}
            onCancel={() => setShowRenameModal(false)}
            onRename={(name, description) => rename(name, description)}
            resourceType={ResourceType.COHORTREVIEW}
          />
        )}
      </React.Fragment>
    );
  }
);
