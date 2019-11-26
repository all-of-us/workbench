import {Component} from '@angular/core';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {Modal, ModalFooter, ModalTitle} from 'app/components/modals';
import {SpinnerOverlay} from 'app/components/spinners';
import {CreateReviewModal} from 'app/pages/data/cohort-review/create-review-modal';
import {cohortReviewStore, visitsFilterOptions} from 'app/services/review-state.service';
import {cohortBuilderApi, cohortReviewApi, cohortsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';
import {Cohort, CriteriaType, DomainType, ReviewStatus, SortOrder, WorkspaceAccessLevel} from 'generated/fetch';

const styles = reactStyles({
  title: {
    color: colors.primary,
    fontSize: '0.9rem',
    fontWeight: 200,
  },
});

interface State {
  reviewPresent: boolean;
  cohort: Cohort;
  readonly: boolean;
}

export class CohortReview extends React.Component<{}, State> {
  constructor(props: any) {
    super(props);
    this.state = {
      reviewPresent: undefined,
      cohort: undefined,
      readonly: false
    };
  }

  componentDidMount(): void {
    const {ns, wsid, cid} = urlParamsStore.getValue();
    const {accessLevel, cdrVersionId} = currentWorkspaceStore.getValue();
    this.setState({readonly: accessLevel === WorkspaceAccessLevel.READER});
    cohortReviewApi().getParticipantCohortStatuses(ns, wsid, cid, +cdrVersionId, {
      page: 0,
      pageSize: 25,
      sortOrder: SortOrder.Asc,
      filters: {items: []}
    }).then(review => {
      cohortReviewStore.next(review);
      const reviewPresent = review.reviewStatus !== ReviewStatus.NONE;
      this.setState({reviewPresent});
      if (reviewPresent) {
        navigate(['workspaces', ns, wsid, 'data', 'cohorts', cid, 'review', 'participants']);
      }
    });
    cohortsApi().getCohort(ns, wsid, cid).then(cohort => this.setState({cohort}));
    if (!visitsFilterOptions.getValue()) {
      cohortBuilderApi().getCriteriaBy(
        +cdrVersionId, DomainType[DomainType.VISIT], CriteriaType[CriteriaType.VISIT]
      ).then(response => {
        visitsFilterOptions.next([
          {value: null, label: 'Any'},
          ...response.items.map(option => {
            return {value: option.name, label: option.name};
          })
        ]);
      });
    }
  }

  reviewCreated = () => {
    this.setState({reviewPresent: true});
  }

  goBack = () => {
    history.back();
  }

  render() {
    const {cohort, readonly, reviewPresent} = this.state;
    const loading = !cohort || reviewPresent === undefined;
    const ableToReview = !!cohort && reviewPresent === false && !readonly;
    const unableToReview = !!cohort && reviewPresent === false && readonly;
    return <React.Fragment>
      {loading ? <SpinnerOverlay/>
        : <React.Fragment>
        {ableToReview && <CreateReviewModal canceled={() => this.goBack()} cohort={cohort} created={() => this.reviewCreated()}/>}
        {unableToReview && <Modal onRequestClose={() => this.goBack()}>
          <ModalTitle style={styles.title}>Users with read-only access cannot create cohort reviews</ModalTitle>
          <ModalFooter>
            <Button style={{}} type='primary' onClick={() => this.goBack()}>Return to cohorts</Button>
          </ModalFooter>
        </Modal>}
      </React.Fragment>}
    </React.Fragment>;
  }
}

@Component({
  selector: 'app-cohort-review',
  template: '<div #root></div>'
})
export class CohortReviewComponent extends ReactWrapperBase {
  constructor() {
    super(CohortReview, []);
  }
}
