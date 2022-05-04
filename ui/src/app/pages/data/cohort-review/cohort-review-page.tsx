import * as React from 'react';
import { useParams } from 'react-router';

const { useEffect, useState } = React;

import { Button } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { visitsFilterOptions } from 'app/services/review-state.service';
import {
  cohortBuilderApi,
  cohortReviewApi,
  cohortsApi,
} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { useNavigation } from 'app/utils/navigation';
import { MatchParams } from 'app/utils/stores';
import { reactStyles } from 'app/utils';
import { CriteriaType, Domain } from 'generated/fetch';

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
    marginTop: 0,
    fontSize: '20px',
    fontWeight: 600,
    color: colors.primary,
    overflow: 'auto',
  },
  description: {
    margin: '0 0 0.25rem',
    color: '#000000',
  },
});

export const CohortReviewPage = (spinnerProps: WithSpinnerOverlayProps) => {
  const { ns, wsid, cid } = useParams<MatchParams>();
  const [navigate, navigateByUrl] = useNavigation();
  const [cohort, setCohort] = useState(undefined);
  const [cohortReviews, setCohortReviews] = useState(undefined);
  const [activeReview, setActiveReview] = useState(undefined);
  const [loading, setLoading] = useState(true);

  const loadCohortAndReviews = async () => {
    const [cohortResponse, cohortReviewResponse] = await Promise.all([
      cohortsApi().getCohort(ns, wsid, +cid),
      cohortReviewApi().getCohortReviewsByCohortId(ns, wsid, +cid),
    ]);
    setCohort(cohortResponse);
    setCohortReviews(cohortReviewResponse.items);
    setActiveReview(cohortReviewResponse.items[0]);
    spinnerProps.hideSpinner();
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

  return (
    <FadeBox style={{ margin: 'auto', paddingTop: '1rem', width: '95.7%' }}>
      {loading ? (
        <SpinnerOverlay />
      ) : (
        <React.Fragment>
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
            <h4 style={styles.title}>
              Review Sets for {cohort.name}
              <Button
                style={{ float: 'right', height: '1.3rem' }}
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
                Cohort Description
              </Button>
            </h4>
            <div style={styles.description}>{cohort.description}</div>
          </div>
          <div style={{display: 'flex'}}>
            <div style={{flex: '0 0 20%'}}>
              <div>Review Sets</div>
              {cohortReviews.map((cohortReview, cr) => <div key={cr}>
                {cohortReview.name}
              </div>)}
            </div>
            <div style={{flex: '0 0 80%'}}>Table</div>
          </div>
        </React.Fragment>
      )}
    </FadeBox>
  );
};
