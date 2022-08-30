import * as React from 'react';
import { useParams } from 'react-router';
import * as fp from 'lodash/fp';
import { RadioButton } from 'primereact/radiobutton';
import {
  faCircleCheck,
  faDiamondExclamation,
} from '@fortawesome/pro-regular-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

const { useState } = React;

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
import { MatchParams } from 'app/utils/stores';

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
            border: `1px solid ${colorWithWhiteness(colors.black, 0.9)}`,
            borderRadius: '3px',
            boxShadow:
              '0 0 2px 0 rgba(0,0,0,0.12), 0 3px 2px 0 rgba(0,0,0,0.12)',
            display: 'flex',
            height: '3.5rem',
            marginTop: '0.25rem',
          }}
        >
          <div
            style={{
              flex: '0 0 10%',
            }}
          >
            <RadioButton
              style={{ marginLeft: '0.25rem' }}
              name='reviewItem'
              onChange={() => onSelect(cohortReview.cohortReviewId)}
              checked={selected}
            />
          </div>
          <div
            style={{
              color: colors.primary,
              flex: '0 0 50%',
              fontSize: '13px',
              lineHeight: '0.75rem',
              padding: '0.5rem 0.25rem',
            }}
          >
            <div>{cohortReview.cohortName}</div>
            <Clickable>
              <span style={{ color: colors.accent }}>Cohort details</span>
            </Clickable>
            <div style={{ color: colors.disabled, fontSize: '11px' }}>
              {displayDate(cohortReview.creationTime)}
            </div>
          </div>
          <div
            style={{
              color: colors.primary,
              flex: '0 0 40%',
              fontSize: '13px',
            }}
          >
            <div style={{ height: '50%', textAlign: 'right' }}>
              <ResourceActionsMenu actions={actions()} disabled={readOnly} />
            </div>
            {cohortModifiedTime > cohortReview.creationTime ? (
              <div style={{ color: colors.warning, padding: '0.25rem 0' }}>
                <FontAwesomeIcon icon={faDiamondExclamation} /> Outdated
              </div>
            ) : (
              <div style={{ color: colors.select, padding: '0.25rem 0' }}>
                <FontAwesomeIcon icon={faCircleCheck} /> Latest
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
            resourceType={ResourceType.COHORTREVIEW}
          />
        )}
      </React.Fragment>
    );
  }
);
