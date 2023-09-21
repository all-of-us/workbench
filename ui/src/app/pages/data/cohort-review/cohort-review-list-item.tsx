import * as React from 'react';
import { useParams } from 'react-router';
import * as fp from 'lodash/fp';
import { RadioButton } from 'primereact/radiobutton';

import { ResourceType, WorkspaceAccessLevel } from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { RenameModal } from 'app/components/rename-modal';
import {
  Action,
  ResourceActionsMenu,
} from 'app/components/resource-actions-menu';
import { withConfirmDeleteModal } from 'app/components/with-confirm-delete-modal';
import { cohortReviewApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { withCurrentWorkspace } from 'app/utils';
import { displayDate } from 'app/utils/dates';
import { useNavigation } from 'app/utils/navigation';
import { MatchParams } from 'app/utils/stores';
import latest from 'assets/icons/latest.svg';
import outdated from 'assets/icons/outdated.svg';

const { useState } = React;

export const CohortReviewListItem = fp.flow(
  withConfirmDeleteModal(),
  withCurrentWorkspace()
)(
  ({
    showConfirmDeleteModal,
    workspace,
    cohortReview,
    cohortModifiedTime,
    onUpdate,
    selected,
    onSelect,
    existingNames,
  }) => {
    const { ns, wsid, cid } = useParams<MatchParams>();
    const [navigate] = useNavigation();
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
              ResourceType.COHORT_REVIEW,
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
            border: `1px solid ${colorWithWhiteness(colors.black, 0.9)}`,
            borderRadius: '3px',
            boxShadow:
              '0 0 2px 0 rgba(0,0,0,0.12), 0 3px 2px 0 rgba(0,0,0,0.12)',
            display: 'flex',
            height: '5.25rem',
            marginTop: '0.375rem',
          }}
        >
          <div
            style={{
              flex: '0 0 10%',
            }}
          >
            <RadioButton
              style={{ marginLeft: '0.375rem' }}
              name='reviewItem'
              onChange={() => onSelect(cohortReview.cohortReviewId)}
              checked={selected}
            />
          </div>
          <div
            style={{
              color: colors.primary,
              flex: '0 0 50%',
              lineHeight: '1.125rem',
              padding: '0.75rem 0.375rem',
            }}
          >
            <div style={{ fontSize: '14px', fontWeight: 600 }}>
              {cohortReview.cohortName}
            </div>
            <Clickable
              onClick={() =>
                navigate([
                  'workspaces',
                  ns,
                  wsid,
                  'data',
                  'cohorts',
                  cid,
                  'reviews',
                  cohortReview.cohortReviewId,
                  'cohort-description',
                ])
              }
            >
              <span
                style={{
                  color: colors.accent,
                  fontSize: '12px',
                  fontWeight: 500,
                }}
              >
                Cohort details
              </span>
            </Clickable>
            <div style={{ color: colors.disabled, fontSize: '10px' }}>
              {displayDate(cohortReview.creationTime)}
            </div>
          </div>
          <div
            style={{
              color: colors.primary,
              flex: '0 0 40%',
              fontSize: '12px',
            }}
          >
            <div style={{ height: '50%', textAlign: 'right' }}>
              <ResourceActionsMenu actions={actions()} disabled={readOnly} />
            </div>
            {cohortModifiedTime > cohortReview.creationTime ? (
              <div style={{ color: colors.warning, padding: '0.375rem 0' }}>
                <img src={outdated} /> Outdated
              </div>
            ) : (
              <div style={{ color: colors.select, padding: '0.375rem 0' }}>
                <img src={latest} /> Latest
              </div>
            )}
          </div>
        </div>
        {showRenameModal && (
          <RenameModal
            existingNames={existingNames}
            oldName={cohortReview.cohortName}
            onCancel={() => setShowRenameModal(false)}
            onRename={(name, description) => rename(name, description)}
            resourceType={ResourceType.COHORT_REVIEW}
          />
        )}
      </React.Fragment>
    );
  }
);
