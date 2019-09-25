import {Component} from '@angular/core';
import * as React from 'react';

import {SpinnerOverlay} from 'app/components/spinners';
import {CreateReviewModal} from 'app/pages/data/cohort-review/create-review-modal';
import {
  cohortReviewStore,
  filterStateStore,
  initialFilterState,
  multiOptions,
  visitsFilterOptions,
  vocabOptions
} from 'app/services/review-state.service';
import {cohortBuilderApi, cohortReviewApi, cohortsApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase} from 'app/utils';
import {currentCohortStore, currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';
import {CriteriaType, DomainType} from 'generated/fetch';
import {PageFilterType, ReviewStatus, SortOrder, WorkspaceAccessLevel} from 'generated/fetch';

interface State {
  reviewPresent: boolean;
  cohortLoaded: boolean;
  readonly: boolean;
}

export class CohortReview extends React.Component<{}, State> {
  constructor(props: any) {
    super(props);
    this.state = {
      reviewPresent: undefined,
      cohortLoaded: false,
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
      pageFilterType: PageFilterType.ParticipantCohortStatuses,
      filters: {items: []}
    }).then(review => {
      cohortReviewStore.next(review);
      const reviewPresent = review.reviewStatus !== ReviewStatus.NONE;
      this.setState({reviewPresent});
      if (reviewPresent) {
        navigate(['workspaces', ns, wsid, 'data', 'cohorts', cid, 'review', 'participants']);
      }
    });
    cohortsApi().getCohort(ns, wsid, cid).then(cohort => {
      // This effectively makes the 'current cohort' available to child components, by using
      // the `withCurrentCohort` HOC. In addition, this store is used to render the breadcrumb.
      currentCohortStore.next(cohort);
      this.setState({cohortLoaded: true});
    });
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
    const {cohortLoaded, readonly, reviewPresent} = this.state;
    const loading = !cohortLoaded || reviewPresent === undefined;
    const ableToReview = cohortLoaded && reviewPresent === false && !readonly;
    const unableToReview = cohortLoaded && reviewPresent === false && readonly;
    return <React.Fragment>
      {loading ? <SpinnerOverlay/>
        : <React.Fragment>
        {ableToReview && <CreateReviewModal canceled={() => this.goBack()} created={() => this.reviewCreated()}/>}
        {unableToReview && <div>Cannot review</div>}
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

  // constructor(private location: Location, private router: Router) {
  //   this.subscription = router.events.subscribe(event => {
  //     if (event instanceof NavigationStart) {
  //       const oldRoute = router.url.split('/').pop();
  //       const newRoute = event.url.split('/').pop();
  //       if (oldRoute === 'participants' && newRoute === 'review') {
  //         // back button was pressed on 'participants' route, go back again to avoid blank page
  //         location.back();
  //       }
  //     }
  //   });
  // }
}
