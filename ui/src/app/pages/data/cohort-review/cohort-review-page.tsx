import * as React from 'react';
import { useParams } from 'react-router';
import { useHistory } from 'react-router-dom';
import * as fp from 'lodash/fp';

const { useEffect, useState } = React;

import {
  CohortReview,
  CriteriaType,
  Domain,
  FilterColumns as Columns,
  PageFilterRequest as Request,
  SortOrder,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { ClrIcon } from 'app/components/icons';
import { SpinnerOverlay } from 'app/components/spinners';
import { withSpinnerOverlay } from 'app/components/with-spinner-overlay';
import { CohortReviewListItem } from 'app/pages/data/cohort-review/cohort-review-list-item';
import { CohortReviewOverview } from 'app/pages/data/cohort-review/cohort-review-overview';
import { CohortReviewParticipantsTable } from 'app/pages/data/cohort-review/cohort-review-participants-table';
import { CreateCohortReviewModal } from 'app/pages/data/cohort-review/create-cohort-review-modal';
import { visitsFilterOptions } from 'app/services/review-state.service';
import {
  cohortBuilderApi,
  cohortReviewApi,
  cohortsApi,
} from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { datatableStyles } from 'app/styles/datatable';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import { displayDate } from 'app/utils/dates';
import { currentCohortReviewStore, useNavigation } from 'app/utils/navigation';
import { MatchParams } from 'app/utils/stores';

const styles = reactStyles({
  backBtn: {
    padding: 0,
    border: 0,
    fontSize: '14px',
    color: colors.accent,
    background: 'transparent',
    cursor: 'pointer',
  },
  title: {
    margin: '0 0.5rem 0 0',
    fontSize: '20px',
    fontWeight: 600,
    color: colors.primary,
    overflow: 'auto',
  },
  description: {
    margin: '0 0 0.25rem',
    color: '#000000',
  },
  columnHeader: {
    background: '#f4f4f4',
    color: colors.primary,
    fontWeight: 600,
  },
  columnBody: {
    background: '#ffffff',
    padding: '0.5rem 0.5rem 0.3rem 0.75rem',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    borderBottom: 'none',
    lineHeight: '0.6rem',
    cursor: 'pointer',
  },
  reviewList: {
    border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
    borderRadius: '3px',
    flex: '0 0 20%',
    marginBottom: '0.25rem',
  },
  reviewListHeader: {
    borderBottom: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
    color: colors.primary,
    fontSize: '16px',
    fontWeight: 600,
    padding: '0.5rem',
  },
  sortIcon: {
    marginTop: '4px',
    color: '#2691D0',
    fontSize: '0.5rem',
    float: 'right',
  },
  tableBody: {
    textAlign: 'left',
    lineHeight: '0.75rem',
  },
});
const reverseColumnEnum = {
  participantId: Columns.PARTICIPANTID,
  sexAtBirth: Columns.SEXATBIRTH,
  gender: Columns.GENDER,
  race: Columns.RACE,
  ethnicity: Columns.ETHNICITY,
  birthDate: Columns.BIRTHDATE,
  deceased: Columns.DECEASED,
  status: Columns.STATUS,
};
const rows = 25;
const defaultReviewQuery = {
  page: 0,
  pageSize: rows,
  sortColumn: reverseColumnEnum.participantId,
  sortOrder: SortOrder.Asc,
  filters: { items: [] },
} as Request;

const sortByCreationTime = (a, b) => b.creationTime - a.creationTime;

export const CohortReviewPage = fp.flow(
  withCurrentWorkspace(),
  withSpinnerOverlay()
)(({ hideSpinner, showSpinner, workspace }) => {
  const history = useHistory();
  const { ns, wsid, cid, crid } = useParams<MatchParams>();
  const [navigate, navigateByUrl] = useNavigation();
  const [cohort, setCohort] = useState(undefined);
  const [cohortReviews, setCohortReviews] = useState(undefined);
  const [activeReview, setActiveReview] = useState(undefined);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [participantCount, setParticipantCount] = useState(undefined);
  const readOnly = workspace.accessLevel === WorkspaceAccessLevel.READER;

  const getParticipantData = (cohortReviewId: number) => {
    showSpinner();
    cohortReviewApi()
      .getParticipantCohortStatuses(
        ns,
        wsid,
        +cid,
        cohortReviewId,
        defaultReviewQuery
      )
      .then(({ cohortReview }) => {
        setCohortReviews((prevCohortReviews) => {
          const updateIndex = prevCohortReviews.findIndex(
            (cr) => cr.cohortReviewId === cohortReview.cohortReviewId
          );
          if (updateIndex > -1) {
            prevCohortReviews[updateIndex] = cohortReview;
          }
          return prevCohortReviews;
        });
        currentCohortReviewStore.next(cohortReview);
        setActiveReview(cohortReview);
        hideSpinner();
      });
  };

  // sets the cohort review id as a url param
  const updateUrlWithCohortReviewId = (cohortReviewId: number) =>
    history.push(
      `/workspaces/${ns}/${wsid}/data/cohorts/${cid}/reviews/${cohortReviewId}`
    );

  const loadCohortAndReviews = async () => {
    const [cohortResponse, cohortReviewResponse, participantCountResponse] =
      await Promise.all([
        cohortsApi().getCohort(ns, wsid, +cid),
        cohortReviewApi().getCohortReviewsByCohortId(ns, wsid, +cid),
        cohortReviewApi().cohortParticipantCount(ns, wsid, +cid),
      ]);
    cohortReviewResponse.items.sort(sortByCreationTime);
    setCohort(cohortResponse);
    setCohortReviews(cohortReviewResponse.items);
    setParticipantCount(participantCountResponse);
    if (cohortReviewResponse.items.length > 0) {
      let selectedReview = cohortReviewResponse.items[0];
      if (crid) {
        selectedReview = cohortReviewResponse.items.find(
          (cr) => cr.cohortReviewId === +crid
        );
      } else {
        updateUrlWithCohortReviewId(selectedReview.cohortReviewId);
      }
      currentCohortReviewStore.next(selectedReview);
      setActiveReview(selectedReview);
      getParticipantData(selectedReview.cohortReviewId);
    } else {
      hideSpinner();
    }
    setLoading(false);
  };

  const getVisitsFilterOptions = () => {
    cohortBuilderApi()
      .findCriteriaBy(
        ns,
        wsid,
        Domain[Domain.VISIT],
        CriteriaType[CriteriaType.VISIT]
      )
      .then((response) => {
        visitsFilterOptions.next([
          { value: null, label: 'Any' },
          ...response.items.map((option) => {
            return { value: option.name, label: option.name };
          }),
        ]);
      });
  };

  useEffect(() => {
    loadCohortAndReviews();
    if (!visitsFilterOptions.getValue()) {
      getVisitsFilterOptions();
    }
  }, []);

  const onReviewCreate = (review: CohortReview) => {
    updateUrlWithCohortReviewId(review.cohortReviewId);
    currentCohortReviewStore.next(review);
    setCohortReviews((prevCohortReviews) => [...prevCohortReviews, review]);
    setActiveReview(review);
    setShowCreateModal(false);
  };

  const onReviewSelect = (review: CohortReview) => {
    updateUrlWithCohortReviewId(review.cohortReviewId);
    if (review.participantCohortStatuses?.length) {
      currentCohortReviewStore.next(review);
      setActiveReview(review);
    } else {
      getParticipantData(review.cohortReviewId);
    }
  };

  return (
    <FadeBox style={{ margin: 'auto', paddingTop: '1rem', width: '95.7%' }}>
      {loading ? (
        <SpinnerOverlay />
      ) : (
        <React.Fragment>
          <style>{datatableStyles}</style>
          <div>
            <button
              style={styles.backBtn}
              type='button'
              onClick={() =>
                navigateByUrl(`workspaces/${ns}/${wsid}/data/cohorts/build`, {
                  queryParams: { cohortId: cid },
                })
              }
            >
              Back to cohort
            </button>
            <div style={{ display: 'flex' }}>
              <h4 style={styles.title}>Review Sets for {cohort.name}</h4>
              <Clickable
                disabled={loading}
                onClick={() =>
                  navigate([
                    'workspaces',
                    ns,
                    wsid,
                    'data',
                    'cohorts',
                    cid,
                    'review',
                    'cohort-description',
                  ])
                }
              >
                <span
                  style={{
                    color: colors.accent,
                    fontSize: '12px',
                    fontWeight: 500,
                    marginRight: '0.5rem',
                  }}
                >
                  Cohort details
                </span>
              </Clickable>
              <span
                style={{
                  color: colors.disabled,
                  fontSize: '11px',
                  marginRight: '0.5rem',
                }}
              >
                Last modified date: {displayDate(cohort.lastModifiedTime)}
              </span>
            </div>
            <div style={styles.description}>{cohort.description}</div>
          </div>
          {!!activeReview && (
            <CohortReviewOverview cohortReview={activeReview} />
          )}
          <div style={{ display: 'flex' }}>
            <div style={styles.reviewList}>
              <div style={styles.reviewListHeader}>
                Review Sets
                <Clickable
                  style={{ display: 'inline-block', marginLeft: '0.5rem' }}
                  disabled={readOnly}
                  onClick={() => setShowCreateModal(true)}
                >
                  <ClrIcon shape='plus-circle' class='is-solid' size={18} />
                </Clickable>
              </div>
              <div style={{ minHeight: '10rem', padding: '0.25rem' }}>
                {cohortReviews.map((cohortReview, cr) => (
                  <CohortReviewListItem
                    key={cr}
                    cohortReview={cohortReview}
                    cohortModifiedTime={cohort.lastModifiedTime}
                    onUpdate={() => loadCohortAndReviews()}
                    onSelect={() => {
                      if (
                        activeReview?.cohortReviewId !==
                        cohortReview.cohortReviewId
                      ) {
                        onReviewSelect(cohortReview);
                      }
                    }}
                    selected={
                      activeReview?.cohortReviewId ===
                      cohortReview.cohortReviewId
                    }
                    existingNames={cohortReviews.map(
                      ({ cohortName }) => cohortName
                    )}
                  />
                ))}
              </div>
            </div>
            <div style={{ flex: '0 0 80%', marginLeft: '0.25rem' }}>
              {!cohortReviews.length ? (
                <div
                  style={{
                    color: colorWithWhiteness(colors.dark, 0.6),
                    fontSize: '20px',
                    fontWeight: 400,
                    padding: '1rem',
                  }}
                >
                  There are no review sets for this cohort. Click the plus icon
                  to create a review set.
                </div>
              ) : (
                !!activeReview?.participantCohortStatuses && (
                  <CohortReviewParticipantsTable cohortReview={activeReview} />
                )
              )}
            </div>
          </div>
        </React.Fragment>
      )}
      {showCreateModal && (
        <CreateCohortReviewModal
          canceled={() => setShowCreateModal(false)}
          cohortName={cohort.name}
          created={(review) => onReviewCreate(review)}
          existingNames={cohortReviews.map(({ cohortName }) => cohortName)}
          participantCount={participantCount}
        />
      )}
    </FadeBox>
  );
});
